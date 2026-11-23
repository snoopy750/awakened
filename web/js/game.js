// Spielzustand und Regeln (entspricht GameRepository.kt). Gespeichert wird lokal im Browser.
import * as D from './domain.js';

const KEY = 'awakened.v1';
export const SCHEMA = 1;

export function defaultSettings() {
  return {
    penaltyEnabled: true,
    deadlineMinute: 23 * 60 + 59,
    inactivityDays: 3,
    lockHours: 12,
    spotifyPlaylist: '',
    cardioUnit: 'MINUTES',
    soundEnabled: true,
    vibrationEnabled: true,
  };
}

export function emptyState() {
  return {
    schema: SCHEMA,
    profile: null,
    settings: defaultSettings(),
    days: {},
    weights: [],
    workouts: [],
    challenges: [],
    challengeProgress: {},
    weeklyClaims: {},
    xpLog: [],
    penalty: { active: false },
    meta: { lastEvaluatedDay: -1, inactivityAnchorDay: 0, penaltiesCleared: 0, jokerWeekUsed: -1, doubleXpWeekUsed: -1, doubleXpDay: -1, nextId: 1 },
  };
}

export class Game {
  constructor(storage = globalThis.localStorage, clock = () => new Date()) {
    this.storage = storage;
    this.clock = clock;
    this.listeners = new Set();
    this.state = this.load();
  }

  // ------------------------------------------------------------ Persistenz

  load() {
    try {
      const raw = this.storage?.getItem(KEY);
      if (raw) return migrate(JSON.parse(raw));
    } catch (e) { console.warn('Speicher nicht lesbar', e); }
    return emptyState();
  }
  save() {
    try { this.storage?.setItem(KEY, JSON.stringify(this.state)); } catch (e) { console.warn('Speichern fehlgeschlagen', e); }
  }
  on(fn) { this.listeners.add(fn); return () => this.listeners.delete(fn); }
  emit(ev) { this.listeners.forEach((fn) => fn(ev)); }
  commit() { this.save(); this.emit({ type: 'change' }); }

  now() { return this.clock(); }
  today() { return D.epochDay(this.now()); }
  id() { return this.state.meta.nextId++; }

  get p() { return this.state.profile; }
  get s() { return this.state.settings; }
  stats() { const p = this.p; return p ? p.stats : { STR: 10, AGI: 10, VIT: 10, END: 10, INT: 10 }; }
  skills() { return this.p ? D.unlockedSkills(this.p.level, this.p.stats) : new Set(); }

  // ------------------------------------------------------------ Profil

  createProfile({ name, age, sex, heightCm, weightKg, fitness, goal }) {
    const today = this.today();
    this.state.profile = {
      name: name.trim(), age, sex, heightCm, weightKg, fitness, goal, createdDay: today,
      level: 1, xpInLevel: 0, totalXp: 0, statPoints: 0,
      stats: { STR: 10, AGI: 10, VIT: 10, END: 10, INT: 10 },
      activeTitle: 'AWAKENED',
    };
    this.state.weights.push({ id: this.id(), day: today, kg: weightKg });
    Object.assign(this.state.meta, { lastEvaluatedDay: today, inactivityAnchorDay: today });
    this.ensureDay(today);
    this.commit();
  }

  updateProfile(patch) { Object.assign(this.p, patch); this.commit(); }

  addWeight(kg) {
    this.state.weights.push({ id: this.id(), day: this.today(), kg });
    this.p.weightKg = kg;
    this.commit();
  }
  deleteWeight(id) { this.state.weights = this.state.weights.filter((w) => w.id !== id); this.commit(); }

  allocateStat(stat) {
    const p = this.p;
    if (!p || p.statPoints <= 0) return;
    const before = this.skills();
    p.stats[stat]++; p.statPoints--;
    this.announceNewSkills(before, this.skills());
    this.commit();
  }

  setTitle(id) { this.p.activeTitle = id; this.commit(); }

  // ------------------------------------------------------------ Tagesquests

  day(d) { return this.state.days[d]; }

