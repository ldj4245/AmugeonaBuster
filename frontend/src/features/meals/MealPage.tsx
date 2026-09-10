import { useEffect, useState } from "react";
import {
  Bell,
  ChevronLeft,
  ChevronRight,
  Clock,
  Coffee,
  MapPin,
  MessageSquare,
  RefreshCw,
  Star,
  Utensils,
} from "lucide-react";
import {
  api,
  cafeterias,
  errorText,
  readLocal,
  saveLocal,
} from "../../lib/api";
import { ReviewPanel } from "./ReviewPanel";

export interface Course {
  courseName: string;
  menuDetails: string;
  calories: number;
  price?: string;
  imageUrl?: string;
}
interface Meal {
  cafeteriaName: string;
  dateStr: string;
  courses: Course[];
  status: "AVAILABLE" | "PARTIAL" | "EMPTY" | "UNAVAILABLE";
  message?: string;
}
type Stats = Record<string, { averageRating: number; reviewCount: number }>;
export const koreaDate = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(
    new Date(),
  );
const shiftDate = (value: string, days: number) => {
  const date = new Date(`${value}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
};

export const MealPage = ({
  onGames,
  onSettings,
}: {
  onGames: () => void;
  onSettings: () => void;
}): React.JSX.Element => {
  const [place, setPlace] = useState(() => {
    const shared = cafeterias.findIndex(
      (c) =>
        c.cotNo === new URLSearchParams(window.location.search).get("cotNo"),
    );
    const saved = readLocal<number>("club-cafeteria", 0);
    return shared >= 0
      ? shared
      : Number.isInteger(saved) && cafeterias[saved]
        ? saved
        : 0;
  });
  const [date, setDate] = useState(koreaDate);
  const [filter, setFilter] = useState("점심");
  const [meal, setMeal] = useState<Meal | null>(null);
  const [stats, setStats] = useState<Stats>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [review, setReview] = useState<Course | null>(null);
  const cafeteria = cafeterias[place];
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setMeal(null);
    setError("");
    setReview(null);
    setStats({});
    saveLocal("club-cafeteria", place);
    api<Meal>(
      `/welstory/menu-details?${new URLSearchParams({ cotNo: cafeteria.cotNo, hallNo: cafeteria.hallNo, date })}`,
      { signal: controller.signal },
    )
      .then(setMeal)
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorText(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    api<Stats>(
      `/welstory/reviews/stats?${new URLSearchParams({ cafeteriaName: cafeteria.name, menuDate: date })}`,
      { signal: controller.signal },
    )
      .then(setStats)
      .catch(() => undefined);
    return () => controller.abort();
  }, [place, cafeteria, date, reload]);
  const day = new Date(`${date}T12:00:00Z`);
  const weekStart = shiftDate(date, -((day.getUTCDay() + 6) % 7));
  const dates = Array.from({ length: 7 }, (_, i) => shiftDate(weekStart, i));
  const today = koreaDate();
  const courses = (meal?.courses || []).filter(
    (c) => filter === "전체" || c.courseName.includes(`(${filter})`),
  );
  return (
    <section>
      <div className="page-title">
        <div>
          <h1>오늘의 식단</h1>
          <p>{cafeteria.name}</p>
        </div>
        <label className="location">
          <MapPin size={16} />
          <select
            aria-label="구내식당"
            value={place}
            onChange={(e) => setPlace(Number(e.target.value))}
          >
            {cafeterias.map((c, i) => (
              <option value={i} key={c.cotNo}>
                {c.short}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className="workspace">
        <div className="main-column">
          <div className="date-bar">
            <div className="month">
              {day.getUTCFullYear()}년 {day.getUTCMonth() + 1}월
              <button
                aria-label="이전 주"
                disabled={weekStart <= shiftDate(today, -28)}
                onClick={() => setDate(shiftDate(date, -7))}
              >
                <ChevronLeft size={18} />
              </button>
              <button
                aria-label="다음 주"
                disabled={weekStart >= shiftDate(today, 21)}
                onClick={() => setDate(shiftDate(date, 7))}
              >
                <ChevronRight size={18} />
              </button>
            </div>
            <button className="today-button" onClick={() => setDate(today)}>
              오늘
            </button>
          </div>
          <div className="week">
            {dates.map((d, i) => (
              <button
                key={d}
                className={d === date ? "active" : ""}
                aria-pressed={d === date}
                onClick={() => setDate(d)}
              >
                <span className="weekday">
                  {["월", "화", "수", "목", "금", "토", "일"][i]}
                  {d === today ? " · 오늘" : ""}
                </span>
                <strong>{Number(d.slice(-2))}</strong>
              </button>
            ))}
          </div>
          <div className="menu-meta">
            <div className="meal-nav">
              {["아침", "점심", "저녁", "야식", "전체"].map((type) => (
                <button
                  key={type}
                  aria-pressed={filter === type}
                  className={filter === type ? "active" : ""}
                  onClick={() => setFilter(type)}
                >
                  {type}
                </button>
              ))}
            </div>
            <button
              className="icon-btn"
              disabled={loading}
              aria-label="식단 새로고침"
              onClick={() => setReload(reload + 1)}
            >
              <RefreshCw size={16} className={loading ? "spin" : ""} />
            </button>
          </div>
          {meal?.status === "PARTIAL" && (
            <p className="notice" role="status">
              일부 식단을 불러오지 못했습니다. 새로고침 후 다시 확인해 주세요.
            </p>
          )}
          {loading ? (
            <div
              className="food-grid"
              aria-busy="true"
              aria-label="식단 불러오는 중"
            >
              {[1, 2].map((i) => (
                <div className="meal-skeleton" key={i}>
                  <div />
                  <span />
                  <span />
                </div>
              ))}
            </div>
          ) : error || meal?.status === "UNAVAILABLE" ? (
            <div className="empty-note">
              <Utensils size={32} />
              <h2>식단을 불러올 수 없습니다</h2>
              <p>{error || meal?.message || "잠시 후 다시 시도해 주세요."}</p>
              <button
                className="button secondary"
                onClick={() => setReload(reload + 1)}
              >
                다시 불러오기
              </button>
            </div>
          ) : !courses.length ? (
            <div className="empty-note">
              <Utensils size={32} />
              <h2>등록된 식단이 없습니다</h2>
              <p>
                {date} · {filter}
              </p>
              {filter !== "전체" && (
                <button
                  className="button secondary"
                  onClick={() => setFilter("전체")}
                >
                  전체 식단 보기
                </button>
              )}
            </div>
          ) : (
            <div className="food-grid">
              {courses.map((c, i) => (
                <article className="food-card" key={`${c.courseName}-${i}`}>
                  <div className="photo">
                    <div className="actual-photo-fallback">
                      <Utensils size={34} strokeWidth={1} />
                      <span>식단 사진 없음</span>
                    </div>
                    {c.imageUrl && (
                      <img
                        src={c.imageUrl}
                        loading="lazy"
                        alt={c.courseName}
                        onError={(e) => {
                          e.currentTarget.hidden = true;
                        }}
                      />
                    )}
                  </div>
                  <div className="food-body">
                    <div className="category-line">
                      <span>{c.courseName}</span>
                      {stats[c.courseName]?.reviewCount > 0 && (
                        <span className="rating">
                          <Star size={11} />
                          {stats[c.courseName].averageRating}
                        </span>
                      )}
                    </div>
                    <h2>{c.menuDetails.split(/[,\n]/)[0]}</h2>
                    <p className="side-dishes">{c.menuDetails}</p>
                    <div className="nutrition">
                      {c.calories > 0 && (
                        <span>{c.calories.toLocaleString()} kcal</span>
                      )}
                      {c.price && <strong>{c.price}</strong>}
                    </div>
                    <button className="review" onClick={() => setReview(c)}>
                      <span>
                        <MessageSquare size={13} />
                        한줄평 {stats[c.courseName]?.reviewCount || 0}
                      </span>
                      <ChevronRight size={15} />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
          <p className="food-notice">
            <Clock size={13} />
            메뉴는 식당 사정에 따라 변경될 수 있습니다.
          </p>
          {review && (
            <ReviewPanel
              key={`${date}-${review.courseName}`}
              course={review}
              cafeteriaName={cafeteria.name}
              menuDate={date}
              onClose={() => setReview(null)}
            />
          )}
        </div>
        <aside className="right-column">
          <div className="coffee-card">
            <span className="eyebrow">식후 커피</span>
            <h2>커피내기</h2>
            <p>2~8명 · 한 기기로 진행</p>
            <div className="cup-sketch">
              <Coffee size={82} strokeWidth={1} />
            </div>
            <button onClick={onGames}>
              게임 선택
              <ChevronRight size={16} />
            </button>
          </div>
          <section className="aside-section">
            <div className="aside-heading">
              <h3>게임</h3>
              <span>전체 5</span>
            </div>
            <div className="shortcuts">
              {[
                "주사위 2개",
                "5초 맞추기",
                "반응속도 대결",
                "지뢰 피하기",
                "한 명 뽑기",
              ].map((name) => (
                <button className="game-shortcut" key={name} onClick={onGames}>
                  <span>
                    <strong>{name}</strong>
                  </span>
                  <ChevronRight className="arrow" size={14} />
                </button>
              ))}
            </div>
          </section>
          <button className="alarm" onClick={onSettings}>
            <Bell size={18} />
            <span>
              <strong>식단 알림</strong>
              <p>카카오톡 알림 설정</p>
            </span>
            <ChevronRight size={14} />
          </button>
        </aside>
      </div>
    </section>
  );
};
