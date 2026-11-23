import test from 'node:test';
import assert from 'node:assert/strict';
import * as D from '../js/domain.js';
import { Game } from '../js/game.js';

class MemStorage { constructor() { this.m = {}; } getItem(k) { return this.m[k] ?? null; } setItem(k, v) { this.m[k] = v; } }

function newGame(start = new Date(2026, 10, 23, 10, 0)) {
  let now = start;
  const g = new Game(new MemStorage(), () => now);
  g.setNow = (d) => { now = d; };
  g.createProfile({ name: 'Test', age: 25, sex: 'FEMALE', heightCm: 168, weightKg: 60, fitness: 'BEGINNER', goal: 'STRENGTH' });
  return g;
}

test('XP-Kurve 100 × Level^1.5', () => {
  assert.equal(D.xpForNextLevel(1), 100);
  assert.equal(D.xpForNextLevel(2), 282);
  assert.equal(D.xpForNextLevel(10), 3162);
  const r = D.addXp(1, 0, 400);
  assert.deepEqual([r.level, r.xpInLevel, r.statPoints], [3, 18, 6]); // 400 − 100 − 282
});

test('Ränge', () => {
  assert.equal(D.rankFor(1).letter, 'E');
  assert.equal(D.rankFor(19).letter, 'D');
  assert.equal(D.rankFor(70).letter, 'S');
});

test('Quest-Skalierung wie Android', () => {
  assert.equal(D.dailyTarget(1, 'PUSHUPS', 'BEGINNER', 'STRENGTH'), 10); // 12 → 10
  assert.equal(D.dailyTarget(1, 'JUMP_ROPE', 'INTERMEDIATE', 'ENDURANCE'), 180);
  assert.equal(D.dailyTarget(200, 'PUSHUPS', 'ADVANCED', 'STRENGTH'), 100);
  assert.equal(D.cardioMinutesToTenthKm(10), 15);
});

test('Wochenstart ist Montag', () => {
  const mon = D.epochDay(new Date(2026, 10, 23)); // 23.11.2026 = Montag
  assert.equal(D.weekStart(mon), mon);
  assert.equal(D.weekStart(mon + 6), mon);
  assert.equal(D.weekStart(mon + 7), mon + 7);
});

test('Grundumsatz Mifflin-St Jeor', () => {
  assert.equal(D.bmr(60, 168, 25, 'FEMALE'), 600 + 1050 - 125 - 161);
});

test('Daily Quest abschliessen gibt XP und Bonus', () => {
  const g = newGame();
  const rec = g.ensureDay();
  for (const ex of D.DAILY) g.setProgress(ex, rec.ex[ex].target);
  assert.ok(g.day(g.today()).allDone);
  assert.ok(g.p.totalXp >= 5 * 20 + 52);
});

test('Verpasste Daily löst Penalty Zone aus, Straf-Quest hebt sie auf', () => {
  const g = newGame(new Date(2026, 10, 23, 10, 0));
  g.setNow(new Date(2026, 10, 25, 9, 0)); // 24.11. verpasst
  g.evaluatePenalties();
  assert.equal(g.state.penalty.active, true);
  const { burpeesTarget, cardioTarget } = g.state.penalty;
  g.addPenaltyProgress(burpeesTarget, cardioTarget);
  assert.equal(g.state.penalty.active, false);
  assert.equal(g.state.meta.penaltiesCleared, 1);
});

test('Tag der Profilerstellung ist Schonfrist', () => {
  const g = newGame(new Date(2026, 10, 23, 10, 0));
  g.setNow(new Date(2026, 10, 23, 23, 59));
  g.evaluatePenalties();
  assert.equal(g.state.penalty.active, false);
});

test('Sperre endet automatisch nach Ablauf', () => {
  const g = newGame(new Date(2026, 10, 23, 10, 0));
  g.setNow(new Date(2026, 10, 25, 9, 0));
  g.evaluatePenalties();
  g.setNow(new Date(2026, 10, 25, 22, 0));
  g.ensureDay(); // heute aktiv machen, damit keine Inaktivität greift
  g.addProgress('PUSHUPS', 1);
  g.evaluatePenalties();
  assert.equal(g.state.penalty.active, false);
});

test('Backup-Export und -Import', () => {
  const g = newGame();
  g.addProgress('PUSHUPS', 7);
  const json = g.exportJson();
  const h = new Game(new MemStorage(), () => new Date(2026, 10, 23, 12));
  h.importJson(json);
  assert.equal(h.day(h.today()).ex.PUSHUPS.done, 7);
  assert.equal(h.p.name, 'Test');
});
