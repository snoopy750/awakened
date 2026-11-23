// Reine Spiellogik – identisch mit der Android-Version (domain/*.kt).

export const SEX = { MALE: 'Männlich', FEMALE: 'Weiblich', DIVERSE: 'Divers' };
export const FITNESS = { BEGINNER: 'Anfänger', INTERMEDIATE: 'Mittel', ADVANCED: 'Fortgeschritten' };
export const FITNESS_HINT = {
  BEGINNER: 'Du trainierst selten oder fängst neu an.',
  INTERMEDIATE: 'Du trainierst ab und zu und bist recht fit.',
  ADVANCED: 'Du trainierst regelmässig und intensiv.',
};
export const GOAL = { STRENGTH: 'Kraft', ENDURANCE: 'Ausdauer', WEIGHT_LOSS: 'Abnehmen' };
export const CARDIO_TYPE = {
  RUNNING: { label: 'Laufen', met: 9.8 },
  CYCLING: { label: 'Velo', met: 7.5 },
  WALKING: { label: 'Gehen', met: 3.8 },
  OTHER: { label: 'Anderes', met: 6.0 },
};

export const STATS = {
  STR: { label: 'Stärke', desc: 'Mehr XP für Kraftübungen (Liegestütze, Kniebeugen).' },
  AGI: { label: 'Agilität', desc: 'Mehr XP für Seilspringen und Training.' },
  VIT: { label: 'Vitalität', desc: 'Verkürzt die Sperrzeit in der Penalty Zone.' },
  END: { label: 'Ausdauer', desc: 'Mehr XP für Ausdauer-Einheiten.' },
  INT: { label: 'Intelligenz', desc: 'Mehr XP für Challenges der Kategorie „Anderes“.' },
};

export const EXERCISES = {
  PUSHUPS: { label: 'Liegestütze', unit: 'Wdh.', met: 8.0, spr: 2.0, stat: 'STR' },
  SITUPS: { label: 'Sit-ups', unit: 'Wdh.', met: 3.8, spr: 2.5, stat: 'STR' },
  SQUATS: { label: 'Kniebeugen', unit: 'Wdh.', met: 5.0, spr: 2.5, stat: 'STR' },
  JUMP_ROPE: { label: 'Seilspringen', unit: 'Sprünge', met: 11.8, spr: 0.5, stat: 'AGI' },
  CARDIO: { label: 'Ausdauer', unit: 'min', met: 8.0, spr: 60, stat: 'END' },
  BURPEES: { label: 'Burpees', unit: 'Wdh.', met: 8.0, spr: 4.0, stat: 'AGI' },
};
export const DAILY = ['PUSHUPS', 'SITUPS', 'SQUATS', 'JUMP_ROPE', 'CARDIO'];
const STRENGTH_EX = ['PUSHUPS', 'SQUATS', 'SITUPS'];

export const CHALLENGE_CATEGORY = { FITNESS: 'Fitness', OTHER: 'Anderes' };
export const CHALLENGE_FREQ = { DAILY: 'Täglich', WEEKLY: 'Wöchentlich', ONCE: 'Einmalig' };

// ------------------------------------------------------------------ Leveling

export const STAT_POINTS_PER_LEVEL = 3;
export const MAX_LEVEL = 999;

export function xpForNextLevel(level) {
  if (level < 1) throw new Error('Level muss >= 1 sein');
  return Math.floor(100 * Math.pow(level, 1.5));
}

export function addXp(level, xpInLevel, gained) {
  let l = level, xp = xpInLevel + gained, n = 0;
  while (l < MAX_LEVEL && xp >= xpForNextLevel(l)) { xp -= xpForNextLevel(l); l++; n++; }
  return { level: l, xpInLevel: xp, levelsGained: n, statPoints: n * STAT_POINTS_PER_LEVEL };
}

export function levelProgress(level, xpInLevel) {
  return Math.min(1, Math.max(0, xpInLevel / xpForNextLevel(level)));
}