  ensureDay(d = this.today()) {
    const p = this.p;
    if (!p) return null;
    let rec = this.state.days[d];
    if (!rec) {
      rec = { ex: {}, allDone: false, bonus: false, active: false, xp: 0, kcal: 0, outcome: null };
      this.state.days[d] = rec;
    }
    if (!Object.keys(rec.ex).length) {
      for (const ex of D.DAILY) {
        if (ex === 'CARDIO') {
          const unit = this.s.cardioUnit;
          rec.ex[ex] = { target: D.cardioTarget(p.level, p.fitness, p.goal, unit), done: 0, unit: unit === 'MINUTES' ? 'min' : 'km10', xp: false };
        } else {
          rec.ex[ex] = { target: D.dailyTarget(p.level, ex, p.fitness, p.goal), done: 0, unit: 'reps', xp: false };
        }
      }
    }
    return rec;
  }

  addProgress(ex, delta) {
    const d = this.today();
    const row = this.ensureDay(d).ex[ex];
    this.setProgressInternal(d, ex, Math.max(0, row.done + delta));
    this.commit();
  }
  setProgress(ex, value) {
    const d = this.today();
    this.ensureDay(d);
    this.setProgressInternal(d, ex, Math.max(0, value));
    this.commit();
  }

  setProgressInternal(d, ex, newDone) {
    const rec = this.ensureDay(d);
    const row = rec.ex[ex];
    if (newDone > row.done) rec.active = true;
    row.done = Math.min(newDone, row.target * 10 + 1000);
    if (row.done >= row.target && !row.xp) {
      row.xp = true;
      let extra = 1;
      if (['PUSHUPS', 'SQUATS', 'SITUPS'].includes(ex) && this.skills().has('POWER_SURGE')) extra *= 1.1;
      const xp = this.xpWithBonuses(D.XP.DAILY_EXERCISE, this.stats()[D.EXERCISES[ex].stat], extra);
      this.awardXp(xp, `Daily: ${D.EXERCISES[ex].label}`);
      this.emit({ type: 'quest', title: D.EXERCISES[ex].label, xp });
    }
    this.checkAllDailies(d);
    this.checkWeeklies(d);
  }

  addCardioMinutes(minutes) {
    const d = this.today();
    const row = this.ensureDay(d).ex.CARDIO;
    const boost = this.skills().has('ENDURANCE_BOOST') ? 1.25 : 1;
    const delta = row.unit === 'km10' ? Math.round((minutes / 6) * 10 * boost) : Math.round(minutes * boost);
    this.setProgressInternal(d, 'CARDIO', row.done + delta);
  }

  checkAllDailies(d) {
    const rec = this.ensureDay(d);
    const rows = Object.values(rec.ex);
    const allDone = rows.length > 0 && rows.every((r) => r.done >= r.target);
    if (allDone && !rec.bonus) {
      Object.assign(rec, { allDone: true, bonus: true, active: true });
      const bonus = this.xpWithBonuses(D.XP.allDailiesBonus(this.p.level), 10, 1);
      this.awardXp(bonus, 'Bonus: Alle Dailies erledigt');
      this.emit({ type: 'message', title: '[QUEST ABGESCHLOSSEN]', text: `Die Daily Quest ist erfüllt. Belohnung: +${bonus} XP. Die Strafe wurde abgewendet.` });
    } else {
      rec.allDone = allDone;
    }
  }

  // ------------------------------------------------------------ XP

  xpWithBonuses(base, statValue, extra) {
    const doubleXp = this.state.meta.doubleXpDay === this.today();
    let e = extra;
    if (this.skills().has('MONARCH_AURA')) e *= 1.15;
    return D.XP.apply(base, statValue, doubleXp, e);
  }

  awardXp(amount, reason) {
    const p = this.p;
    if (!p || amount <= 0) return null;
    const before = this.skills();
    const oldLevel = p.level;
    const r = D.addXp(p.level, p.xpInLevel, amount);
    Object.assign(p, { level: r.level, xpInLevel: r.xpInLevel, totalXp: p.totalXp + amount, statPoints: p.statPoints + r.statPoints });
    const d = this.today();
    this.state.xpLog.unshift({ ts: this.now().getTime(), day: d, amount, reason });
    this.state.xpLog.length = Math.min(this.state.xpLog.length, 300);
    const rec = this.ensureDay(d);
    rec.xp += amount;
    this.emit({ type: 'xp', amount, reason });
    if (r.levelsGained > 0) {
      const newRank = D.rankFor(r.level) !== D.rankFor(oldLevel);
      this.emit({ type: 'levelup', level: r.level, levels: r.levelsGained, statPoints: r.statPoints, newRank });
      this.announceNewSkills(before, this.skills());
    }
    return r;
  }

