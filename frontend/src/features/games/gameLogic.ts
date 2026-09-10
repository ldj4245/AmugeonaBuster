export type GameId = "dice" | "reaction" | "timer" | "beans" | "draw";
export interface Score {
  name: string;
  value: number;
  label: string;
}
export interface GameResult {
  loser: string;
  scores?: Score[];
  note?: string;
}
export const games = [
  {
    id: "dice",
    title: "주사위 2개",
    label: "주사위",
    description: "주사위 두 개의 합계로 결정합니다.",
    duration: "1분",
    type: "주사위",
    color: "neutral",
    mark: "⚄",
  },
  {
    id: "reaction",
    title: "반응속도 대결",
    label: "반응속도 대결",
    description: "신호가 바뀌면 누르고 기록을 비교합니다.",
    duration: "1~2분",
    type: "순발력",
    color: "mint",
    mark: "↯",
  },
  {
    id: "timer",
    title: "5초 맞추기",
    label: "시간감각 대결",
    description: "5초에 가장 가까운 기록을 만듭니다.",
    duration: "1분",
    type: "집중력",
    color: "butter",
    mark: "5.00",
  },
  {
    id: "beans",
    title: "지뢰 피하기",
    label: "지뢰 피하기",
    description: "차례대로 하나씩 선택해 지뢰를 피합니다.",
    duration: "30초",
    type: "운",
    color: "peach",
    mark: "∴",
  },
  {
    id: "draw",
    title: "한 명 뽑기",
    label: "빠른 한 명 뽑기",
    description: "참여자 중 한 명을 같은 확률로 추첨합니다.",
    duration: "5초",
    type: "운",
    color: "lilac",
    mark: "?",
  },
] as const;
// Rejection sampling avoids modulo bias; choose each result once per round.
export const randomInt = (limit: number): number => {
  if (!Number.isInteger(limit) || limit < 1 || limit > 0x100000000)
    throw new Error("Invalid random range");
  const boundary = 0x100000000 - (0x100000000 % limit);
  const buffer = new Uint32Array(1);
  do {
    crypto.getRandomValues(buffer);
  } while (buffer[0] >= boundary);
  return buffer[0] % limit;
};
export const resolveScores = (scores: Score[]): GameResult => {
  const worst = Math.max(...scores.map((score) => score.value));
  const tied = scores.filter((score) => score.value === worst);
  return {
    loser: tied[randomInt(tied.length)].name,
    scores: [...scores].sort((a, b) => a.value - b.value),
    note:
      tied.length > 1
        ? "최하위 동점자 중 무작위 추첨한 결과입니다."
        : undefined,
  };
};

export const diceContenders = (
  scores: Score[],
  rule: "high" | "low",
): Score[] => {
  const target =
    rule === "high"
      ? Math.max(...scores.map((s) => s.value))
      : Math.min(...scores.map((s) => s.value));
  return scores.filter((s) => s.value === target);
};