export const RANKS = [
  { letter: 'E', min: 1, title: 'E-Rang Hunter' },
  { letter: 'D', min: 10, title: 'D-Rang Hunter' },
  { letter: 'C', min: 20, title: 'C-Rang Hunter' },
  { letter: 'B', min: 35, title: 'B-Rang Hunter' },
  { letter: 'A', min: 50, title: 'A-Rang Hunter' },
  { letter: 'S', min: 70, title: 'S-Rang Hunter' },
];
export const rankFor = (level) => RANKS.filter((r) => level >= r.min).pop();
export const nextRank = (rank) => RANKS[RANKS.indexOf(rank) + 1] ?? null;

export const XP = {
  DAILY_EXERCISE: 20,
  WEEKLY_QUEST: 150,
  PENALTY_QUEST: 30,
  allDailiesBonus: (level) => 50 + level * 2,
  training: (minutes, kcal) => minutes * 2 + Math.floor(kcal / 10),
  statMultiplier: (v) => 1 + Math.min(50, Math.max(0, v - 10)) / 100,
  apply(base, statValue, doubleXp, extra = 1) {
    return Math.round(base * XP.statMultiplier(statValue) * extra * (doubleXp ? 2 : 1));
  },
};

// ------------------------------------------------------------------ Quest-Skalierung

const BASE = {
  BEGINNER: { PUSHUPS: 10, SITUPS: 15, SQUATS: 20, JUMP_ROPE: 50, CARDIO: 10, BURPEES: 20 },
  INTERMEDIATE: { PUSHUPS: 25, SITUPS: 30, SQUATS: 40, JUMP_ROPE: 150, CARDIO: 20, BURPEES: 40 },
  ADVANCED: { PUSHUPS: 50, SITUPS: 60, SQUATS: 80, JUMP_ROPE: 300, CARDIO: 30, BURPEES: 60 },
};
const CAP = { PUSHUPS: 100, SITUPS: 100, SQUATS: 100, JUMP_ROPE: 1000, CARDIO: 60, BURPEES: 100 };

function goalFactor(goal, ex) {
  if (goal === 'STRENGTH') return STRENGTH_EX.includes(ex) ? 1.2 : 1;
  if (goal === 'ENDURANCE') return ['CARDIO', 'JUMP_ROPE'].includes(ex) ? 1.2 : 1;
  if (goal === 'WEIGHT_LOSS') return ['CARDIO', 'JUMP_ROPE'].includes(ex) ? 1.15 : 1;
  return 1;
}

export const levelFactor = (level) => 1 + 0.04 * (Math.max(1, level) - 1);

// Kotlins roundToInt rundet .5 auf (wie Math.round für positive Werte).
export function dailyTarget(level, ex, fitness, goal) {
  const raw = BASE[fitness][ex] * levelFactor(level) * goalFactor(goal, ex);
  const capped = Math.min(raw, CAP[ex]);
  const step = ex === 'CARDIO' ? 1 : ex === 'JUMP_ROPE' ? 10 : 5;
  return Math.max(step, Math.round(capped / step) * step);
}

export const cardioMinutesToTenthKm = (min) => Math.max(5, Math.round((min / 6) * 2) * 5);

export function cardioTarget(level, fitness, goal, unit) {
  const m = dailyTarget(level, 'CARDIO', fitness, goal);
  return unit === 'MINUTES' ? m : cardioMinutesToTenthKm(m);
}

export const weeklyPushupTarget = (daily) => daily * 5;

export function penaltyQuest(fitness) {
  return { BEGINNER: [30, 15], INTERMEDIATE: [50, 20], ADVANCED: [80, 30] }[fitness];
}

// ------------------------------------------------------------------ Fitness

