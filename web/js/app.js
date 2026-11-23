import * as D from './domain.js';
import { Game, minutesEquivalent } from './game.js';
import { BUILD } from './version.js';

const game = new Game();
const $ = (sel, root = document) => root.querySelector(sel);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const isIOS = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const isStandalone = () => navigator.standalone === true || matchMedia('(display-mode: standalone)').matches;

let route = 'status';
let onboarding = { step: 0, data: { sex: 'FEMALE', fitness: 'BEGINNER', goal: 'STRENGTH' } };

// ================================================================== Ton & Vibration

const sound = {
  ctx: null, buffers: {},
  unlock() {
    if (!this.ctx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return;
      this.ctx = new AC();
      for (const name of ['level_up', 'quest_clear', 'system_alert']) {
        fetch(`sounds/${name}.m4a`).then((r) => r.arrayBuffer()).then((b) => this.ctx.decodeAudioData(b))
          .then((buf) => { this.buffers[name] = buf; }).catch(() => {});
      }
    }
    if (this.ctx.state === 'suspended') this.ctx.resume();
  },
  play(name) {
    if (!game.s.soundEnabled || !this.ctx || !this.buffers[name]) return;
    const src = this.ctx.createBufferSource();
    src.buffer = this.buffers[name];
    src.connect(this.ctx.destination);
    src.start();
  },
};
const vibrate = (p) => { if (game.s.vibrationEnabled) navigator.vibrate?.(p); };
['pointerdown', 'touchend', 'keydown'].forEach((t) => addEventListener(t, () => sound.unlock(), { passive: true }));

// ================================================================== Ereignisse aus dem Spiel

const levelQueue = [];
const msgQueue = [];
game.on((ev) => {
  if (ev.type === 'change') return scheduleRender();
  if (ev.type === 'quest') { toast(`[QUEST ERFÜLLT] ${ev.title}  +${ev.xp} XP`); sound.play('quest_clear'); vibrate(40); }
  if (ev.type === 'levelup') levelQueue.push(ev);
  if (ev.type === 'message') msgQueue.push(ev);
  if (ev.type === 'penalty') { sound.play('system_alert'); vibrate([200, 100, 200, 100, 400]); }
  queueMicrotask(showNextOverlay);
});

let renderPending = false;
function scheduleRender() {
  if (renderPending) return;
  renderPending = true;
  requestAnimationFrame(() => { renderPending = false; render(); });
}

function toast(text) {
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = text;
  $('#toasts').append(el);
  setTimeout(() => el.remove(), 2600);
}

function showNextOverlay() {
  if ($('#overlay').childElementCount) return;
  const lv = levelQueue.shift();
  if (lv) {
    sound.play('level_up');
    vibrate([60, 60, 60, 60, 300]);
    $('#overlay').innerHTML = `
      <div class="levelup">
        <div class="ring"></div>
        <div class="sys">[SYSTEM]</div>
        <div class="lu">LEVEL UP!</div>
        <div class="fade head" style="font-size:30px">Level ${lv.level}</div>
        <div class="fade muted">Du bist stärker geworden.</div>
        <div class="fade head" style="color:var(--gold);font-size:19px">+${lv.statPoints} Stat-Punkte verfügbar</div>
        ${lv.newRank ? `<div class="fade head" style="color:var(--gold);font-size:22px">Neuer Rang: ${D.rankFor(lv.level).title}</div>` : ''}
        <button class="btn fill fade" data-act="closeOverlay" style="margin-top:12px">Bestätigen</button>
      </div>`;
    return;
  }
  const msg = msgQueue.shift();
  if (msg) {
    $('#overlay').innerHTML = `<div class="modal">${win(msg.title.replace(/^\[|\]$/g, ''), `
      <p class="txt">${esc(msg.text)}</p><div style="height:14px"></div>
      <button class="btn fill" data-act="closeOverlay">Bestätigen</button>`)}</div>`;
  }
}

// ================================================================== Bausteine

function win(title, body, cls = '', bang = '!') {
  const t = title ? `<div class="win-title"><div class="bang">${bang}</div><h2>${esc(title)}</h2></div>` : '';
  return `<section class="win ${cls}">${t}${body}</section>`;
}
const bar = (p, cls = '') => `<div class="bar ${cls}"><i style="width:${Math.round(Math.min(1, Math.max(0, p)) * 100)}%"></i></div>`;
const kv = (k, v, style = '') => `<div class="kv"><span>${esc(k)}</span><span style="${style}">${v}</span></div>`;
const seg = (name, opts, sel) => `<div class="seg">${Object.entries(opts).map(([k, l]) =>
  `<button data-act="seg" data-name="${name}" data-val="${k}" class="${k === sel ? 'on' : ''}">${esc(typeof l === 'string' ? l : l.label)}</button>`).join('')}</div>`;
const toggle = (act, title, sub, on) => `<div class="switch" data-act="${act}"><div class="grow"><div>${esc(title)}</div>${sub ? `<div class="small muted">${esc(sub)}</div>` : ''}</div><div class="toggle ${on ? 'on' : ''}"></div></div>`;

