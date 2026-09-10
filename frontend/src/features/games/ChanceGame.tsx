import { useEffect, useRef, useState } from "react";
import { Coffee, Check, ArrowRight } from "lucide-react";
import { GameResult, randomInt } from "./gameLogic";

export const ChanceGame = ({
  mode,
  players,
  onFinish,
}: {
  mode: "beans" | "draw";
  players: string[];
  onFinish: (result: GameResult) => void;
}): React.JSX.Element => {
  const [bomb] = useState(() => randomInt(16));
  const [opened, setOpened] = useState<number[]>([]);
  const [turn, setTurn] = useState(0);
  const [lost, setLost] = useState(false);
  const [spinning, setSpinning] = useState(false);
  const [highlight, setHighlight] = useState(0);
  const lock = useRef(false);
  const timeout = useRef<ReturnType<typeof setTimeout>>();
  useEffect(() => () => clearTimeout(timeout.current), []);
  const choose = (index: number) => {
    if (lock.current || opened.includes(index)) return;
    lock.current = true;
    setOpened((previous) => [...previous, index]);
    if (index === bomb) setLost(true);
    else
      timeout.current = setTimeout(() => {
        setTurn((previous) => (previous + 1) % players.length);
        lock.current = false;
      }, 450);
  };
  const draw = () => {
    if (lock.current) return;
    lock.current = true;
    setSpinning(true);
    const chosen = randomInt(players.length);
    let step = 0;
    const spin = () => {
      step++;
      setHighlight(step % players.length);
      if (step < 24) timeout.current = setTimeout(spin, 65 + step * 5);
      else {
        setHighlight(chosen);
        timeout.current = setTimeout(
          () => onFinish({ loser: players[chosen] }),
          600,
        );
      }
    };
    spin();
  };
  if (mode === "draw")
    return (
      <div className="draw-game">
        <span className="eyebrow">무작위 추첨</span>
        <h2>{spinning ? "추첨 중" : "한 명 뽑기"}</h2>
        <div className="draw-names">
          {players.map((name, index) => (
            <div
              key={name}
              className={spinning && highlight === index ? "selected" : ""}
            >
              <span>{String(index + 1).padStart(2, "0")}</span>
              {name}
              <Coffee size={20} />
            </div>
          ))}
        </div>
        <button
          className="button primary wide"
          disabled={spinning}
          onClick={draw}
        >
          {spinning ? "추첨 중" : "한 명 뽑기"}
          <ArrowRight size={18} />
        </button>
        <p className="game-footnote">모든 참여자의 확률은 동일합니다.</p>
      </div>
    );
  return (
    <div className="beans-game">
      <div className="turn-strip">
        <span>남은 칸 {16 - opened.length}개</span>
        <strong>{lost ? "지뢰 선택" : `${players[turn]} 님 차례`}</strong>
      </div>
      <p>16개 중 지뢰는 하나입니다. 순서대로 하나씩 선택하세요.</p>
      <div className="bean-grid">
        {Array.from({ length: 16 }, (_, index) => (
          <button
            key={index}
            className={`bean ${opened.includes(index) ? (index === bomb ? "bomb" : "safe") : ""}`}
            disabled={lost || opened.includes(index)}
            onClick={() => choose(index)}
            aria-label={`${index + 1}번 칸${opened.includes(index) ? (index === bomb ? ", 꽝" : ", 통과") : ""}`}
          >
            {opened.includes(index) ? (
              index === bomb ? (
                <Coffee size={28} />
              ) : (
                <Check size={24} />
              )
            ) : (
              <>
                <span className="bean-art" />
                <small>{String(index + 1).padStart(2, "0")}</small>
              </>
            )}
          </button>
        ))}
      </div>
      {lost ? (
        <button
          className="button primary wide"
          onClick={() => onFinish({ loser: players[turn] })}
        >
          결과 확인
          <ArrowRight size={18} />
        </button>
      ) : (
        <p className="game-footnote" aria-live="polite">
          {opened.length
            ? "통과. 다음 참여자 차례입니다."
            : "하나를 선택하세요."}
        </p>
      )}
    </div>
  );
};