export function bmi(kg, cm) {
  if (cm <= 0) return 0;
  const m = cm / 100;
  return kg / (m * m);
}
export function bmiCategory(b) {
  if (b <= 0) return '–';
  if (b < 18.5) return 'Untergewicht';
  if (b < 25) return 'Normalgewicht';
  if (b < 30) return 'Übergewicht';
  return 'Adipositas';
}
export function bmr(kg, cm, age, sex) {
  const base = 10 * kg + 6.25 * cm - 5 * age;
  return sex === 'MALE' ? base + 5 : sex === 'FEMALE' ? base - 161 : base - 78;
}
export const caloriesMet = (met, kg, minutes) => Math.round(met * kg * (minutes / 60));

// ------------------------------------------------------------------ Penalty

export const PENALTY_REASON = {
  DAILY_MISSED: 'Die Daily Quest wurde nicht rechtzeitig abgeschlossen.',
  INACTIVITY: 'Zu lange keine Aktivität.',
};
export const MAX_DAYS_LOOKBACK = 7;

export const isPastDeadline = (minuteOfDay, deadline) => minuteOfDay >= deadline;
export const minutesUntilDeadline = (minuteOfDay, deadline) => Math.max(0, deadline - minuteOfDay);

export function daysToEvaluate(lastEvaluatedDay, today, pastDeadlineToday, firstDay) {
  const earliest = Math.max(firstDay, today - MAX_DAYS_LOOKBACK, (lastEvaluatedDay ?? firstDay - 1) + 1);
  const last = pastDeadlineToday ? today : today - 1;
  const out = [];
  for (let d = earliest; d <= last; d++) out.push(d);
  return out;
}

export const decideDay = (completed, jokerAvailable) =>
  completed ? 'COMPLETED' : jokerAvailable ? 'JOKER_USED' : 'FAILED';

export function inactivityTriggered(today, lastActiveDay, anchorDay, inactivityDays) {
  if (inactivityDays <= 0) return false;
  const ref = Math.max(lastActiveDay ?? -Infinity, anchorDay);
  return today - ref >= inactivityDays;
}

export function unlockAt(startMs, lockHours, halved, vitality = 10) {
  let h = lockHours;
  if (halved) h /= 2;
  h *= 1 - Math.min(30, Math.max(0, vitality - 10)) / 100;
  return startMs + Math.floor(h * 3_600_000);
}

export const penaltyQuestDone = (bd, bt, cd, ct) => bd >= bt && cd >= ct;

// ------------------------------------------------------------------ Skills & Titel

export const SKILLS = [
  { id: 'SECOND_WIND', name: 'Zweiter Atem', type: 'Passiv', lvl: 5,
    desc: '1 Joker pro Woche: Eine verpasste Daily Quest löst keine Penalty Zone aus.' },
  { id: 'DOUBLE_XP_DAY', name: 'Doppel-XP-Tag', type: 'Aktiv', lvl: 10,
    desc: 'Einmal pro Woche aktivierbar: Alle XP des heutigen Tages werden verdoppelt.' },
  { id: 'ENDURANCE_BOOST', name: 'Ausdauer-Boost', type: 'Passiv', lvl: 8, stat: 'END', val: 15,
    desc: 'Ausdauer-Einheiten zählen 25 % mehr für die Daily Quest.' },
  { id: 'IRON_WILL', name: 'Eiserner Wille', type: 'Passiv', lvl: 12, stat: 'VIT', val: 18,
    desc: 'Die Sperrzeit der Penalty Zone wird halbiert.' },
  { id: 'POWER_SURGE', name: 'Kraftschub', type: 'Passiv', lvl: 6, stat: 'STR', val: 15,
    desc: '+10 % XP auf alle Kraftübungen.' },
  { id: 'SHARP_MIND', name: 'Scharfer Verstand', type: 'Passiv', lvl: 4, stat: 'INT', val: 14,
    desc: '+20 % XP auf Challenges der Kategorie „Anderes“.' },
  { id: 'SHADOW_STEP', name: 'Schattenschritt', type: 'Passiv', lvl: 15, stat: 'AGI', val: 20,
    desc: 'Pausen im Training werden um 20 % verkürzt, Training gibt +10 % XP.' },
  { id: 'MONARCH_AURA', name: 'Aura des Erwachten', type: 'Passiv', lvl: 40,
    desc: '+15 % XP auf alles. Nur für die Stärksten.' },
];
export const skillUnlocked = (s, level, stats) => level >= s.lvl && (!s.stat || stats[s.stat] >= s.val);
export const unlockedSkills = (level, stats) => new Set(SKILLS.filter((s) => skillUnlocked(s, level, stats)).map((s) => s.id));
export const skillRequirement = (s) => `Level ${s.lvl}` + (s.stat ? ` · ${s.stat} ≥ ${s.val}` : '');