const ICONS = {
  status: '<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"/><path d="M12 8v8M8 12h8"/>',
  quests: '<path d="M6 3h9l4 4v14H6z"/><path d="M9 12l2 2 4-4"/>',
  training: '<path d="M3 10v4M6 7v10M18 7v10M21 10v4M6 12h12"/>',
  skills: '<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
  stats: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  gear: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z"/>',
  back: '<path d="M15 18l-6-6 6-6"/>',
};
const icon = (n) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${ICONS[n]}</svg>`;

// ================================================================== Hauptrender

function render() {
  const st = game.state;
  const penalty = st.penalty.active;
  document.body.classList.toggle('penalty', penalty && !!st.profile);
  if (!st.profile) { $('#app').innerHTML = renderOnboarding(); return; }
  if (penalty && route !== 'settings') route = 'penalty';
  if (!penalty && route === 'penalty') route = 'status';

  const p = st.profile;
  const tabs = [['status', 'Status'], ['quests', 'Quests'], ['training', 'Training'], ['skills', 'Skills'], ['stats', 'Statistik']];
  const sub = route === 'settings';
  const scrollY = window.scrollY;
  $('#app').innerHTML = `
    <header class="topbar">
      ${sub ? `<button class="icon-btn" data-act="go" data-to="${penalty ? 'penalty' : 'status'}" aria-label="Zurück">${icon('back')}</button>` : ''}
      <div class="logo">${sub ? 'EINSTELLUNGEN' : 'AWAKENED'}</div>
      <div class="lv">LV ${p.level} · ${D.rankFor(p.level).letter}</div>
      ${sub ? '' : `<button class="icon-btn" data-act="go" data-to="settings" aria-label="Einstellungen">${icon('gear')}</button>`}
    </header>
    <main>${SCREENS[route]()}</main>
    ${penalty || sub ? '' : `<nav class="tabbar">${tabs.map(([k, l]) => `<button data-act="go" data-to="${k}" class="${route === k ? 'on' : ''}">${icon(k)}${l}</button>`).join('')}</nav>`}`;
  if (lastRoute === route) window.scrollTo(0, scrollY); else window.scrollTo(0, 0);
  lastRoute = route;
}
let lastRoute = null;

// ================================================================== Onboarding

function renderOnboarding() {
  const o = onboarding, d = o.data;
  const installHint = isIOS && !isStandalone() ? `<div class="banner" data-act="installHelp">📲 Tipp: Erst zum Home-Bildschirm hinzufügen, dann bleiben deine Daten sicher gespeichert. Antippen für Anleitung.</div>` : '';
  let body = '';
  if (o.step === 0) {
    body = win('BENACHRICHTIGUNG', `
      <p class="txt">Du hast die geheimen Bedingungen erfüllt, um ein Spieler zu werden.\n\nDas System wird dich ab heute täglich prüfen. Erfüllst du deine Quests, wirst du stärker. Versagst du, erwartet dich die PENALTY ZONE.\n\nMöchtest du akzeptieren?</p>
      <div style="height:16px"></div><button class="btn fill" data-act="ob" data-step="1">Akzeptieren</button>`);
  } else if (o.step === 1) {
    body = win('SPIELER-REGISTRIERUNG', `<div class="modal-stack" style="display:flex;flex-direction:column;gap:12px">
      <label class="field"><span>Name</span><input class="input" id="ob-name" value="${esc(d.name)}" autocomplete="nickname"></label>
      <div class="grid2">
        <label class="field"><span>Alter</span><input class="input" id="ob-age" inputmode="numeric" value="${esc(d.age)}"></label>
        <label class="field"><span>Grösse (cm)</span><input class="input" id="ob-height" inputmode="decimal" value="${esc(d.heightCm)}"></label>
      </div>
      <label class="field"><span>Gewicht (kg)</span><input class="input" id="ob-weight" inputmode="decimal" value="${esc(d.weightKg)}"></label>
      <div class="field"><span>Geschlecht</span>${seg('ob-sex', D.SEX, d.sex)}</div>
      <p id="ob-err" class="small" style="color:var(--red);margin:0"></p>
      <button class="btn fill" data-act="obRegister">Weiter</button></div>`, '', '?');
  } else if (o.step === 2) {
    body = win('KLASSE WÄHLEN', `<div style="display:flex;flex-direction:column;gap:12px">
      <div class="field"><span>Fitnesslevel</span>${seg('ob-fitness', D.FITNESS, d.fitness)}<div class="small muted">${D.FITNESS_HINT[d.fitness]}</div></div>
      <div class="field"><span>Ziel</span>${seg('ob-goal', D.GOAL, d.goal)}</div>
      <div class="btns"><button class="btn" data-act="ob" data-step="1">Zurück</button><button class="btn fill" data-act="ob" data-step="3">Weiter</button></div></div>`);
  } else {
    const b = D.bmi(d.weightKg, d.heightCm);
    const rows = D.DAILY.map((ex) => {
      const t = ex === 'CARDIO' ? `${D.dailyTarget(1, ex, d.fitness, d.goal)} min` : `${D.dailyTarget(1, ex, d.fitness, d.goal)} ${D.EXERCISES[ex].unit}`;
      return kv(D.EXERCISES[ex].label, t);
    }).join('');
    body = win('SPIELER-ANALYSE', `
      ${kv('Name', esc(d.name))}
      ${kv('BMI', `${D.fmtNum(b)} · ${D.bmiCategory(b)}`)}
      ${kv('Grundumsatz', `${Math.round(D.bmr(d.weightKg, d.heightCm, d.age, d.sex))} kcal/Tag`)}
      <div class="sep"></div><div class="label">Erste Daily Quest</div>${rows}
      <p class="small muted">Die Ziele steigen langsam mit deinem Level.</p>
      <div class="btns"><button class="btn" data-act="ob" data-step="2">Zurück</button><button class="btn fill" data-act="obFinish">Erwachen</button></div>`);
  }
  return `<main style="padding-top:calc(var(--safe-t) + 28px)"><div class="logo head" style="text-align:center;font-size:34px;letter-spacing:.3em;color:var(--cyan);text-shadow:0 0 18px rgba(61,232,255,.6)">AWAKENED</div>${installHint}${body}</main>`;
}

// ================================================================== Screens

const SCREENS = {
  status() {
    const p = game.p, s = game.s;
    const rank = D.rankFor(p.level), nxt = D.nextRank(rank);
    const today = game.ensureDay();
    const rows = Object.values(today.ex);
    const done = rows.filter((r) => r.done >= r.target).length;
    const left = D.minutesUntilDeadline(D.minuteOfDay(), s.deadlineMinute);
    const title = D.TITLES.find((t) => t.id === p.activeTitle) || D.TITLES[0];
    const b = D.bmi(p.weightKg, p.heightCm);
    const statRows = Object.entries(D.STATS).map(([k, st]) => `
      <div class="stat-row"><div class="grow"><div>${k} · ${st.label}</div><div class="small dim">${st.desc}</div></div>
      <div class="val">${p.stats[k]}</div>${p.statPoints > 0 ? `<button class="plus" data-act="stat" data-stat="${k}">+</button>` : ''}</div>`).join('');
    return `
      ${win('STATUS', `
        ${kv('NAME', esc(p.name))}
        <div class="kv" data-act="titles" style="cursor:pointer"><span>TITEL</span><span style="color:var(--violet)">${esc(title.name)} ›</span></div>
        <div class="sep"></div>
        <div class="row"><div class="grow"><div class="label" style="margin:0">LEVEL</div><div class="big">${p.level}</div></div>
          <div style="text-align:right"><div class="label" style="margin:0">RANG</div><div class="rank">${rank.letter}</div></div></div>
        <div style="margin:10px 0 4px" class="row"><span class="grow small muted">XP</span><span class="head">${p.xpInLevel} / ${D.xpForNextLevel(p.level)}</span></div>
        ${bar(D.levelProgress(p.level, p.xpInLevel))}
        <div style="height:8px"></div>
        ${kv('STREAK', `${game.currentStreak()} 🔥`)}
        ${kv('GESAMT-XP', p.totalXp)}
        ${kv('BESTE SERIE', game.bestStreak())}
        ${nxt ? `<p class="small dim" style="margin:6px 0 0">Nächster Rang ${nxt.letter} ab Level ${nxt.min}</p>` : ''}`)}
      ${win('QUEST INFO', `
        ${kv('Daily Quest', `${done} / ${rows.length} erledigt`, done === rows.length ? 'color:var(--green)' : '')}
        ${kv('Deadline', `${D.fmtMinuteOfDay(s.deadlineMinute)} (noch ${D.fmtRemaining(left)})`)}
        ${game.state.meta.doubleXpDay === game.today() ? kv('Aktiv', 'Doppel-XP-Tag', 'color:var(--gold)') : ''}
        <div style="height:10px"></div><button class="btn" data-act="go" data-to="quests">Zur Daily Quest</button>`)}
      ${win('STATS', `${p.statPoints > 0 ? `<p class="head" style="color:var(--gold);margin:0 0 6px;font-size:18px">Verfügbare Punkte: ${p.statPoints}</p>` : ''}${statRows}`)}
      ${win('KÖRPER', `
        ${kv('Grösse', `${Math.round(p.heightCm)} cm`)}
        ${kv('Gewicht', `${D.fmtNum(p.weightKg)} kg`)}
        ${kv('BMI', `${D.fmtNum(b)} · ${D.bmiCategory(b)}`)}
        ${kv('Grundumsatz', `${Math.round(D.bmr(p.weightKg, p.heightCm, p.age, p.sex))} kcal/Tag`)}`)}`;
  },

  quests() {
    const p = game.p, s = game.s;
    const today = game.ensureDay();
    const all = Object.values(today.ex).every((r) => r.done >= r.target);
    const left = D.minutesUntilDeadline(D.minuteOfDay(), s.deadlineMinute);
    const date = new Date().toLocaleDateString('de-CH', { weekday: 'long', day: 'numeric', month: 'long' });
    const quests = D.DAILY.map((ex) => {
      const r = today.ex[ex];
      const ok = r.done >= r.target;
      const steps = r.unit === 'km10' ? [5, 10, 50] : ex === 'JUMP_ROPE' ? [10, 50, 100] : [1, 5, 10];
      const fmtStep = (n) => (r.unit === 'km10' ? `${D.fmtNum(n / 10)} km` : n);
      return `<div class="quest ${ok ? 'done' : ''}">
        <div class="qhead"><span class="name">${ok ? '<span class="check">✓</span> ' : ''}${D.EXERCISES[ex].label}</span>
          <span class="cnt">[${fmtAmount(r.done, r.unit)} / ${fmtAmount(r.target, r.unit)}] ${unitLabel(ex, r)}</span></div>
        ${bar(r.done / r.target, ok ? 'green' : '')}
        <div class="btns">
          <button class="btn sm dim" data-act="prog" data-ex="${ex}" data-d="${-steps[0]}">−${fmtStep(steps[0])}</button>
          ${steps.map((n) => `<button class="btn sm" data-act="prog" data-ex="${ex}" data-d="${n}">+${fmtStep(n)}</button>`).join('')}
        </div>
        <button class="btn sm dim" style="margin-top:8px;min-height:38px" data-act="manual" data-ex="${ex}">Manuell eingeben</button>
      </div>`;
    }).join('');
    const weekly = game.weeklyQuests().map((q) => `
      <div class="quest ${q.claimed ? 'done' : ''}"><div class="qhead"><span class="name">${q.claimed ? '<span class="check">✓</span> ' : ''}${q.title}</span>
        <span class="cnt">${q.claimed ? `✓ +${D.XP.WEEKLY_QUEST}` : `${Math.min(q.progress, q.target)}/${q.target}`}</span></div>
        <div class="small muted" style="margin-bottom:6px">${q.desc}</div>${bar(q.progress / q.target, q.claimed ? 'green' : 'gold')}</div>`).join('');
    const ch = game.state.challenges.filter((c) => !c.archived).map((c) => {
      const pr = game.challengeProgress(c);
      return `<div class="quest ${pr.completed ? 'done' : ''}">
        <div class="qhead"><span class="name">${pr.completed ? '<span class="check">✓</span> ' : ''}${esc(c.name)}</span><span class="cnt">${pr.done}/${c.target} ${esc(c.unit)}</span></div>
        <div class="small muted" style="margin-bottom:6px">${D.CHALLENGE_CATEGORY[c.category]} · ${D.CHALLENGE_FREQ[c.frequency]} · ${c.xp} XP</div>
        ${bar(pr.done / c.target, pr.completed ? 'green' : 'violet')}
        <div class="btns"><button class="btn sm dim" data-act="chDel" data-id="${c.id}">Löschen</button>
          <button class="btn sm dim" data-act="ch" data-id="${c.id}" data-d="-1">−1</button>
          <button class="btn sm" data-act="ch" data-id="${c.id}" data-d="1">+1</button>
          ${pr.completed ? '<button class="btn sm green" disabled>Erfüllt</button>' : `<button class="btn sm green" data-act="ch" data-id="${c.id}" data-d="${c.target - pr.done}">Erfüllt</button>`}</div></div>`;
    }).join('');
    return `
      ${win('QUEST INFO', `
        <div class="head" style="font-size:20px">Tägliche Quest: Training des Erwachten</div>
        <div class="small muted" style="margin-bottom:6px">${date}</div>
        <div class="label">Ziele</div>${quests}
        <p class="small" style="margin:10px 0 0;color:${all ? 'var(--green)' : 'var(--gold)'}">${all ? '✓ Alle Ziele erfüllt · Bonus erhalten' : `Belohnung bei Abschluss aller Ziele: +${D.XP.allDailiesBonus(p.level)} XP Bonus`}</p>`)}
      ${!all && s.penaltyEnabled ? win('WARNUNG', `<p class="txt">Wird die Daily Quest nicht bis ${D.fmtMinuteOfDay(s.deadlineMinute)} abgeschlossen, folgt die PENALTY ZONE. Verbleibende Zeit: ${D.fmtRemaining(left)}</p>`, 'red') : ''}
      ${win('WEEKLY QUESTS', `${weekly}<p class="small dim" style="margin:8px 0 0">Weekly Quests werden jeden Montag zurückgesetzt.</p>`, 'gold')}
      ${win('CHALLENGES', `${ch || '<p class="small muted" style="margin:0 0 10px">Erstelle eigene Challenges, z. B. 20 Seiten lesen, 30 min lernen oder um 6 Uhr aufstehen.</p>'}
        <div style="height:8px"></div><button class="btn" data-act="newCh">Neue Challenge</button>`, 'violet')}`;
  },

  training: () => renderTraining(),

  skills() {
    const p = game.p, meta = game.state.meta, t = game.today();
    const unlocked = game.skills();
    const list = D.SKILLS.map((sk) => {
      const on = unlocked.has(sk.id);
      let extra = '';
      if (on && sk.id === 'SECOND_WIND') extra = `<div class="small" style="color:var(--violet)">${meta.jokerWeekUsed === D.weekStart(t) ? 'Joker diese Woche verbraucht' : 'Joker diese Woche verfügbar (wird automatisch eingesetzt)'}</div>`;
      if (on && sk.id === 'DOUBLE_XP_DAY') {
        extra = meta.doubleXpDay === t ? '<div class="small" style="color:var(--gold)">Heute aktiv: doppelte XP!</div>'
          : meta.doubleXpWeekUsed === D.weekStart(t) ? '<div class="small dim">Diese Woche bereits eingesetzt</div>'
            : '<button class="btn sm gold" style="margin-top:8px" data-act="doubleXp">Heute aktivieren</button>';
      }
      return `<div class="quest"><div class="qhead"><span class="name" style="color:${on ? 'var(--cyan)' : 'var(--dim)'}">${on ? '' : '🔒 '}${sk.name}</span>
        <span class="cnt" style="color:${on ? 'var(--green)' : 'var(--dim)'}">${on ? 'FREI' : ''}</span></div>
        <div class="small ${on ? 'muted' : 'dim'}">${sk.desc}</div><div class="small dim">${sk.type} · ${D.skillRequirement(sk)}</div>${extra}</div>`;
    }).join('');
    const a = game.achievements();
    const titles = D.TITLES.map((ti) => {
      const ok = ti.check(a);
      return `<div class="kv"><span style="color:${ok ? 'var(--text)' : 'var(--dim)'}">${ok ? '★' : '☆'} ${ti.name}${ti.id === p.activeTitle ? ' <span style="color:var(--violet)">(aktiv)</span>' : ''}</span><span class="small" style="font:13px system-ui;color:var(--dim)">${ti.cond}</span></div>`;
    }).join('');
    return `${win('SKILLS', `<p class="small muted" style="margin:0 0 6px">Skills werden durch Level und Stat-Werte freigeschaltet. Verteile deine Punkte im Status gezielt.</p>${list}`)}
      ${win('TITEL', `${titles}<p class="small dim" style="margin:8px 0 0">Aktiven Titel wählst du im Status durch Antippen.</p>`, 'violet')}`;
  },

  stats() {
    const t = game.today(), st = game.state;
    // XP der letzten 14 Tage
    const xs = [];
    for (let d = t - 13; d <= t; d++) xs.push({ d, v: st.days[d]?.xp || 0 });
    const max = Math.max(50, ...xs.map((x) => x.v));
    const bw = 300 / 14;
    const bars = xs.map((x, i) => {
      const h = (x.v / max) * 110;
      return `<rect x="${i * bw + 3}" y="${120 - h}" width="${bw - 6}" height="${h}" rx="2" fill="${x.d === t ? 'var(--cyan)' : 'rgba(61,232,255,.55)'}"/>` +
        (i % 2 === 1 || i === 13 ? `<text x="${i * bw + bw / 2}" y="134" fill="var(--dim)" font-size="8" text-anchor="middle">${D.fmtShort(x.d)}</text>` : '');
    }).join('');
    // Verlauf 28 Tage, beginnend am Montag
    const startHist = D.weekStart(t) - 21;
    let cells = '';
    for (let d = startHist; d < startHist + 28; d++) {
      const r = st.days[d];
      const cls = d > t ? '' : r?.allDone ? 'ok' : r?.outcome === 'JOKER_USED' ? 'joker' : r?.outcome === 'FAILED' ? 'fail' : '';
      cells += `<i class="${cls} ${d === t ? 'today' : ''}" style="${d > t ? 'opacity:.2' : ''}"></i>`;
    }
    const a = game.achievements();
    const kcal = st.workouts.reduce((s, w) => s + w.kcal, 0);
    // Gewicht
    const ws = [...st.weights].sort((x, y) => x.day - y.day || x.id - y.id);
    let weightChart = '<p class="small muted">Trage dein Gewicht regelmässig ein, um den Verlauf zu sehen.</p>';
    if (ws.length >= 2) {
      const min = Math.min(...ws.map((w) => w.kg)) - 1, mx = Math.max(...ws.map((w) => w.kg)) + 1;
      const pts = ws.map((w, i) => [10 + (i / (ws.length - 1)) * 280, 10 + (1 - (w.kg - min) / (mx - min)) * 100]);
      weightChart = `<svg class="chart" viewBox="0 0 300 125">
        <polyline points="${pts.map((p) => p.join(',')).join(' ')}" fill="none" stroke="var(--cyan)" stroke-width="2" style="filter:drop-shadow(0 0 4px var(--cyan))"/>
        ${pts.map((p) => `<circle cx="${p[0]}" cy="${p[1]}" r="3" fill="var(--navy0)" stroke="var(--cyan)" stroke-width="1.5"/>`).join('')}
        <text x="0" y="122" fill="var(--dim)" font-size="9">${D.fmtShort(ws[0].day)}</text>
        <text x="300" y="122" fill="var(--dim)" font-size="9" text-anchor="end">${D.fmtShort(ws.at(-1).day)}</text></svg>
        ${kv('Aktuell', `${D.fmtNum(ws.at(-1).kg)} kg`)}${kv('Veränderung', `Δ ${(ws.at(-1).kg - ws[0].kg >= 0 ? '+' : '')}${D.fmtNum(ws.at(-1).kg - ws[0].kg)} kg`)}`;
    }
    const wlist = [...ws].reverse().slice(0, 8).map((w) => `<div class="row" style="padding:3px 0"><span class="grow muted">${D.fmtDate(w.day)}</span><span class="head">${D.fmtNum(w.kg)} kg</span>
      <button class="icon-btn" style="margin:0;padding:6px;color:var(--dim)" data-act="delWeight" data-id="${w.id}" aria-label="Eintrag löschen">✕</button></div>`).join('');
    const wo = st.workouts.slice(0, 10).map((w) => `<div class="quest"><div class="qhead"><span class="name">${esc(w.title)}</span><span class="cnt" style="color:var(--gold)">+${w.xp} XP</span></div>
      <div class="small muted">${D.fmtDate(w.day)} · ${D.fmtDuration(w.durationSec)} · ${w.kcal} kcal${w.details ? `<br>${esc(w.details)}` : ''}</div></div>`).join('');
    return `
      ${win('XP – LETZTE 14 TAGE', `<svg class="chart" viewBox="0 0 300 140">${bars}</svg>`)}
      ${win('VERLAUF – 28 TAGE', `<div class="history">${cells}</div>
        <div class="legend"><span><i style="background:var(--green)"></i>erfüllt</span><span><i style="background:var(--violet)"></i>Joker</span><span><i style="background:var(--red)"></i>verpasst</span></div>
        <div style="height:8px"></div>${kv('Aktuelle Serie', `${game.currentStreak()} Tage`)}${kv('Beste Serie', `${a.bestStreak} Tage`)}`)}
      ${win('GESAMT', `${kv('Liegestütze', game.total('PUSHUPS'))}${kv('Sit-ups', game.total('SITUPS'))}${kv('Kniebeugen', game.total('SQUATS'))}
        ${kv('Seilsprünge', game.total('JUMP_ROPE'))}${kv('Ausdauer', `${a.totalCardioMinutes} min`)}${kv('Trainings', st.workouts.length)}
        ${kv('Kalorien (Trainings)', `${kcal} kcal`)}${kv('Gesamt-XP', game.p.totalXp)}${kv('Straf-Quests überlebt', a.penaltiesCleared)}`)}
      ${win('GEWICHT', `${weightChart}${wlist ? `<div class="sep"></div>${wlist}` : ''}<div style="height:10px"></div><button class="btn" data-act="addWeight">Gewicht eintragen</button>`)}
      ${st.workouts.length ? win('LETZTE TRAININGS', wo) : ''}`;
  },

  penalty() {
    const s = game.state.penalty;
    const rem = Math.max(0, s.unlockAt - Date.now());
    const hh = Math.floor(rem / 3_600_000), mm = Math.floor((rem % 3_600_000) / 60000), ss = Math.floor((rem % 60000) / 1000);
    return `
      <div style="height:10px"></div><div class="penalty-title">PENALTY ZONE</div>
      ${win('WARNUNG', `<p class="txt">${esc(D.PENALTY_REASON[s.reason] || '')}\n\nDas System ist gesperrt, bis die Straf-Quest erfüllt ist oder die Sperrzeit abläuft.</p>
        <div style="height:10px"></div><div class="small muted" style="text-align:center">Automatisches Ende in</div>
        <div class="timer" data-tick="penalty" style="color:var(--red);text-shadow:0 0 20px rgba(255,46,77,.7)">${hh}:${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}</div>`, 'red')}
      ${win('STRAF-QUEST', `<p class="small muted" style="margin:0 0 8px">Überlebe die Straf-Quest, um die Sperre sofort aufzuheben.</p>
        <div class="quest"><div class="qhead"><span class="name">Burpees</span><span class="cnt">${s.burpeesDone} / ${s.burpeesTarget}</span></div>${bar(s.burpeesDone / s.burpeesTarget, 'red')}
          <div class="btns"><button class="btn sm dim" data-act="pen" data-b="-1">−1</button><button class="btn sm red" data-act="pen" data-b="1">+1</button><button class="btn sm red" data-act="pen" data-b="5">+5</button><button class="btn sm red" data-act="pen" data-b="10">+10</button></div></div>
        <div class="quest"><div class="qhead"><span class="name">Ausdauer</span><span class="cnt">${s.cardioDone} / ${s.cardioTarget} min</span></div>${bar(s.cardioDone / s.cardioTarget, 'red')}
          <div class="btns"><button class="btn sm dim" data-act="pen" data-c="-1">−1</button><button class="btn sm red" data-act="pen" data-c="1">+1</button><button class="btn sm red" data-act="pen" data-c="5">+5</button><button class="btn sm red" data-act="pen" data-c="10">+10</button></div></div>`, 'red')}
      <p class="small muted" style="text-align:center;margin:0">Noch strenger: Sperre Social Media zusätzlich mit der iPhone-Bildschirmzeit (Einstellungen → Hilfe).</p>`;
  },

  settings() {
    const s = game.s, p = game.p;
    const timeVal = D.fmtMinuteOfDay(s.deadlineMinute);
    const opt = (vals, cur, fmt) => vals.map((v) => `<option value="${v}" ${v === cur ? 'selected' : ''}>${fmt(v)}</option>`).join('');
    return `
      ${!isStandalone() ? `<div class="banner" data-act="installHelp">📲 AWAKENED zum Home-Bildschirm hinzufügen – antippen für Anleitung.</div>` : ''}
      ${win('PENALTY ZONE', `
        ${toggle('tPenalty', 'Penalty Zone aktiv', 'Verpasste Daily Quests sperren das System, bis die Straf-Quest erfüllt ist.', s.penaltyEnabled)}
        <div class="grid2" style="margin-top:6px">
          <label class="field"><span>Deadline</span><input class="input" type="time" data-set="deadline" value="${timeVal}"></label>
          <label class="field"><span>Sperrzeit</span><select class="input" data-set="lockHours">${opt([1, 2, 4, 6, 8, 12, 24], s.lockHours, (v) => `${v} h`)}</select></label>
        </div>
        <label class="field" style="margin-top:10px"><span>Inaktivität bis Strafe</span><select class="input" data-set="inactivityDays">${opt([0, 1, 2, 3, 4, 5, 7], s.inactivityDays, (v) => (v === 0 ? 'Aus' : `${v} Tage ohne Aktivität`))}</select></label>
        <p class="small dim" style="margin:6px 0 10px">Die Penalty Zone startet auch, wenn du so viele Tage hintereinander gar nichts einträgst.</p>
        <button class="btn red sm" data-act="testPenalty" ${game.state.penalty.active ? 'disabled' : ''}>Penalty Zone testen</button>`, 'red')}
      ${win('DAILY QUEST', `<div class="field"><span>Ausdauer messen in</span>${seg('cardioUnit', { MINUTES: 'Minuten', KILOMETERS: 'Kilometer' }, s.cardioUnit)}</div>
        <p class="small dim" style="margin:6px 0 0">Gilt ab der nächsten Daily Quest.</p>`)}
      ${win('PROFIL', `<div style="display:flex;flex-direction:column;gap:10px">
        <label class="field"><span>Name</span><input class="input" data-prof="name" value="${esc(p.name)}"></label>
        <div class="grid2"><label class="field"><span>Alter</span><input class="input" inputmode="numeric" data-prof="age" value="${p.age}"></label>
        <label class="field"><span>Grösse (cm)</span><input class="input" inputmode="decimal" data-prof="heightCm" value="${p.heightCm}"></label></div>
        <div class="field"><span>Fitnesslevel</span>${seg('fitness', D.FITNESS, p.fitness)}</div>
        <div class="field"><span>Ziel</span>${seg('goal', D.GOAL, p.goal)}</div>
        <p class="small dim" style="margin:0">Änderungen am Fitnesslevel und Ziel gelten ab der nächsten Daily Quest. Gewicht trägst du unter Statistik ein.</p></div>`)}
      ${win('SPOTIFY', `<label class="field"><span>Playlist-Link</span><input class="input" data-set="spotify" placeholder="https://open.spotify.com/playlist/…" value="${esc(s.spotifyPlaylist)}"></label>
        <p class="small dim" style="margin:6px 0 0">In Spotify: Playlist → ··· → Teilen → Link kopieren. Beim Trainingsstart wird sie geöffnet.</p>`)}
      ${win('TON', `${toggle('tSound', 'Sound', 'Level-up, Quest erfüllt, Warnungen', s.soundEnabled)}${'vibrate' in navigator ? toggle('tVib', 'Vibration', null, s.vibrationEnabled) : ''}`)}
      ${win('HILFE FÜRS IPHONE', `
        <div class="label">Erinnerungen</div>
        <p class="small" style="margin:0">Eine Web-App kann geschlossen keine Erinnerungen schicken. So geht's trotzdem, mit der Kurzbefehle-App:</p>
        <ol class="steps small"><li>Kurzbefehle öffnen → <b>Automation</b> → <b>+</b></li><li><b>Tageszeit</b> wählen, z. B. 20:00, täglich, <b>Sofort ausführen</b></li>
          <li>Aktion <b>Mitteilung anzeigen</b>: „[QUEST INFO] Ist die Daily Quest erledigt?“</li><li>Für eine zweite Warnung kurz vor der Deadline wiederholen.</li></ol>
        <div class="label" style="margin-top:12px">Penalty Zone verschärfen</div>
        <p class="small" style="margin:0">Apps sperren kann nur das iPhone selbst: Einstellungen → <b>Bildschirmzeit</b> → <b>App-Limits</b> → z. B. „Soziale Netzwerke“ auf 30 min pro Tag. Den Code dafür kann jemand anderes festlegen.</p>`, 'gold', '?')}
      ${win('DATEN', `<p class="small muted" style="margin:0 0 10px">Alles bleibt auf diesem Gerät. Mach ab und zu eine Sicherung, z. B. in iCloud Drive.</p>
        <div class="btns"><button class="btn sm" data-act="exportData">Sicherung speichern</button><button class="btn sm" data-act="importData">Wiederherstellen</button></div>
        <input type="file" id="importFile" accept="application/json,.json" class="hidden">
        <div style="height:10px"></div><button class="btn sm red" data-act="wipe">Alles zurücksetzen</button>`)}
      ${win('VERSION', `${kv('Installiert', `Web ${esc(BUILD)}`)}<div id="updState" class="small muted" style="margin:4px 0 10px"></div>
        <button class="btn sm" data-act="checkUpdate">Nach Updates suchen</button>`, 'dim')}`;
  },
};

function fmtAmount(v, unit) { return unit === 'km10' ? D.fmtNum(v / 10) : String(v); }
function unitLabel(ex, r) { return r.unit === 'km10' ? 'km' : r.unit === 'min' ? 'min' : D.EXERCISES[ex].unit; }

// ================================================================== Training

const TR_KEY = 'awakened.training';
let tr = loadTraining();

function loadTraining() {
  try { const t = JSON.parse(localStorage.getItem(TR_KEY)); if (t?.phase === 'RUNNING') return t; } catch { /* leer */ }
  return null;
}
function saveTraining() {
  try { tr?.phase === 'RUNNING' ? localStorage.setItem(TR_KEY, JSON.stringify(tr)) : localStorage.removeItem(TR_KEY); } catch { /* egal */ }
}

function newPlan() {
  const rec = game.ensureDay();
  const rounds = 3;
  const shadow = game.skills().has('SHADOW_STEP');
  const plan = ['PUSHUPS', 'SITUPS', 'SQUATS', 'JUMP_ROPE'].map((ex) => {
    const r = rec.ex[ex];
    const remaining = Math.max(0, r.target - r.done);
    return { ex, reps: Math.max(1, Math.ceil((remaining > 0 ? remaining : r.target) / rounds)) };
  });
  const c = rec.ex.CARDIO;
  const left = Math.max(0, c.target - c.done);
  return {
    phase: 'SETUP', plan, rounds, rest: shadow ? 48 : 60, cardioMin: c.unit === 'km10' ? Math.round(left * 0.6) : left,
    cardioType: 'RUNNING', spotify: !!game.s.spotifyPlaylist,
  };
}

function renderTraining() {
  if (!tr) tr = newPlan();
  if (tr.phase === 'SETUP') {
    const stepper = (label, key, val, unit = '') => `<div class="row" style="padding:6px 0"><span class="grow head" style="font-size:18px">${label}</span>
      <div class="stepper"><button data-act="trSet" data-key="${key}" data-d="-1">−</button><span class="v">${val}${unit}</span><button data-act="trSet" data-key="${key}" data-d="1">+</button></div></div>`;
    return `${win('TRAINING', `<p class="small muted" style="margin:0 0 8px">Der Plan basiert auf deinen offenen Daily-Zielen. Wiederholungen pro Runde:</p>
      ${tr.plan.map((it) => stepper(D.EXERCISES[it.ex].label, `rep:${it.ex}`, it.reps)).join('')}
      <div class="sep"></div>
      ${stepper('Runden', 'rounds', tr.rounds)}${stepper('Pause', 'rest', tr.rest, ' s')}${stepper('Ausdauer danach', 'cardioMin', tr.cardioMin, ' min')}
      ${tr.cardioMin > 0 ? `<div style="margin:6px 0">${seg('cardioType', D.CARDIO_TYPE, tr.cardioType)}</div>` : ''}
      ${game.s.spotifyPlaylist ? toggle('trSpotify', 'Spotify-Playlist öffnen', 'Beim Start', tr.spotify) : '<p class="small dim">Tipp: Spotify-Playlist in den Einstellungen hinterlegen.</p>'}
      <div style="height:12px"></div><button class="btn fill" data-act="trStart">Training starten</button>`)}`;
  }
  if (tr.phase === 'DONE') {
    const s = tr.summary;
    return win('TRAINING ABGESCHLOSSEN', `
      ${kv('Dauer', D.fmtDuration(s.durationSec))}
      ${Object.entries(s.reps).filter(([, n]) => n > 0).map(([ex, n]) => kv(D.EXERCISES[ex].label, n)).join('')}
      ${s.cardio ? kv('Ausdauer', `${s.cardio} min`) : ''}
      ${kv('Kalorien (geschätzt)', `${s.kcal} kcal`)}
      ${kv('Belohnung', `+${s.xp} XP`, 'color:var(--gold)')}
      <div style="height:12px"></div><button class="btn fill" data-act="trReset">Fertig</button>`, 'green', '✓');
  }
  // RUNNING
  const step = tr.steps[tr.index];
  const works = tr.steps.filter((x) => x.k !== 'rest').length;
  const worksDone = tr.steps.slice(0, tr.index).filter((x) => x.k !== 'rest').length;
  let main = '';
  if (step.k === 'work') {
    main = `<div class="small muted" style="text-align:center">Runde ${step.round} von ${tr.rounds}</div>
      <div class="step-name">${D.EXERCISES[step.ex].label}</div><div class="timer" style="font-size:72px">${step.reps}</div><div class="step-sub">${D.EXERCISES[step.ex].unit}</div>
      <div style="height:12px"></div><button class="btn fill" data-act="trDone">Erledigt</button>`;
  } else if (step.k === 'rest') {
    main = `<div class="step-name" style="color:var(--muted)">PAUSE</div><div class="timer" data-tick="rest">${D.fmtDuration(Math.ceil(Math.max(0, tr.restEnd - nowT()) / 1000))}</div>
      <div class="step-sub">Als Nächstes: ${esc(step.next)}</div><div style="height:12px"></div>
      <div class="btns"><button class="btn sm" data-act="trRest">+15 s</button><button class="btn sm fill" data-act="trSkip">Überspringen</button></div>`;
  } else {
    main = `<div class="step-name">${D.CARDIO_TYPE[step.type].label}</div><div class="timer" data-tick="step">${D.fmtDuration(Math.floor((nowT() - tr.stepStart) / 1000))}</div>
      <div class="step-sub">Ziel: ${step.min} min</div><div style="height:12px"></div>
      <div class="btns"><button class="btn sm" data-act="trCardioTarget">Ziel erreicht</button><button class="btn sm fill" data-act="trDone">Beenden</button></div>`;
  }
  return `${win('TRAINING', `
      <div class="row"><span class="grow small muted">Gesamtzeit</span><span class="head" data-tick="total" style="font-size:20px">${D.fmtDuration(Math.floor((nowT() - tr.startedAt) / 1000))}</span></div>
      ${bar(worksDone / works)}<div style="height:16px"></div>${main}
      ${tr.pausedAt ? '<p class="head" style="text-align:center;color:var(--gold);margin:10px 0 0">ANGEHALTEN</p>' : ''}`)}
    <div class="btns"><button class="btn sm" data-act="trPause">${tr.pausedAt ? 'Fortsetzen' : 'Anhalten'}</button>
      ${step.k === 'work' ? '<button class="btn sm dim" data-act="trSkip">Überspringen</button>' : ''}
      <button class="btn sm red" data-act="trFinish">Beenden</button></div>
    ${game.s.spotifyPlaylist ? '<button class="btn sm dim" data-act="spotify">Spotify öffnen</button>' : ''}`;
}

const nowT = () => (tr?.pausedAt ? tr.pausedAt : Date.now());

function trStart() {
  const items = tr.plan.filter((x) => x.reps > 0);
  const steps = [];
  for (let r = 1; r <= tr.rounds; r++) {
    items.forEach((it, i) => {
      steps.push({ k: 'work', ex: it.ex, reps: it.reps, round: r });
      const last = r === tr.rounds && i === items.length - 1;
      if (!last) steps.push({ k: 'rest', sec: tr.rest, next: D.EXERCISES[(items[i + 1] || items[0]).ex].label });
    });
  }
  if (tr.cardioMin > 0) {
    if (steps.length) steps.push({ k: 'rest', sec: tr.rest, next: D.CARDIO_TYPE[tr.cardioType].label });
    steps.push({ k: 'cardio', min: tr.cardioMin, type: tr.cardioType });
  }
  if (!steps.length) return;
  const now = Date.now();
  Object.assign(tr, { phase: 'RUNNING', steps, index: 0, startedAt: now, stepStart: now, restEnd: 0, pausedAt: null,
    reps: {}, cardioDone: 0, activeMs: {}, restMs: 0, cardioMs: 0 });
  if (tr.spotify && game.s.spotifyPlaylist) openSpotify();
  requestWakeLock();
  saveTraining();
}

function accountStep() {
  const step = tr.steps[tr.index];
  const ms = nowT() - tr.stepStart;
  if (step.k === 'work') tr.activeMs[step.ex] = (tr.activeMs[step.ex] || 0) + ms;
  else if (step.k === 'rest') tr.restMs += ms;
  else tr.cardioMs += ms;
}

function trNext() {
  accountStep();
  tr.index++;
  if (tr.index >= tr.steps.length) { tr.index--; return trFinish(); }
  const step = tr.steps[tr.index];
  tr.stepStart = nowT();
  tr.restEnd = step.k === 'rest' ? tr.stepStart + step.sec * 1000 : 0;
  saveTraining();
  render();
}

function trFinish(fromButton = false) {
  if (fromButton) accountStep();
  const durationSec = Math.floor((nowT() - tr.startedAt) / 1000);
  tr.pausedAt = null;
  const kg = game.p.weightKg;
  let kcal = 0;
  for (const [ex, ms] of Object.entries(tr.activeMs)) kcal += D.EXERCISES[ex].met * kg * (ms / 3_600_000);
  kcal += 1.8 * kg * (tr.restMs / 3_600_000);
  kcal += D.CARDIO_TYPE[tr.cardioType].met * kg * (tr.cardioMs / 3_600_000);
  const hasReps = Object.values(tr.reps).some((n) => n > 0);
  const title = !hasReps && tr.cardioDone > 0 ? D.CARDIO_TYPE[tr.cardioType].label : tr.cardioDone > 0 ? `Kraft + ${D.CARDIO_TYPE[tr.cardioType].label}` : 'Krafttraining';
  const res = game.finishTraining({ title, startedAt: tr.startedAt, durationSec, reps: tr.reps, cardioMinutes: tr.cardioDone, kcal: Math.round(kcal) });
  tr = { phase: 'DONE', summary: { durationSec, reps: tr.reps, cardio: tr.cardioDone, kcal: res.kcal, xp: res.xp } };
  saveTraining();
  releaseWakeLock();
  render();
}

let wakeLock = null;
async function requestWakeLock() { try { wakeLock = await navigator.wakeLock?.request('screen'); } catch { /* nicht unterstützt */ } }
function releaseWakeLock() { wakeLock?.release?.().catch(() => {}); wakeLock = null; }

function openSpotify() {
  const m = game.s.spotifyPlaylist.match(/(playlist|album|artist|track)[/:]([A-Za-z0-9]{10,})/);
  if (!m) return toast('Kein gültiger Spotify-Link hinterlegt.');
  window.open(`https://open.spotify.com/${m[1]}/${m[2]}`, '_blank');
}

