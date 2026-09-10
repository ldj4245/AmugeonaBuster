import { useEffect, useRef, useState } from "react";
import { ArrowRight, Dices } from "lucide-react";
import { diceContenders, GameResult, randomInt, Score } from "./gameLogic";

const pips: Record<number, number[]> = {
  1: [4],
  2: [0, 8],
  3: [0, 4, 8],
  4: [0, 2, 6, 8],
  5: [0, 2, 4, 6, 8],
  6: [0, 2, 3, 5, 6, 8],
};
export const DiceGame = ({
  players,
  rule,
  onFinish,
}: {
  players: string[];
  rule: "high" | "low";
  onFinish: (result: GameResult) => void;
}): React.JSX.Element => {
  const [contenders, setContenders] = useState(players);
  const [turn, setTurn] = useState(0);
  const [round, setRound] = useState(1);
  const [dice, setDice] = useState([1, 1]);
  const [phase, setPhase] = useState<"ready" | "rolling" | "rolled" | "tie">(
    "ready",
  );
  const [scores, setScores] = useState<Score[]>([]);
  const [history, setHistory] = useState<Score[]>([]);
  const lock = useRef(false);
  const timeout = useRef<ReturnType<typeof setTimeout>>();
  useEffect(() => () => clearTimeout(timeout.current), []);
  const roll = () => {
    if (lock.current) return;
    lock.current = true;
    const result = [randomInt(6) + 1, randomInt(6) + 1];
    setPhase("rolling");
    let frames = 0;
    const animate = () => {
      if (++frames < 10) {
        setDice([randomInt(6) + 1, randomInt(6) + 1]);
        timeout.current = setTimeout(animate, 75);
      } else {
        setDice(result);
        setScores((previous) => [
          ...previous,
          {
            name: contenders[turn],
            value: result[0] + result[1],
            label: `${result[0]} + ${result[1]} = ${result[0] + result[1]}`,
          },
        ]);
        setPhase("rolled");
      }
    };
    animate();
  };
  const next = () => {
    if (turn + 1 < contenders.length) {
      setTurn(turn + 1);
      setPhase("ready");
      lock.current = false;
      return;
    }
    const tied = diceContenders(scores, rule);
    if (tied.length === 1) {
      onFinish({
        loser: tied[0].name,
        scores: [
          ...history,
          ...scores.map((s) => ({ ...s, name: `${s.name} · ${round}회차` })),
        ],
        note:
          rule === "high"
            ? "합계가 가장 높은 사람이 사는 규칙입니다."
            : "합계가 가장 낮은 사람이 사는 규칙입니다.",
      });
    } else {
      setContenders(tied.map((s) => s.name));
      setHistory([
        ...history,
        ...scores.map((s) => ({ ...s, name: `${s.name} · ${round}회차` })),
      ]);
      setPhase("tie");
    }
  };
  return (
    <div className="dice-game">
      <div className="turn-strip">
        <span>
          {round}회차 · {rule === "high" ? "높은 합계" : "낮은 합계"}가 사기
        </span>
        <strong>
          {phase === "tie"
            ? "동점 재경기"
            : `${contenders[turn]} · ${turn + 1}/${contenders.length}`}
        </strong>
      </div>
      {phase === "tie" ? (
        <div className="score-reveal">
          <h2>동점자가 있습니다</h2>
          <p>{contenders.join(", ")}</p>
          <button
            className="button primary"
            onClick={() => {
              setScores([]);
              setTurn(0);
              setRound(round + 1);
              setPhase("ready");
              lock.current = false;
            }}
          >
            동점자만 다시 굴리기
            <ArrowRight size={16} />
          </button>
        </div>
      ) : (
        <>
          <div
            className={`dice-pair ${phase === "rolling" ? "rolling" : ""}`}
            aria-label={`주사위 ${dice[0]}, ${dice[1]}`}
          >
            {dice.map((value, i) => (
              <div className="die" key={i}>
                {Array.from({ length: 9 }, (_, p) => (
                  <i key={p} className={pips[value].includes(p) ? "pip" : ""} />
                ))}
              </div>
            ))}
          </div>
          <div className="dice-total" aria-live="polite">
            {phase === "rolled" ? (
              <>
                <span>합계</span>
                <strong>{dice[0] + dice[1]}</strong>
              </>
            ) : (
              <span>
                {phase === "rolling" ? "굴리는 중" : "주사위 2개를 굴리세요"}
              </span>
            )}
          </div>
          {phase === "rolled" ? (
            <button className="button primary wide" onClick={next}>
              {turn + 1 === contenders.length ? "결과 확인" : "다음 참여자"}
              <ArrowRight size={16} />
            </button>
          ) : (
            <button
              className="button primary wide"
              disabled={phase === "rolling"}
              onClick={roll}
            >
              <Dices size={18} />
              주사위 굴리기
            </button>
          )}
        </>
      )}
      {!!scores.length && (
        <ol className="score-list">
          {scores.map((s) => (
            <li key={s.name}>
              <strong>{s.name}</strong>
              <span>{s.label}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
};