export const TITLES = [
  { id: 'AWAKENED', name: 'Der Erwachte', cond: 'Profil erstellt', check: () => true },
  { id: 'PERSISTENT', name: 'Unermüdlich', cond: '7-Tage-Streak', check: (a) => a.bestStreak >= 7 },
  { id: 'IRON_DISCIPLINE', name: 'Eiserne Disziplin', cond: '30-Tage-Streak', check: (a) => a.bestStreak >= 30 },
  { id: 'IRON_FIST', name: 'Eisenfaust', cond: '1000 Liegestütze insgesamt', check: (a) => a.totalPushups >= 1000 },
  { id: 'WIND_RUNNER', name: 'Windläufer', cond: '600 Ausdauer-Minuten insgesamt', check: (a) => a.totalCardioMinutes >= 600 },
  { id: 'WARRIOR', name: 'Krieger', cond: '25 Trainings abgeschlossen', check: (a) => a.totalWorkouts >= 25 },
  { id: 'SURVIVOR', name: 'Überlebender der Penalty Zone', cond: 'Eine Straf-Quest abgeschlossen', check: (a) => a.penaltiesCleared >= 1 },
  { id: 'SCHOLAR', name: 'Gelehrter', cond: '10 Challenges abgeschlossen', check: (a) => a.challengesCompleted >= 10 },
  { id: 'ELITE', name: 'Elite-Hunter', cond: 'Level 35 erreicht', check: (a) => a.level >= 35 },
  { id: 'SHADOW_SOVEREIGN', name: 'Herrscher der Schatten', cond: 'Level 70 erreicht', check: (a) => a.level >= 70 },
];

// ------------------------------------------------------------------ Datum (lokale Zeit)

export function epochDay(date = new Date()) {
  return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86_400_000);
}
export const dateOfDay = (day) => { const u = new Date(day * 86_400_000); return new Date(u.getUTCFullYear(), u.getUTCMonth(), u.getUTCDate()); };
export const minuteOfDay = (d = new Date()) => d.getHours() * 60 + d.getMinutes();
/** Montag der Woche. 1970-01-01 war ein Donnerstag. */
export const weekStart = (day) => day - (((day + 3) % 7) + 7) % 7;

export function streak(completed, today) {
  let d = completed.has(today) ? today : today - 1, n = 0;
  while (completed.has(d)) { n++; d--; }
  return n;
}
export function bestStreak(completed) {
  const s = [...completed].sort((a, b) => a - b);
  if (!s.length) return 0;
  let best = 1, cur = 1;
  for (let i = 1; i < s.length; i++) { cur = s[i] === s[i - 1] + 1 ? cur + 1 : 1; best = Math.max(best, cur); }
  return best;
}

const pad = (n) => String(n).padStart(2, '0');
export const fmtMinuteOfDay = (m) => `${pad(Math.floor(m / 60))}:${pad(m % 60)}`;
export const fmtRemaining = (min) => (min >= 60 ? `${Math.floor(min / 60)}h ${min % 60}min` : `${min}min`);
export function fmtDuration(sec) {
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = Math.floor(sec % 60);
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}
export const fmtDate = (day) => { const d = dateOfDay(day); return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`; };
export const fmtShort = (day) => { const d = dateOfDay(day); return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.`; };
export const fmtNum = (x, digits = 1) => x.toFixed(digits).replace('.', ',');