// Ticker: Uhren aktualisieren, Pausen automatisch beenden, Penalty-Countdown
setInterval(() => {
  if (route === 'training' && tr?.phase === 'RUNNING' && !tr.pausedAt) {
    const step = tr.steps[tr.index];
    if (step.k === 'rest' && Date.now() >= tr.restEnd) { sound.play('quest_clear'); vibrate(80); return trNext(); }
    const set = (k, v) => { const el = document.querySelector(`[data-tick="${k}"]`); if (el) el.textContent = v; };
    set('total', D.fmtDuration(Math.floor((Date.now() - tr.startedAt) / 1000)));
    set('rest', D.fmtDuration(Math.ceil(Math.max(0, tr.restEnd - Date.now()) / 1000)));
    set('step', D.fmtDuration(Math.floor((Date.now() - tr.stepStart) / 1000)));
  }
  if (route === 'penalty') {
    if (Date.now() >= game.state.penalty.unlockAt) { game.evaluatePenalties(); return; }
    const el = document.querySelector('[data-tick="penalty"]');
    if (el) {
      const rem = Math.max(0, game.state.penalty.unlockAt - Date.now());
      el.textContent = `${Math.floor(rem / 3_600_000)}:${String(Math.floor((rem % 3_600_000) / 60000)).padStart(2, '0')}:${String(Math.floor((rem % 60000) / 1000)).padStart(2, '0')}`;
    }
  }
}, 250);

