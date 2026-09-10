import { useEffect, useRef, useState } from "react";
import { ArrowRight, Hand, Timer, Zap } from "lucide-react";
import { GameResult, randomInt, resolveScores, Score } from "./gameLogic";

interface Props {
  players: string[];
  mode: "reaction" | "timer";
  onFinish: (result: GameResult) => void;
}
export const TimedGame = ({
  players,
  mode,
  onFinish,
}: Props): React.JSX.Element => {
  const [turn, setTurn] = useState(0);
  const [phase, setPhase] = useState<"ready" | "waiting" | "go" | "scored">(
    "ready",
  );
  const [scores, setScores] = useState<Score[]>([]);
  const [notice, setNotice] = useState("");
  const phaseRef = useRef(phase);
  const clock = useRef(0);
  const timeout = useRef<ReturnType<typeof setTimeout>>();
  const changePhase = (value: typeof phase) => {
    phaseRef.current = value;
    setPhase(value);
  };
  useEffect(() => {
    const cancel = () => {
      if (document.hidden && ["waiting", "go"].includes(phaseRef.current)) {
        clearTimeout(timeout.current);
        changePhase("ready");
        setNotice("화면 전환으로 측정을 취소했습니다. 다시 시작해 주세요.");
      }
    };
    document.addEventListener("visibilitychange", cancel);
    return () => {
      clearTimeout(timeout.current);
      document.removeEventListener("visibilitychange", cancel);
    };
  }, []);
  const record = (value: number, label: string) => {
    clearTimeout(timeout.current);
    setScores((previous) => [
      ...previous,
      { name: players[turn], value, label },
    ]);
    changePhase("scored");
  };
  const tap = () => {
    if (phaseRef.current === "ready") {
      setNotice("");
      if (mode === "timer") {
        clock.current = performance.now();
        changePhase("go");
        timeout.current = setTimeout(() => record(15000, "시간 초과"), 20000);
      } else {
        changePhase("waiting");
        timeout.current = setTimeout(
          () => {
            clock.current = performance.now();
            changePhase("go");
            timeout.current = setTimeout(
              () => record(10000, "시간 초과"),
              10000,
            );
          },
          1500 + randomInt(3000),
        );
      }
    } else if (phaseRef.current === "waiting") {
      record(10000, "부정 출발");
    } else if (phaseRef.current === "go") {
      const elapsed = Math.round(performance.now() - clock.current);
      record(
        mode === "timer" ? Math.abs(elapsed - 5000) : elapsed,
        mode === "timer" ? `${(elapsed / 1000).toFixed(3)}초` : `${elapsed} ms`,
      );
    }
  };
  const next = () => {
    if (turn + 1 === players.length) onFinish(resolveScores(scores));
    else {
      setTurn(turn + 1);
      changePhase("ready");
    }
  };
  return (
    <div className="timed-game">
      <div className="turn-strip">
        <span>
          {turn + 1} / {players.length} 번째 도전
        </span>
        <strong>{players[turn]} 님 차례</strong>
      </div>
      <div className="round-progress">
        {players.map((name, index) => (
          <span key={name} className={index <= turn ? "filled" : ""} />
        ))}
      </div>
      {phase === "scored" ? (
        <div className="score-reveal" aria-live="polite">
          <span>이번 기록</span>
          <strong>{scores[scores.length - 1]?.label}</strong>
          {mode === "timer" && (
            <p>
              5초와의 차이{" "}
              {(scores[scores.length - 1]?.value / 1000).toFixed(3)}초
            </p>
          )}
          <button className="button primary" onClick={next}>
            {turn + 1 === players.length
              ? "최종 결과 보기"
              : "다음 사람에게 넘기기"}
            <ArrowRight size={18} />
          </button>
        </div>
      ) : (
        <button
          type="button"
          className={`tap-surface ${mode} ${phase}`}
          onClick={tap}
        >
          {mode === "timer" ? (
            <Timer size={42} strokeWidth={1.4} />
          ) : phase === "go" ? (
            <Zap size={44} />
          ) : (
            <Hand size={42} strokeWidth={1.4} />
          )}
          <strong>
            {phase === "ready"
              ? "시작"
              : mode === "timer"
                ? "5초에 맞춰 정지"
                : phase === "waiting"
                  ? "신호 대기"
                  : "누르세요"}
          </strong>
          <span>
            {phase === "ready"
              ? mode === "timer"
                ? "누르는 순간 시작 · 다시 누르면 정지"
                : "초록색으로 바뀌는 순간 다시 탭"
              : mode === "timer"
                ? "버튼을 다시 누르면 정지합니다."
                : phase === "waiting"
                  ? "신호 전에 누르면 부정 출발입니다."
                  : "TAP!"}
          </span>
        </button>
      )}
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      <p className="game-footnote">
        같은 기기에서 차례대로 진행합니다.{" "}
        {mode === "reaction"
          ? "부정 출발과 시간 초과는 10,000ms로 기록합니다."
          : "20초 후 자동 종료됩니다."}
      </p>
    </div>
  );
};
