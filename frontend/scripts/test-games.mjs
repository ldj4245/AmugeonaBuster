import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import ts from "typescript";

const source = readFileSync(
  new URL("../src/features/games/gameLogic.ts", import.meta.url),
  "utf8",
);
const output = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ES2022,
    target: ts.ScriptTarget.ES2022,
  },
}).outputText;
const { randomInt, resolveScores, diceContenders, games } = await import(
  `data:text/javascript;base64,${Buffer.from(output).toString("base64")}`
);
assert.equal(games.length, 5);
assert.equal(games[0].id, "dice");
const diceScores = [
  { name: "A", value: 12 },
  { name: "B", value: 2 },
  { name: "C", value: 12 },
];
assert.deepEqual(
  diceContenders(diceScores, "high").map((s) => s.name),
  ["A", "C"],
);
assert.deepEqual(
  diceContenders(diceScores, "low").map((s) => s.name),
  ["B"],
);
assert.equal(
  diceContenders(
    [
      { name: "A", value: 6 },
      { name: "C", value: 4 },
    ],
    "high",
  )[0].name,
  "A",
);
for (const invalid of [0, -1, 1.2]) assert.throws(() => randomInt(invalid));
for (let n = 2; n <= 16; n++)
  for (let i = 0; i < 100; i++)
    assert.ok(randomInt(n) >= 0 && randomInt(n) < n);
assert.equal(randomInt(1), 0);
const scores = [
  { name: "가", value: 200, label: "200ms" },
  { name: "나", value: 900, label: "900ms" },
  { name: "다", value: 350, label: "350ms" },
];
const result = resolveScores(scores);
assert.equal(result.loser, "나");
assert.deepEqual(
  result.scores.map((s) => s.value),
  [200, 350, 900],
);
assert.deepEqual(
  scores.map((s) => s.value),
  [200, 900, 350],
  "input must not be mutated",
);
const tied = [...scores, { name: "라", value: 900, label: "900ms" }];
for (let i = 0; i < 100; i++) {
  const r = resolveScores(tied);
  assert.ok(["나", "라"].includes(r.loser));
  assert.ok(r.note);
}
assert.equal(
  resolveScores([
    { name: "정확", value: 0, label: "5초" },
    { name: "초과", value: 250, label: "5.25초" },
  ]).loser,
  "초과",
);
console.log(
  "PASS: 5 games, dice high/low and rerolls, random ranges, loser selection, timing error.",
);