// ================================================================== Dialoge

function modal(html) { $('#overlay').innerHTML = `<div class="modal" data-act="modalBg">${html}</div>`; }
function closeModal() { $('#overlay').innerHTML = ''; showNextOverlay(); }

function numberDialog(title, text, value, onSave, step = 'any') {
  modal(win(title, `<div class="stack"><p class="txt small muted">${esc(text)}</p>
    <input class="input" id="dlgNum" type="number" inputmode="decimal" step="${step}" value="${esc(value)}">
    <div class="btns"><button class="btn dim" data-act="closeOverlay">Abbrechen</button><button class="btn fill" data-act="dlgSave">Speichern</button></div></div>`));
  dialogSave = () => { const v = parseFloat(String($('#dlgNum').value).replace(',', '.')); if (Number.isFinite(v)) { onSave(v); closeModal(); } };
  setTimeout(() => $('#dlgNum')?.focus(), 50);
}
function confirmDialog(title, text, label, onYes, cls = 'red') {
  modal(win(title, `<div class="stack"><p class="txt">${esc(text)}</p>
    <div class="btns"><button class="btn dim" data-act="closeOverlay">Abbrechen</button><button class="btn fill ${cls}" data-act="dlgSave">${esc(label)}</button></div></div>`, cls));
  dialogSave = () => { closeModal(); onYes(); };
}
let dialogSave = null;

