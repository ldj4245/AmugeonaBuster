import { useEffect, useMemo, useRef, useState } from "react";
import { CircleDot, Play } from "lucide-react";
import { GameResult, randomInt } from "./gameLogic";

interface Point {
  x: number;
  y: number;
}

const BOARD_WIDTH = 360;
const BOARD_HEIGHT = 500;
const BALL_START: Point = { x: BOARD_WIDTH / 2, y: 34 };
const LANE_LEFT = 18;
const LANE_WIDTH = BOARD_WIDTH - LANE_LEFT * 2;
const DROP_DURATION = 3200;

const clamp = (value: number, min: number, max: number) =>
  Math.min(max, Math.max(min, value));

const createRoute = (targetX: number): Point[] => {
  const route: Point[] = [BALL_START];
  for (let row = 0; row < 7; row++) {
    const progress = (row + 1) / 8;
    const center = BALL_START.x + (targetX - BALL_START.x) * progress;
    const remainingSpread = 58 * (1 - progress) + 13;
    const jitter = randomInt(Math.round(remainingSpread * 2) + 1) - remainingSpread;
    route.push({
      x: clamp(center + jitter, 34, BOARD_WIDTH - 34),
      y: 92 + row * 43,
    });
  }
  route.push({ x: targetX, y: 428 });
  return route;
};

export const PinballGame = ({
  players,
  onFinish,
}: {
  players: string[];
  onFinish: (result: GameResult) => void;
}): React.JSX.Element => {
  const [phase, setPhase] = useState<"ready" | "dropping" | "landed">(
    "ready",
  );
  const [ball, setBall] = useState<Point>(BALL_START);
  const [chosen, setChosen] = useState<number | null>(null);
  const frame = useRef<number>();
  const finishTimer = useRef<ReturnType<typeof setTimeout>>();
  const pins = useMemo(
    () =>
      Array.from({ length: 7 }, (_, row) => {
        const count = row % 2 === 0 ? 6 : 7;
        const gap = 42;
        const start = (BOARD_WIDTH - (count - 1) * gap) / 2;
        return Array.from({ length: count }, (_, column) => ({
          x: start + column * gap,
          y: 92 + row * 43,
        }));
      }).flat(),
    [],
  );

  useEffect(
    () => () => {
      cancelAnimationFrame(frame.current ?? 0);
      clearTimeout(finishTimer.current);
    },
    [],
  );

  const drop = () => {
    if (phase !== "ready") return;
    const selected = randomInt(players.length);
    const laneSize = LANE_WIDTH / players.length;
    const targetX = LANE_LEFT + laneSize * (selected + 0.5);
    const route = createRoute(targetX);
    const startedAt = performance.now();
    const duration = window.matchMedia("(prefers-reduced-motion: reduce)")
      .matches
      ? 700
      : DROP_DURATION;
    setChosen(selected);
    setPhase("dropping");

    const animate = (now: number) => {
      const progress = Math.min(1, (now - startedAt) / duration);
      const routePosition = progress * (route.length - 1);
      const segment = Math.min(route.length - 2, Math.floor(routePosition));
      const segmentProgress = routePosition - segment;
      const eased = segmentProgress * segmentProgress;
      const from = route[segment];
      const to = route[segment + 1];
      setBall({
        x: from.x + (to.x - from.x) * eased,
        y: from.y + (to.y - from.y) * eased,
      });
      if (progress < 1) {
        frame.current = requestAnimationFrame(animate);
        return;
      }
      setPhase("landed");
      navigator.vibrate?.([25, 35, 25]);
      finishTimer.current = setTimeout(
        () =>
          onFinish({
            loser: players[selected],
            note: "각 참여자 칸에 도착할 확률은 같습니다.",
          }),
        850,
      );
    };
    frame.current = requestAnimationFrame(animate);
  };

  return (
    <div className="pinball-game">
      <div className="turn-strip">
        <span>핀볼 한 판</span>
        <strong>
          {phase === "ready"
            ? "도착한 칸이 커피 담당"
            : phase === "dropping"
              ? "공이 내려가는 중"
              : `${players[chosen ?? 0]} 칸에 도착`}
        </strong>
      </div>
      <div className={`pinball-board ${phase}`} aria-live="polite">
        <svg
          viewBox={`0 0 ${BOARD_WIDTH} ${BOARD_HEIGHT}`}
          role="img"
          aria-label="참여자 칸으로 공이 떨어지는 핀볼 보드"
        >
          <rect className="pinball-board-bg" x="1" y="1" width="358" height="498" rx="18" />
          <path className="pinball-funnel" d="M24 18 L24 55 L150 76 M336 18 L336 55 L210 76" />
          {pins.map((pin, index) => (
            <g key={index} className="pinball-peg">
              <circle cx={pin.x} cy={pin.y} r="6" />
              <circle cx={pin.x - 1.5} cy={pin.y - 1.5} r="1.5" />
            </g>
          ))}
          {players.map((name, index) => {
            const width = LANE_WIDTH / players.length;
            const x = LANE_LEFT + width * index;
            const active = phase === "landed" && chosen === index;
            const maxCharacters =
              players.length <= 4 ? 6 : players.length <= 6 ? 3 : 2;
            const laneLabel = name.length <= maxCharacters ? name : `${index + 1}번`;
            return (
              <g key={name} className={`pinball-lane ${active ? "active" : ""}`}>
                <rect x={x + 2} y="405" width={width - 4} height="70" rx="5" />
                <text x={x + width / 2} y="458" textAnchor="middle">
                  {laneLabel}
                </text>
              </g>
            );
          })}
          <g className="pinball-ball" transform={`translate(${ball.x} ${ball.y})`}>
            <circle r="11" />
            <circle className="pinball-ball-shine" cx="-3" cy="-3" r="3" />
          </g>
        </svg>
      </div>
      <button
        className="button primary wide pinball-drop"
        disabled={phase !== "ready"}
        onClick={drop}
      >
        {phase === "ready" ? (
          <>
            <Play size={17} fill="currentColor" />
            공 떨어뜨리기
          </>
        ) : (
          <>
            <CircleDot size={17} />
            {phase === "dropping" ? "내려가는 중" : "결과 확인 중"}
          </>
        )}
      </button>
      <p className="game-footnote">
        결과는 공을 떨어뜨리는 순간 공정하게 정해집니다.
      </p>
    </div>
  );
};