  announceNewSkills(before, after) {
    for (const id of after) {
      if (before.has(id)) continue;
      const s = D.SKILLS.find((x) => x.id === id);
      this.emit({ type: 'message', title: '[NEUER SKILL]', text: `Skill freigeschaltet: „${s.name}“. ${s.desc}` });
    }
  }

  // ------------------------------------------------------------ Weekly Quests

  weeklyQuests(d = this.today()) {
    const ws = D.weekStart(d);
    const claimed = new Set(this.state.weeklyClaims[ws] || []);
    let cardioDays = 0, pushups = 0, maxPushTarget = 0;
    for (let x = ws; x <= ws + 6; x++) {
      const rec = this.state.days[x];
      if (!rec?.ex?.CARDIO) continue;
      if (minutesEquivalent(rec.ex.CARDIO) >= 30) cardioDays++;
      pushups += rec.ex.PUSHUPS?.done || 0;
      maxPushTarget = Math.max(maxPushTarget, rec.ex.PUSHUPS?.target || 0);
    }
    const todayTarget = this.state.days[d]?.ex?.PUSHUPS?.target || maxPushTarget || 20;
    const st = this.currentStreak(d);
    return [
      { key: 'CARDIO_3X', title: 'Ausdauer-Training', desc: '3× Ausdauer von mindestens 30 Minuten', progress: cardioDays, target: 3 },
      { key: 'STREAK_7', title: 'Ununterbrochen', desc: '7-Tage-Streak der Daily Quest', progress: Math.min(st, 7), target: 7 },
      { key: 'PUSHUPS_TOTAL', title: 'Eiserne Arme', desc: 'Liegestütze diese Woche insgesamt', progress: pushups, target: D.weeklyPushupTarget(todayTarget) },
    ].map((q) => ({ ...q, claimed: claimed.has(q.key), done: q.progress >= q.target }));
  }

  checkWeeklies(d) {
    const ws = D.weekStart(d);
    for (const q of this.weeklyQuests(d)) {
      if (!q.done || q.claimed) continue;
      (this.state.weeklyClaims[ws] ||= []).push(q.key);
      const xp = this.xpWithBonuses(D.XP.WEEKLY_QUEST, 10, 1);
      this.awardXp(xp, `Weekly: ${q.title}`);
      this.emit({ type: 'quest', title: `Weekly: ${q.title}`, xp });
    }
  }

  completedDays() {
    return new Set(Object.entries(this.state.days).filter(([, r]) => r.allDone).map(([d]) => Number(d)));
  }
  currentStreak(d = this.today()) { return D.streak(this.completedDays(), d); }
  bestStreak() { return D.bestStreak(this.completedDays()); }

  // ------------------------------------------------------------ Challenges

  createChallenge({ name, category, frequency, target, unit, xp }) {
    this.state.challenges.push({
      id: this.id(), name: name.trim(), category, frequency,
      target: Math.max(1, target), unit: unit.trim() || '×', xp: Math.min(5000, Math.max(1, xp)),
      createdDay: this.today(), archived: false,
    });
    this.commit();
  }
  deleteChallenge(id) {
    this.state.challenges = this.state.challenges.filter((c) => c.id !== id);
    for (const k of Object.keys(this.state.challengeProgress)) if (k.startsWith(`${id}:`)) delete this.state.challengeProgress[k];
    this.commit();
  }
  periodKey(c, d = this.today()) {
    return c.frequency === 'DAILY' ? d : c.frequency === 'WEEKLY' ? D.weekStart(d) : 0;
  }
  challengeProgress(c) { return this.state.challengeProgress[`${c.id}:${this.periodKey(c)}`] || { done: 0, completed: false }; }