function installHelp() {
  modal(win('ZUM HOME-BILDSCHIRM', `<div class="stack">
    <ol class="steps"><li>Diese Seite in <b>Safari</b> öffnen.</li><li>Unten auf das <b>Teilen-Symbol</b> tippen (Quadrat mit Pfeil nach oben).</li>
      <li>Nach unten scrollen → <b>Zum Home-Bildschirm</b>.</li><li>Oben rechts <b>Hinzufügen</b>.</li></ol>
    <p class="small muted" style="margin:0">Danach AWAKENED immer über das neue Icon öffnen. Es startet im Vollbild, und deine Daten bleiben dort dauerhaft gespeichert. Achtung: Safari-Tab und Icon haben getrennte Speicher.</p>
    <button class="btn fill" data-act="closeOverlay">Verstanden</button></div>`, 'gold', '?'));
}

function challengeDialog() {
  const d = { category: 'OTHER', frequency: 'DAILY' };
  const draw = () => modal(win('NEUE CHALLENGE', `<div class="stack">
    <label class="field"><span>Name</span><input class="input" id="chName" placeholder="z. B. 20 Seiten lesen" value="${esc(d.name)}"></label>
    <div class="field"><span>Kategorie</span>${seg('chCat', D.CHALLENGE_CATEGORY, d.category)}</div>
    <div class="field"><span>Wiederholung</span>${seg('chFreq', D.CHALLENGE_FREQ, d.frequency)}</div>
    <div class="grid2"><label class="field"><span>Ziel</span><input class="input" id="chTarget" inputmode="numeric" value="${esc(d.target ?? 1)}"></label>
      <label class="field"><span>Einheit</span><input class="input" id="chUnit" placeholder="Seiten, min, ×" value="${esc(d.unit ?? '')}"></label></div>
    <label class="field"><span>XP-Belohnung</span><input class="input" id="chXp" inputmode="numeric" value="${esc(d.xp ?? 30)}"></label>
    <p class="small dim" style="margin:0">Tipp: Einfache Gewohnheit 20–50 XP, grosse Ziele 100–300 XP.</p>
    <div class="btns"><button class="btn dim" data-act="closeOverlay">Abbrechen</button><button class="btn fill" data-act="dlgSave">Erstellen</button></div></div>`, 'violet'));
  const read = () => Object.assign(d, { name: $('#chName').value, target: $('#chTarget').value, unit: $('#chUnit').value, xp: $('#chXp').value });
  challengeSeg = (name, val) => { read(); d[name === 'chCat' ? 'category' : 'frequency'] = val; draw(); };
  dialogSave = () => {
    read();
    if (!d.name.trim()) return;
    game.createChallenge({ name: d.name, category: d.category, frequency: d.frequency, target: parseInt(d.target, 10) || 1, unit: d.unit, xp: parseInt(d.xp, 10) || 30 });
    closeModal();
  };
  draw();
}
let challengeSeg = null;

