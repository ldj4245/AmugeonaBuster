import { useState } from "react";
import {
  ArrowLeft,
  ChevronRight,
  Coffee,
  Dices,
  Plus,
  RotateCcw,
  Share2,
  X,
} from "lucide-react";
import { readLocal, saveLocal } from "../../lib/api";
import { games, GameId, GameResult } from "./gameLogic";
import { TimedGame } from "./TimedGame";
import { ChanceGame } from "./ChanceGame";
import { DiceGame } from "./DiceGame";

interface History {
  game: string;
  loser: string;
  date: string;
}
export const GamesPage = (): React.JSX.Element => {
  const [selected, setSelected] = useState<GameId>("dice");
  const [stage, setStage] = useState<"select" | "setup" | "play">("select");
  const [players, setPlayers] = useState<string[]>(() => {
    const saved = readLocal<unknown>("club-players", []);
    return Array.isArray(saved) &&
      saved.length >= 2 &&
      saved.length <= 8 &&
      saved.every((n) => typeof n === "string")
      ? saved
      : ["참여자 1", "참여자 2", "참여자 3", "참여자 4"];
  });
  const [rule, setRule] = useState<"high" | "low">("high");
  const [result, setResult] = useState<GameResult | null>(null);
  const [round, setRound] = useState(0);
  const [message, setMessage] = useState("");
  const [history, setHistory] = useState<History[]>(() => {
    const saved = readLocal<unknown>("club-history", []);
    return Array.isArray(saved)
      ? saved
          .filter(
            (v): v is History =>
              v &&
              typeof v.game === "string" &&
              typeof v.loser === "string" &&
              typeof v.date === "string",
          )
          .slice(0, 5)
      : [];
  });
  const game = games.find((g) => g.id === selected)!;
  const finish = (outcome: GameResult) => {
    setResult(outcome);
    const recent = [
      {
        game: game.title,
        loser: outcome.loser,
        date: new Date().toLocaleDateString("ko-KR"),
      },
      ...history,
    ].slice(0, 5);
    setHistory(recent);
    saveLocal("club-history", recent);
  };
  const start = () => {
    const names = players.map((n) => n.trim());
    if (names.some((n) => !n) || new Set(names).size !== names.length) {
      setMessage("참여자 이름을 빈칸 없이, 서로 다르게 입력해 주세요.");
      return;
    }
    setPlayers(names);
    saveLocal("club-players", names);
    setStage("play");
    setResult(null);
    setRound(round + 1);
    setMessage("");
  };
  const choose = (id: GameId) => {
    setSelected(id);
    setStage("select");
    setResult(null);
    setMessage("");
  };
  const losingRule =
    selected === "dice"
      ? `${rule === "high" ? "높은" : "낮은"} 합계가 사기 · 동점자는 재경기`
      : selected === "timer"
        ? "5초와의 차이가 가장 큰 사람"
        : selected === "reaction"
          ? "반응 시간이 가장 긴 사람"
          : selected === "beans"
            ? "지뢰를 선택한 사람"
            : "추첨된 사람";
  return (
    <section>
      <div className="page-title">
        <div>
          <h1>커피내기</h1>
          <p>{stage === "select" ? "게임 선택" : game.title}</p>
        </div>
        <span className="game-meta">2~8명 · 한 기기</span>
      </div>
      <div className="game-toolbar">
        <div className="participants">
          <span className="person-stack">
            {players.slice(0, 4).map((name, i) => (
              <span key={i}>{name.slice(0, 1)}</span>
            ))}
          </span>
          <strong>참여자 {players.length}명</strong>
        </div>
        {stage !== "play" && (
          <button
            className="edit-participants"
            onClick={() => {
              setStage("setup");
              setMessage("");
            }}
          >
            참여자 편집
            <ChevronRight size={15} />
          </button>
        )}
      </div>
      <div className="games-content">
        <div>
          {stage === "select" ? (
            <div className="game-options">
              {games.map((item) => (
                <button
                  key={item.id}
                  className={`game-option ${selected === item.id ? "selected" : ""}`}
                  aria-pressed={selected === item.id}
                  onClick={() => choose(item.id)}
                >
                  <div className="tags">
                    <span>{item.type}</span>
                    <span>약 {item.duration}</span>
                  </div>
                  <div className="game-graphic">
                    {item.id === "dice" ? (
                      <Dices
                        className="dice-card-art"
                        size={78}
                        strokeWidth={1.2}
                      />
                    ) : item.id === "timer" ? (
                      <span className="number-art">
                        5<small>.00</small>
                      </span>
                    ) : item.id === "reaction" ? (
                      <span className="reflex-art">
                        <span />
                        <span />
                        <span />
                      </span>
                    ) : item.id === "beans" ? (
                      <span className="dots-art">
                        {Array.from({ length: 9 }, (_, i) => (
                          <i key={i} />
                        ))}
                      </span>
                    ) : (
                      <span className="random-art">
                        <span>1</span>
                        <span>2</span>
                        <span>?</span>
                      </span>
                    )}
                  </div>
                  <h2>{item.title}</h2>
                  <p>{item.description}</p>
                  <div className="option-bottom">
                    <span>{selected === item.id ? "선택됨" : "선택"}</span>
                    <ChevronRight size={15} />
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <>
              <button
                className="plain-back"
                onClick={() => {
                  setStage("select");
                  setResult(null);
                  setMessage("");
                }}
              >
                <ArrowLeft size={14} />
                게임 선택
              </button>
              <div className="play-panel">
                {stage === "setup" ? (
                  <div className="player-setup">
                    <h2>참여자 설정</h2>
                    <div className="player-inputs">
                      {players.map((name, i) => (
                        <div key={i}>
                          <span className="player-index">{i + 1}</span>
                          <input
                            aria-label={`${i + 1}번 참여자 이름`}
                            maxLength={12}
                            value={name}
                            onChange={(e) =>
                              setPlayers(
                                players.map((n, j) =>
                                  i === j ? e.target.value : n,
                                ),
                              )
                            }
                          />
                          <button
                            className="icon-button"
                            aria-label={`${i + 1}번 참여자 삭제`}
                            disabled={players.length <= 2}
                            onClick={() =>
                              setPlayers(players.filter((_, j) => i !== j))
                            }
                          >
                            <X size={16} />
                          </button>
                        </div>
                      ))}
                    </div>
                    <button
                      className="text-button"
                      disabled={players.length >= 8}
                      onClick={() => {
                        let n = players.length + 1;
                        while (players.includes(`참여자 ${n}`)) n++;
                        setPlayers([...players, `참여자 ${n}`]);
                      }}
                    >
                      <Plus size={15} />
                      참여자 추가
                    </button>
                    <button className="button primary wide" onClick={start}>
                      {players.length}명으로 시작
                    </button>
                  </div>
                ) : result ? (
                  <div className="result-ticket">
                    <Coffee size={38} strokeWidth={1.3} />
                    <p>커피 담당</p>
                    <h2>
                      {result.loser}
                      <small>님</small>
                    </h2>
                    {result.scores && (
                      <ol className="score-list">
                        {result.scores.map((s, i) => (
                          <li key={`${s.name}-${i}`}>
                            <strong>{s.name}</strong>
                            <span>{s.label}</span>
                          </li>
                        ))}
                      </ol>
                    )}
                    {result.note && <p className="muted">{result.note}</p>}
                    <div className="button-row">
                      <button className="button primary" onClick={start}>
                        <RotateCcw size={16} />
                        다시 하기
                      </button>
                      <button
                        className="button secondary"
                        onClick={async () => {
                          try {
                            await navigator.clipboard.writeText(
                              `[아무거나] ${game.title}\n커피 담당: ${result.loser}`,
                            );
                            setMessage("결과를 복사했습니다.");
                          } catch {
                            setMessage(
                              "복사할 수 없습니다. 화면의 결과를 확인해 주세요.",
                            );
                          }
                        }}
                      >
                        <Share2 size={16} />
                        결과 복사
                      </button>
                    </div>
                    <button
                      className="text-button"
                      onClick={() => {
                        setStage("setup");
                        setResult(null);
                      }}
                    >
                      참여자 변경
                    </button>
                  </div>
                ) : selected === "dice" ? (
                  <DiceGame
                    key={round}
                    players={players}
                    rule={rule}
                    onFinish={finish}
                  />
                ) : selected === "timer" || selected === "reaction" ? (
                  <TimedGame
                    key={round}
                    players={players}
                    mode={selected}
                    onFinish={finish}
                  />
                ) : (
                  <ChanceGame
                    key={round}
                    players={players}
                    mode={selected}
                    onFinish={finish}
                  />
                )}
              </div>
            </>
          )}
          {message && (
            <p className="notice" role="status">
              {message}
            </p>
          )}
        </div>
        <aside className="game-detail">
          <span className="detail-caption">선택한 게임</span>
          <h2>{game.title}</h2>
          <p>{game.description}</p>
          {selected === "dice" && (
            <fieldset className="dice-rule" disabled={stage === "play"}>
              <legend>커피 담당 기준</legend>
              <label>
                <input
                  type="radio"
                  name="dice-rule"
                  checked={rule === "high"}
                  onChange={() => setRule("high")}
                />
                합계가 가장 높은 사람
              </label>
              <label>
                <input
                  type="radio"
                  name="dice-rule"
                  checked={rule === "low"}
                  onChange={() => setRule("low")}
                />
                합계가 가장 낮은 사람
              </label>
            </fieldset>
          )}
          <div className="detail-line">
            <span>커피 담당</span>
            <strong>{losingRule}</strong>
          </div>
          <div className="detail-line">
            <span>진행 방식</span>
            <strong>
              {selected === "draw" ? "한 번에 추첨" : "한 명씩 순서대로"}
            </strong>
          </div>
          {(selected === "timer" || selected === "reaction") && (
            <p className="game-footnote">
              최하위 동점자는 무작위로 추첨합니다.
            </p>
          )}
          {stage === "select" && (
            <button className="detail-start" onClick={() => setStage("setup")}>
              게임 시작
              <ChevronRight size={15} />
            </button>
          )}
        </aside>
      </div>
      {stage === "select" && history.length > 0 && (
        <section className="history-section">
          <div className="aside-heading">
            <h3>최근 결과</h3>
            <span>이 기기에 저장</span>
          </div>
          <ul className="history-list">
            {history.map((h, i) => (
              <li key={i}>
                <Coffee size={17} />
                <strong>{h.loser}</strong>
                <span>{h.game}</span>
                <time>{h.date}</time>
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
};
