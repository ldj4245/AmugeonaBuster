import { useEffect, useState } from "react";
import { Star, X } from "lucide-react";
import { api, errorText, post, readLocal, saveLocal } from "../../lib/api";
import { Course } from "./MealPage";
interface Review {
  id: number;
  nickname: string;
  comment: string;
  rating: number;
  courseName: string;
}
export const ReviewPanel = ({
  course,
  cafeteriaName,
  menuDate,
  onClose,
}: {
  course: Course;
  cafeteriaName: string;
  menuDate: string;
  onClose: () => void;
}): React.JSX.Element => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [nickname, setNickname] = useState("");
  const [comment, setComment] = useState("");
  const [rating, setRating] = useState(5);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const date = menuDate;
  useEffect(() => {
    const controller = new AbortController();
    api<Review[]>(
      `/welstory/reviews?${new URLSearchParams({ cafeteriaName, menuDate: date })}`,
      { signal: controller.signal },
    )
      .then((data) =>
        setReviews(data.filter((r) => r.courseName === course.courseName)),
      )
      .catch((e) => {
        if (!controller.signal.aborted) setMessage(errorText(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [cafeteriaName, course.courseName, date]);
  return (
    <section className="review-panel">
      <div className="section-line">
        <h3>{course.courseName} · 한줄평</h3>
        <button
          className="icon-button"
          aria-label="한줄평 닫기"
          onClick={onClose}
        >
          <X size={18} />
        </button>
      </div>
      {loading ? (
        <p>한줄평을 불러오는 중입니다.</p>
      ) : reviews.length ? (
        <ul className="reviews">
          {reviews.map((r) => (
            <li key={r.id}>
              <span>
                <strong>{r.nickname}</strong>
                <span className="rating">★ {r.rating}</span>
              </span>
              <p>{r.comment}</p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">등록된 한줄평이 없습니다.</p>
      )}
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setSaving(true);
          setMessage("");
          try {
            let fingerprint = readLocal<string>("club-review-id", "");
            if (!fingerprint) {
              fingerprint = crypto.randomUUID();
              saveLocal("club-review-id", fingerprint);
            }
            const saved = await post<Review>("/welstory/reviews", {
              cafeteriaName,
              courseName: course.courseName,
              menuDate: date,
              menuDetails: course.menuDetails,
              nickname: nickname.trim(),
              comment: comment.trim(),
              rating,
              userFingerprint: fingerprint,
            });
            setReviews([...reviews, saved]);
            setComment("");
            setMessage("한줄평을 등록했습니다.");
          } catch (err) {
            setMessage(errorText(err));
          } finally {
            setSaving(false);
          }
        }}
      >
        <label className="field-label">
          별점
          <span className="star-input">
            {[1, 2, 3, 4, 5].map((n) => (
              <button
                type="button"
                key={n}
                aria-label={`${n}점`}
                aria-pressed={rating === n}
                onClick={() => setRating(n)}
              >
                <Star size={23} fill={n <= rating ? "currentColor" : "none"} />
              </button>
            ))}
          </span>
        </label>
        <label className="field-label">
          이름
          <input
            required
            maxLength={20}
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
          />
        </label>
        <label className="field-label">
          한줄평
          <textarea
            required
            maxLength={500}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="식사 후기를 입력해 주세요."
          />
        </label>
        <button
          className="button primary"
          disabled={saving || !comment.trim() || !nickname.trim()}
        >
          {saving ? "등록 중…" : "한줄평 남기기"}
        </button>
      </form>
      {message && (
        <p className="notice" role="status">
          {message}
        </p>
      )}
    </section>
  );
};