  addChallengeProgress(id, delta) {
    const c = this.state.challenges.find((x) => x.id === id);
    if (!c) return;
    const d = this.today();
    const key = `${c.id}:${this.periodKey(c, d)}`;
    const cur = this.state.challengeProgress[key] || { done: 0, completed: false };
    const done = Math.max(0, cur.done + delta);
    if (delta > 0) this.ensureDay(d).active = true;
    if (done >= c.target && !cur.completed) {
      this.state.challengeProgress[key] = { done, completed: true };
      const isOther = c.category === 'OTHER';
      const extra = isOther && this.skills().has('SHARP_MIND') ? 1.2 : 1;
      const xp = this.xpWithBonuses(c.xp, isOther ? this.stats().INT : this.stats().STR, extra);
      this.awardXp(xp, `Challenge: ${c.name}`);
      this.emit({ type: 'quest', title: c.name, xp });
      if (c.frequency === 'ONCE') c.archived = true;
    } else {
      this.state.challengeProgress[key] = { ...cur, done };
    }
    this.commit();
  }

  // ------------------------------------------------------------ Training

  finishTraining({ title, startedAt, durationSec, reps, cardioMinutes, kcal }) {
    const d = this.today();
    this.ensureDay(d);
    for (const [ex, n] of Object.entries(reps)) {
      if (n > 0 && D.DAILY.includes(ex) && ex !== 'CARDIO') this.setProgressInternal(d, ex, this.state.days[d].ex[ex].done + n);
    }
    if (cardioMinutes > 0) this.addCardioMinutes(cardioMinutes);
    const minutes = Math.floor(durationSec / 60);
    const extra = this.skills().has('SHADOW_STEP') ? 1.1 : 1;
    const xp = this.xpWithBonuses(D.XP.training(minutes, kcal), this.stats().AGI, extra);
    this.state.workouts.unshift({
      id: this.id(), day: d, startedAt, durationSec, title, kcal, xp, cardioMinutes,
      details: Object.entries(reps).filter(([, n]) => n > 0).map(([ex, n]) => `${n} ${D.EXERCISES[ex].label}`).join(', '),
    });
    const rec = this.state.days[d];
    rec.kcal += kcal; rec.active = true;
    const r = this.awardXp(xp, `Training: ${title}`);
    this.commit();
    return { xp, kcal, levelsGained: r?.levelsGained || 0 };
  }

  // ------------------------------------------------------------ Skills

  activateDoubleXp() {
    if (!this.skills().has('DOUBLE_XP_DAY')) return false;
    const t = this.today();
    if (this.state.meta.doubleXpWeekUsed === D.weekStart(t)) return false;
    Object.assign(this.state.meta, { doubleXpWeekUsed: D.weekStart(t), doubleXpDay: t });
    this.emit({ type: 'message', title: '[SKILL AKTIVIERT]', text: 'Doppel-XP-Tag: Heute erhältst du doppelte XP.' });
    this.commit();
    return true;
  }

  // ------------------------------------------------------------ Penalty

  /** Prüft Deadline, verpasste Tage, Inaktivität und automatisches Entsperren. */
  evaluatePenalties() {
    const p = this.p;
    if (!p) return;
    const nowMs = this.now().getTime();
    const today = this.today();
    this.ensureDay(today);
    const s = this.s, meta = this.state.meta;
    const minute = D.minuteOfDay(this.now());

    if (this.state.penalty.active && nowMs >= this.state.penalty.unlockAt) this.clearPenalty(false);

    if (!s.penaltyEnabled) {
      meta.lastEvaluatedDay = Math.max(meta.lastEvaluatedDay, today - 1);
      this.commit();
      return;
    }

    const days = D.daysToEvaluate(meta.lastEvaluatedDay >= 0 ? meta.lastEvaluatedDay : null, today,
      D.isPastDeadline(minute, s.deadlineMinute), p.createdDay + 1);
    let failed = false;
    for (const d of days) {
      const rec = this.state.days[d] || (this.state.days[d] = { ex: {}, allDone: false, bonus: false, active: false, xp: 0, kcal: 0, outcome: null });
      const joker = this.skills().has('SECOND_WIND') && meta.jokerWeekUsed !== D.weekStart(d);
      const outcome = D.decideDay(rec.allDone, joker);
      rec.outcome = outcome;
      if (outcome === 'JOKER_USED') {
        meta.jokerWeekUsed = D.weekStart(d);
        this.emit({ type: 'message', title: '[JOKER EINGESETZT]', text: `„Zweiter Atem“ hat die verpasste Daily Quest vom ${D.fmtDate(d)} abgefangen.` });
      }
      if (outcome === 'FAILED') failed = true;
      meta.lastEvaluatedDay = d;
    }
    if (failed && !this.state.penalty.active) this.startPenalty('DAILY_MISSED', nowMs);

    if (!this.state.penalty.active) {
      const activeDays = Object.entries(this.state.days).filter(([, r]) => r.active).map(([d]) => Number(d));
      const lastActive = activeDays.length ? Math.max(...activeDays) : null;
      const anchor = Math.max(p.createdDay, meta.inactivityAnchorDay);
      if (D.inactivityTriggered(today, lastActive, anchor, s.inactivityDays)) this.startPenalty('INACTIVITY', nowMs);
    }
    this.commit();
  }