function titleDialog() {
  const a = game.achievements();
  modal(win('TITEL WÄHLEN', `<div class="stack">${D.TITLES.map((t) => {
    const ok = t.check(a);
    return `<button class="btn sm ${ok ? (t.id === game.p.activeTitle ? 'fill' : '') : 'dim'}" ${ok ? `data-act="setTitle" data-id="${t.id}"` : 'disabled'} style="text-transform:none;text-align:left">
      ${t.name}<br><span style="font:12px system-ui;color:var(--muted)">${ok ? 'Freigeschaltet' : `Bedingung: ${t.cond}`}</span></button>`;
  }).join('')}
    <p class="small dim" style="margin:0">Titel erhältst du für Meilensteine wie Streaks, Wiederholungen und Level.</p>
    <button class="btn" data-act="closeOverlay">Schliessen</button></div>`, 'violet'));
}

async function exportData() {
  const name = `awakened-backup-${new Date().toISOString().slice(0, 10)}.json`;
  const file = new File([game.exportJson()], name, { type: 'application/json' });
  if (navigator.canShare?.({ files: [file] })) {
    try { await navigator.share({ files: [file], title: 'AWAKENED-Sicherung' }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(file); a.download = name; a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 5000);
}

// ================================================================== Aktionen

const ACTIONS = {
  go: (el) => { route = el.dataset.to; render(); },
  closeOverlay: () => closeModal(),
  modalBg: (el, ev) => { if (ev.target === el && !msgQueue.length) { /* Klick daneben schliesst nicht – bewusst */ } },
  dlgSave: () => dialogSave?.(),
  installHelp,

  ob: (el) => {
    if (onboarding.step === 1 && !readRegistration(false)) return;
    onboarding.step = Number(el.dataset.step); render(); window.scrollTo(0, 0);
  },
  obRegister: () => { if (readRegistration(true)) { onboarding.step = 2; render(); } },
  obFinish: () => {
    game.createProfile(onboarding.data);
    route = 'status';
    msgQueue.push({ title: '[SYSTEM]', text: `Willkommen, Spieler ${game.p.name}.\n\nDeine erste Daily Quest wartet. Heute gilt noch die Schonfrist, ab morgen zählt es.` });
    render(); showNextOverlay();
  },
  seg: (el) => {
    const { name, val } = el.dataset;
    if (name.startsWith('ob-')) {
      if (onboarding.step === 1) readRegistration(false);
      onboarding.data[name.slice(3)] = val; render(); return;
    }
    if (name === 'chCat' || name === 'chFreq') return challengeSeg?.(name, val);
    if (name === 'cardioType') { tr.cardioType = val; render(); return; }
    if (name === 'cardioUnit') { game.s.cardioUnit = val; game.commit(); return; }
    if (name === 'fitness' || name === 'goal') game.updateProfile({ [name]: val });
  },

  stat: (el) => game.allocateStat(el.dataset.stat),
  titles: titleDialog,
  setTitle: (el) => { game.setTitle(el.dataset.id); closeModal(); },
  prog: (el) => game.addProgress(el.dataset.ex, Number(el.dataset.d)),
  manual: (el) => {
    const ex = el.dataset.ex, r = game.ensureDay().ex[ex];
    const km = r.unit === 'km10';
    numberDialog(`${D.EXERCISES[ex].label.toUpperCase()} EINTRAGEN`, `Gib den Gesamtwert für heute ein (${unitLabel(ex, r)}).`,
      km ? r.done / 10 : r.done, (v) => game.setProgress(ex, Math.round(km ? v * 10 : v)));
  },
  ch: (el) => game.addChallengeProgress(Number(el.dataset.id), Number(el.dataset.d)),
  chDel: (el) => {
    const c = game.state.challenges.find((x) => x.id === Number(el.dataset.id));
    confirmDialog('CHALLENGE LÖSCHEN', `„${c.name}“ und den Fortschritt wirklich löschen?`, 'Löschen', () => game.deleteChallenge(c.id));
  },
  newCh: challengeDialog,
  doubleXp: () => game.activateDoubleXp(),
  addWeight: () => numberDialog('GEWICHT EINTRAGEN', 'Gewicht (kg)', game.p.weightKg, (v) => { if (v > 20 && v < 400) game.addWeight(Math.round(v * 10) / 10); }),
  delWeight: (el) => confirmDialog('EINTRAG LÖSCHEN', 'Diesen Gewichtseintrag löschen?', 'Löschen', () => game.deleteWeight(Number(el.dataset.id))),
  pen: (el) => game.addPenaltyProgress(Number(el.dataset.b || 0), Number(el.dataset.c || 0)),

  trSet: (el) => {
    const [key, ex] = el.dataset.key.split(':'), d = Number(el.dataset.d);
    if (key === 'rep') { const it = tr.plan.find((x) => x.ex === ex); it.reps = clamp(it.reps + d * (ex === 'JUMP_ROPE' ? 10 : 1), 0, 500); }
    if (key === 'rounds') tr.rounds = clamp(tr.rounds + d, 1, 10);
    if (key === 'rest') tr.rest = clamp(tr.rest + d * 10, 10, 300);
    if (key === 'cardioMin') tr.cardioMin = clamp(tr.cardioMin + d * 5, 0, 180);
    render();
  },
  trSpotify: () => { tr.spotify = !tr.spotify; render(); },
  trStart: () => { trStart(); render(); },
  trDone: () => {
    const step = tr.steps[tr.index];
    if (step.k === 'work') tr.reps[step.ex] = (tr.reps[step.ex] || 0) + step.reps;
    if (step.k === 'cardio') tr.cardioDone += Math.max(1, Math.floor((nowT() - tr.stepStart) / 60000));
    trNext();
  },
  trCardioTarget: () => {
    const step = tr.steps[tr.index];
    const missing = step.min * 60000 - (nowT() - tr.stepStart);
    if (missing > 0) tr.cardioMs += missing;
    tr.cardioDone += step.min;
    trNext();
  },
  trSkip: () => trNext(),
  trRest: () => { tr.restEnd += 15000; saveTraining(); render(); },
  trPause: () => {
    if (tr.pausedAt) {
      const d = Date.now() - tr.pausedAt;
      tr.startedAt += d; tr.stepStart += d; if (tr.restEnd) tr.restEnd += d; tr.pausedAt = null;
    } else tr.pausedAt = Date.now();
    saveTraining(); render();
  },
  trFinish: () => confirmDialog('TRAINING BEENDEN', 'Training jetzt beenden? Erledigte Übungen werden gutgeschrieben.', 'Beenden', () => trFinish(true), 'gold'),
  trReset: () => { tr = null; render(); },
  spotify: openSpotify,

  tPenalty: () => { game.s.penaltyEnabled = !game.s.penaltyEnabled; game.commit(); },
  tSound: () => { game.s.soundEnabled = !game.s.soundEnabled; game.commit(); },
  tVib: () => { game.s.vibrationEnabled = !game.s.vibrationEnabled; game.commit(); },
  testPenalty: () => confirmDialog('PENALTY ZONE TESTEN', 'Die Penalty Zone startet sofort, mit echter Straf-Quest und Sperrzeit. Fortfahren?', 'Starten', () => { game.triggerPenaltyManually(); route = 'penalty'; render(); }),
  exportData,
  importData: () => $('#importFile').click(),
  wipe: () => confirmDialog('ALLES ZURÜCKSETZEN', 'Profil, Fortschritt und Einstellungen werden auf diesem Gerät gelöscht. Das lässt sich nicht rückgängig machen.', 'Löschen', () => {
    game.wipe(); tr = null; saveTraining(); onboarding = { step: 0, data: { sex: 'FEMALE', fitness: 'BEGINNER', goal: 'STRENGTH' } }; render();
  }),
  checkUpdate: () => checkUpdate(true),
};
const clamp = (v, a, b) => Math.min(b, Math.max(a, v));

function readRegistration(strict) {
  const d = onboarding.data;
  const num = (id) => parseFloat(String($(id)?.value ?? '').replace(',', '.'));
  d.name = $('#ob-name')?.value ?? d.name;
  d.age = parseInt($('#ob-age')?.value, 10) || d.age;
  d.heightCm = num('#ob-height') || d.heightCm;
  d.weightKg = num('#ob-weight') || d.weightKg;
  if (!strict) return true;
  const ok = d.name?.trim() && d.age >= 10 && d.age <= 100 && d.heightCm >= 100 && d.heightCm <= 250 && d.weightKg >= 30 && d.weightKg <= 300;
  if (!ok) $('#ob-err').textContent = 'Bitte alle Felder plausibel ausfüllen.';
  return ok;
}

document.addEventListener('click', (ev) => {
  const el = ev.target.closest('[data-act]');
  if (!el) return;
  const fn = ACTIONS[el.dataset.act];
  if (fn) fn(el, ev);
});

document.addEventListener('change', (ev) => {
  const el = ev.target;
  if (el.id === 'importFile' && el.files?.[0]) {
    el.files[0].text().then((t) => {
      confirmDialog('WIEDERHERSTELLEN', 'Die Sicherung ersetzt alle aktuellen Daten auf diesem Gerät.', 'Wiederherstellen', () => {
        try { game.importJson(t); toast('Sicherung wiederhergestellt.'); } catch (e) { toast(e.message || 'Datei ungültig.'); }
      }, 'gold');
    });
    el.value = '';
    return;
  }
  const key = el.dataset.set, prof = el.dataset.prof;
  if (key === 'deadline' && el.value) { const [h, m] = el.value.split(':').map(Number); game.s.deadlineMinute = h * 60 + m; game.commit(); }
  if (key === 'lockHours') { game.s.lockHours = Number(el.value); game.commit(); }
  if (key === 'inactivityDays') { game.s.inactivityDays = Number(el.value); game.commit(); }
  if (key === 'spotify') { game.s.spotifyPlaylist = el.value.trim(); game.commit(); }
  if (prof) {
    let v = el.value.trim();
    if (prof !== 'name') v = parseFloat(v.replace(',', '.'));
    const ok = prof === 'name' ? v.length > 0 : prof === 'age' ? v >= 10 && v <= 100 : v >= 100 && v <= 250;
    if (ok) game.updateProfile({ [prof]: prof === 'age' ? Math.round(v) : v }); else render();
  }
});

// ================================================================== Updates (Service Worker)

let swReg = null;
async function checkUpdate(manual) {
  const out = $('#updState');
  if (!swReg) { if (out) out.textContent = 'Updates werden nur in der installierten Web-App geprüft.'; return; }
  if (out) out.textContent = 'Suche …';
  try { await swReg.update(); } catch { if (out) out.textContent = 'Keine Verbindung.'; return; }
  setTimeout(() => {
    if (swReg.waiting || swReg.installing) offerUpdate();
    else if (manual && $('#updState')) $('#updState').textContent = 'Du hast die neueste Version.';
  }, 1500);
}
function offerUpdate() {
  const out = $('#updState');
  if (out) out.innerHTML = '<span style="color:var(--gold)">Neue Version verfügbar.</span>';
  if ($('#updBanner')) return;
  const b = document.createElement('div');
  b.id = 'updBanner'; b.className = 'banner';
  b.style.cssText = 'position:fixed;left:16px;right:16px;bottom:calc(var(--safe-b) + 84px);z-index:20;text-align:center';
  b.textContent = '⬆ Neue Version von AWAKENED – antippen zum Aktualisieren';
  b.onclick = () => { (swReg.waiting || swReg.installing)?.postMessage('SKIP_WAITING'); b.textContent = 'Aktualisiere …'; };
  document.body.append(b);
}
if ('serviceWorker' in navigator && BUILD !== 'dev') {
  navigator.serviceWorker.register('sw.js').then((reg) => {
    swReg = reg;
    if (reg.waiting) offerUpdate();
    reg.addEventListener('updatefound', () => {
      const w = reg.installing;
      w?.addEventListener('statechange', () => { if (w.state === 'installed' && navigator.serviceWorker.controller) offerUpdate(); });
    });
  }).catch(() => {});
  let reloading = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => { if (!reloading) { reloading = true; location.reload(); } });
}

// ================================================================== Start

function refresh() {
  game.evaluatePenalties();
  render();
}
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') { refresh(); checkUpdate(false); } });
navigator.storage?.persist?.().catch(() => {});
if (tr?.phase === 'RUNNING') route = 'training';
refresh();
setInterval(() => { if (game.p && route !== 'training' && !$('#overlay').childElementCount && document.activeElement?.tagName !== 'INPUT') refresh(); }, 60_000);