  startPenalty(reason, nowMs = this.now().getTime()) {
    const p = this.p;
    const [burpees, cardio] = D.penaltyQuest(p.fitness);
    this.state.penalty = {
      active: true, reason, startedAt: nowMs,
      unlockAt: D.unlockAt(nowMs, this.s.lockHours, this.skills().has('IRON_WILL'), p.stats.VIT),
      burpeesTarget: burpees, burpeesDone: 0, cardioTarget: cardio, cardioDone: 0,
    };
    this.emit({ type: 'penalty' });
  }

  triggerPenaltyManually() {
    if (!this.p || this.state.penalty.active) return;
    this.startPenalty('DAILY_MISSED');
    this.commit();
  }

  addPenaltyProgress(burpees = 0, cardio = 0) {
    const s = this.state.penalty;
    if (!s.active) return;
    s.burpeesDone = Math.max(0, s.burpeesDone + burpees);
    s.cardioDone = Math.max(0, s.cardioDone + cardio);
    if (burpees > 0 || cardio > 0) this.ensureDay(this.today()).active = true;
    if (D.penaltyQuestDone(s.burpeesDone, s.burpeesTarget, s.cardioDone, s.cardioTarget)) this.clearPenalty(true);
    this.commit();
  }

  clearPenalty(byQuest) {
    this.state.penalty = { active: false };
    this.state.meta.inactivityAnchorDay = this.today();
    if (byQuest) {
      this.state.meta.penaltiesCleared++;
      this.awardXp(D.XP.PENALTY_QUEST, 'Straf-Quest überlebt');
      this.emit({ type: 'message', title: '[PENALTY ZONE VERLASSEN]', text: 'Straf-Quest abgeschlossen. Die Sperre ist aufgehoben.' });
    }
  }

  // ------------------------------------------------------------ Erfolge & Statistik

  total(ex) { return Object.values(this.state.days).reduce((s, r) => s + (r.ex?.[ex]?.done || 0), 0); }
  totalCardioMinutes() { return Object.values(this.state.days).reduce((s, r) => s + (r.ex?.CARDIO ? minutesEquivalent(r.ex.CARDIO) : 0), 0); }

  achievements() {
    return {
      level: this.p?.level || 1,
      bestStreak: this.bestStreak(),
      totalPushups: this.total('PUSHUPS'),
      totalCardioMinutes: this.totalCardioMinutes(),
      totalWorkouts: this.state.workouts.length,
      penaltiesCleared: this.state.meta.penaltiesCleared,
      challengesCompleted: Object.values(this.state.challengeProgress).filter((x) => x.completed).length,
    };
  }

  // ------------------------------------------------------------ Backup

  exportJson() {
    return JSON.stringify({ app: 'AWAKENED', exportedAt: this.now().toISOString(), ...this.state }, null, 2);
  }
  importJson(text) {
    const data = JSON.parse(text);
    if (data.app !== 'AWAKENED' || !data.schema) throw new Error('Keine AWAKENED-Sicherung (Web-Version).');
    delete data.app; delete data.exportedAt;
    this.state = migrate(data);
    this.commit();
  }
  wipe() { this.state = emptyState(); this.commit(); }
}

export const minutesEquivalent = (row) => (row.unit === 'km10' ? Math.floor(row.done * 0.6) : row.done);

function migrate(s) {
  const base = emptyState();
  return { ...base, ...s, settings: { ...base.settings, ...(s.settings || {}) }, meta: { ...base.meta, ...(s.meta || {}) } };
}
