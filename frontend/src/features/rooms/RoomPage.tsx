import { useCallback, useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  Copy,
  Heart,
  MapPin,
  Users,
  X,
  ExternalLink,
  RefreshCw,
  LocateFixed,
  Search,
} from "lucide-react";
import { api, errorText, post, readLocal, saveLocal } from "../../lib/api";
import { useWebSocket, WebSocketRoomResponse } from "../../hooks/useWebSocket";
import { KakaoMap } from "../../components/KakaoMap";
const menus = [
  "삼겹살",
  "김치찌개",
  "치킨",
  "초밥",
  "돈카츠",
  "라멘",
  "짜장면",
  "짬뽕",
  "마라탕",
  "피자",
  "파스타",
  "스테이크",
  "떡볶이",
  "쌀국수",
  "팟타이",
];
type Room = WebSocketRoomResponse;
const formatDistance = (meters: number) =>
  meters < 1000 ? `${meters}m` : `${(meters / 1000).toFixed(1)}km`;
interface LocationCandidate {
  placeId: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}
export const RoomPage = (): React.JSX.Element => {
  const [room, setRoom] = useState<Room | null>(null);
  const [member, setMember] = useState("");
  const [nickname, setNickname] = useState("");
  const [location, setLocation] = useState(
    () => new URLSearchParams(window.location.search).get("location") || "",
  );
  const [locationAddress, setLocationAddress] = useState("");
  const [locationPlaceId, setLocationPlaceId] = useState("");
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [locationSuggestions, setLocationSuggestions] = useState<LocationCandidate[]>([]);
  const [locationConfirmed, setLocationConfirmed] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationMessage, setLocationMessage] = useState("");
  const [join, setJoin] = useState(() =>
    new URLSearchParams(window.location.search).has("room"),
  );
  const [code, setCode] = useState(
    () => new URLSearchParams(window.location.search).get("room") || "",
  );
  const [selected, setSelected] = useState(menus);
  const [custom, setCustom] = useState("");
  const [swiped, setSwiped] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const voteLock = useRef(false);
  const onMessage = useCallback((value: Room) => setRoom(value), []);
  useWebSocket(room?.roomId || null, onMessage);
  useEffect(() => {
    if (!code) return;
    const controller = new AbortController();
    api<Room>(`/rooms/${encodeURIComponent(code)}`, {
      signal: controller.signal,
    })
      .then((data) => {
        if (data.status === "COMPLETED") setRoom(data);
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, []);
  useEffect(() => {
    if (join || locationConfirmed || location.trim().length < 2) {
      setLocationSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      api<LocationCandidate[]>(`/locations/search?${new URLSearchParams({ query: location.trim() })}`, {
        signal: controller.signal,
      })
        .then(setLocationSuggestions)
        .catch(() => {
          if (!controller.signal.aborted) setLocationSuggestions([]);
        });
    }, 220);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [join, location, locationConfirmed]);
  const perform = async (fn: () => Promise<void>) => {
    setBusy(true);
    setMessage("");
    try {
      await fn();
    } catch (e) {
      setMessage(errorText(e));
    } finally {
      setBusy(false);
    }
  };
  const enter = async () => {
    const data = join
      ? await post<Room>(
          `/rooms/${encodeURIComponent(code.trim().toUpperCase().startsWith("ROOM-") ? code.trim().toUpperCase() : `ROOM-${code.trim().toUpperCase()}`)}/members`,
          { guestNickname: nickname.trim() },
        )
      : await post<Room>("/rooms", {
          hostNickname: nickname.trim(),
          location: location.trim(),
          locationAddress: locationAddress.trim() || undefined,
          locationPlaceId: locationPlaceId || undefined,
          latitude: latitude ?? undefined,
          longitude: longitude ?? undefined,
          customMenus: selected,
        });
    const mine = data.members[data.members.length - 1].id;
    setMember(mine);
    setRoom(data);
    setSwiped([]);
  };
  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setLocationMessage("이 브라우저에서는 현재 위치를 사용할 수 없습니다. 장소를 검색해 주세요.");
      return;
    }
    setLocationLoading(true);
    setLocationMessage("");
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        setLatitude(coords.latitude);
        setLongitude(coords.longitude);
        setLocationPlaceId("");
        setLocationConfirmed(true);
        setLocationSuggestions([]);
        try {
          const resolved = await api<LocationCandidate>(
            `/locations/reverse?${new URLSearchParams({
              latitude: String(coords.latitude),
              longitude: String(coords.longitude),
            })}`,
          );
          setLocation(resolved.name || "현재 위치");
          setLocationAddress(resolved.address || "");
        } catch {
          setLocation("현재 위치");
          setLocationAddress("기기에서 확인한 위치");
        } finally {
          setLocationLoading(false);
        }
      },
      (error) => {
        setLocationLoading(false);
        setLocationMessage(
          error.code === error.PERMISSION_DENIED
            ? "위치 권한이 꺼져 있습니다. 권한을 허용하거나 장소를 검색해 주세요."
            : "현재 위치를 확인하지 못했습니다. 장소를 검색해 주세요.",
        );
      },
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 },
    );
  };
  const selectLocation = (candidate: LocationCandidate) => {
    setLocation(candidate.name);
    setLocationAddress(candidate.address);
    setLocationPlaceId(candidate.placeId);
    setLatitude(candidate.latitude);
    setLongitude(candidate.longitude);
    setLocationConfirmed(true);
    setLocationSuggestions([]);
    setLocationMessage("");
  };
  const nextMenu = room?.defaultMenus.find((menu) => !swiped.includes(menu));
  const vote = async (like: boolean) => {
    if (!room || !nextMenu || voteLock.current) return;
    voteLock.current = true;
    await perform(async () => {
      const data = await post<Room>(`/rooms/${room.roomId}/swipes`, {
        memberId: member,
        menuName: nextMenu,
        isLike: like,
      });
      setRoom(data);
      setSwiped([...swiped, nextMenu]);
    });
    voteLock.current = false;
  };
  return (
    <section className="page-enter">
      <div className="section-heading">
        <div>
          <span className="eyebrow">LET’S PICK TOGETHER</span>
          <h1>메뉴 투표</h1>
          <p>후보 메뉴를 선택하고 참여자를 초대하세요.</p>
        </div>
        <Users size={42} strokeWidth={1} />
      </div>
      {message && (
        <p className="notice" role="status">
          {message}
        </p>
      )}
      {!room ? (
        <div className="room-setup settings-card">
          <div className="meal-tabs">
            <button
              className={!join ? "active" : ""}
              onClick={() => setJoin(false)}
            >
              방 만들기
            </button>
            <button
              className={join ? "active" : ""}
              onClick={() => setJoin(true)}
            >
              방 참여
            </button>
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              perform(enter);
            }}
          >
            <label className="field-label">
              내 이름
              <input
                required
                maxLength={20}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="동료들이 알아볼 이름"
              />
            </label>
            {join ? (
              <label className="field-label">
                초대 코드
                <input
                  required
                  value={code}
                  maxLength={20}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="ROOM-ABC123"
                />
              </label>
            ) : (
              <>
                <div className="field-label location-field" role="group" aria-labelledby="location-label">
                  <span id="location-label">약속 위치</span>
                  <div className="location-input-row">
                    <div className="location-input-wrap">
                      <Search size={16} aria-hidden="true" />
                      <input
                        required
                        maxLength={80}
                        value={location}
                        onChange={(e) => {
                          setLocation(e.target.value);
                          setLocationAddress("");
                          setLocationPlaceId("");
                          setLatitude(null);
                          setLongitude(null);
                          setLocationConfirmed(false);
                          setLocationMessage("");
                        }}
                        placeholder="역, 건물, 주소로 검색"
                        autoComplete="off"
                        aria-label="약속 위치 검색"
                        aria-autocomplete="list"
                      />
                    </div>
                    <button
                      type="button"
                      className="location-current-button"
                      onClick={useCurrentLocation}
                      disabled={locationLoading}
                    >
                      <LocateFixed size={15} />
                      {locationLoading ? "확인 중" : "현재 위치"}
                    </button>
                  </div>
                  {locationSuggestions.length > 0 && (
                    <div className="location-suggestions" role="listbox" aria-label="위치 검색 결과">
                      {locationSuggestions.map((candidate) => (
                        <button
                          type="button"
                          role="option"
                          key={`${candidate.placeId}-${candidate.latitude}`}
                          onClick={() => selectLocation(candidate)}
                        >
                          <strong>{candidate.name}</strong>
                          <span>{candidate.address || "주소 정보 없음"}</span>
                        </button>
                      ))}
                    </div>
                  )}
                  {locationConfirmed && locationAddress && (
                    <p className="location-confirmed">{locationAddress}</p>
                  )}
                  {locationMessage && <p className="location-message">{locationMessage}</p>}
                </div>
                <label className="field-label">
                  후보 메뉴 · {selected.length}개
                </label>
                <div className="menu-options">
                  {Array.from(new Set([...menus, ...selected])).map((menu) => (
                    <button
                      type="button"
                      key={menu}
                      aria-pressed={selected.includes(menu)}
                      className={selected.includes(menu) ? "selected" : ""}
                      onClick={() =>
                        setSelected(
                          selected.includes(menu)
                            ? selected.filter((m) => m !== menu)
                            : [...selected, menu],
                        )
                      }
                    >
                      {menu}
                    </button>
                  ))}
                </div>
                <div className="inline-input">
                  <input
                    aria-label="직접 추가할 메뉴"
                    maxLength={30}
                    value={custom}
                    onChange={(e) => setCustom(e.target.value)}
                    placeholder="먹고 싶은 메뉴 직접 추가"
                  />
                  <button
                    type="button"
                    className="button secondary"
                    disabled={
                      !custom.trim() ||
                      selected.includes(custom.trim()) ||
                      selected.length >= 30
                    }
                    onClick={() => {
                      setSelected([...selected, custom.trim()]);
                      setCustom("");
                    }}
                  >
                    추가
                  </button>
                </div>
              </>
            )}
            <button
              className="button primary wide"
              disabled={
                busy ||
                locationLoading ||
                !nickname.trim() ||
                (!join && (!location.trim() || !selected.length))
              }
            >
              {busy ? "준비 중…" : join ? "방 참여하기" : "함께 고를 방 만들기"}
              <ArrowRight size={17} />
            </button>
          </form>
        </div>
      ) : (
        <>
          <div className="room-header">
            <span>
              <MapPin size={17} />
              {room.location}
            </span>
            {room.locationAddress && <small>{room.locationAddress}</small>}
            <span>{room.members.length}명 참여 중</span>
            <button
              className="text-button"
              onClick={() => {
                setRoom(null);
                setMember("");
                setSwiped([]);
                window.history.replaceState(
                  {},
                  "",
                  `${window.location.pathname}#rooms`,
                );
              }}
            >
              처음으로
            </button>
          </div>
          {room.status === "LOBBY" && (
            <div className="lobby settings-card">
              <span className="eyebrow">INVITE YOUR LUNCH MATES</span>
              <h2>참여자 초대</h2>
              <div className="invite-code">
                {room.roomId}
                <button
                  className="icon-button"
                  aria-label="초대 링크 복사"
                  onClick={async () => {
                    try {
                      await navigator.clipboard.writeText(
                        `${window.location.origin}/?room=${room.roomId}#rooms`,
                      );
                      setMessage("초대 링크를 복사했습니다.");
                    } catch {
                      setMessage(
                        `초대 코드를 직접 공유해 주세요: ${room.roomId}`,
                      );
                    }
                  }}
                >
                  <Copy size={19} />
                </button>
              </div>
              <div className="member-list">
                {room.members.map((m) => (
                  <span key={m.id}>
                    <span>{m.nickname.slice(0, 1)}</span>
                    {m.nickname}
                    {m.id === room.hostId && <small>방장</small>}
                  </span>
                ))}
              </div>
              {member === room.hostId ? (
                <button
                  className="button primary"
                  disabled={busy}
                  onClick={() =>
                    perform(async () =>
                      setRoom(
                        await post<Room>(`/rooms/${room.roomId}/start`, {
                          hostId: member,
                        }),
                      ),
                    )
                  }
                >
                  이 멤버로 시작
                  <ArrowRight size={17} />
                </button>
              ) : (
                <p>방장의 시작을 기다리고 있습니다.</p>
              )}
            </div>
          )}
          {room.status === "PLAYING" && (
            <div className="voting settings-card">
              {nextMenu ? (
                <>
                  <span className="eyebrow">
                    {swiped.length + 1} / {room.defaultMenus.length}
                  </span>
                  <p>메뉴 선택</p>
                  <h2>{nextMenu}</h2>
                  <div className="vote-buttons">
                    <button
                      className="button secondary"
                      disabled={busy}
                      onClick={() => vote(false)}
                    >
                      <X size={21} />
                      선호하지 않음
                    </button>
                    <button
                      className="button primary"
                      disabled={busy}
                      onClick={() => vote(true)}
                    >
                      <Heart size={21} />
                      선호함
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <h2>내 선택은 끝!</h2>
                  <p>
                    투표 진행 중입니다. {room.completedMembersCount} /{" "}
                    {room.members.length}명 완료
                  </p>
                  <button
                    className="button secondary"
                    disabled={busy}
                    onClick={() =>
                      perform(async () =>
                        setRoom(await api<Room>(`/rooms/${room.roomId}`)),
                      )
                    }
                  >
                    <RefreshCw size={16} />
                    진행 상태 확인
                  </button>
                </>
              )}
            </div>
          )}
          {room.status === "COMPLETED" && (
            <div className="match-result">
              <span className="eyebrow">투표 결과</span>
              <h2>
                오늘은 <em>{room.winningMenu}</em>
              </h2>
              <p>참여자 투표를 합산한 결과입니다.</p>
              {room.locationAddress && (
                <p className="match-location">
                  <MapPin size={14} /> {room.locationAddress} 기준 1km
                </p>
              )}
              {room.matchedRestaurants.length > 0 && (
                <KakaoMap
                  matchedRestaurants={room.matchedRestaurants}
                  location={room.location}
                  latitude={room.latitude}
                  longitude={room.longitude}
                />
              )}
              <div className="restaurant-list">
                {room.matchedRestaurants.length ? (
                  room.matchedRestaurants.map((r) => (
                    <a
                      key={r.id}
                      href={
                        r.placeUrl?.startsWith("https://") ||
                        r.placeUrl?.startsWith("http://")
                          ? r.placeUrl
                          : `https://map.kakao.com/link/search/${encodeURIComponent(`${r.address} ${r.name}`)}`
                      }
                      target="_blank"
                      rel="noreferrer"
                    >
                      <div>
                        <h3>{r.name}</h3>
                        <div className="restaurant-meta">
                          <span>
                            {r.matchType === "MENU_MATCH" ? `${room.winningMenu} 검색 결과` : "주변 음식점"}
                          </span>
                          {r.distanceMeters > 0 && <span>{formatDistance(r.distanceMeters)}</span>}
                        </div>
                        <p>{r.address}</p>
                      </div>
                      <ExternalLink size={19} />
                    </a>
                  ))
                ) : (
                  <p>
                    주변 식당을 찾을 수 없습니다.{" "}
                    <a
                      href={`https://map.kakao.com/link/search/${encodeURIComponent(`${room.location} ${room.winningMenu}`)}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      카카오맵에서 찾아보기 ↗
                    </a>
                  </p>
                )}
              </div>
              <button
                className="button secondary"
                onClick={() => {
                  const saved = readLocal<string[]>("club-players", []);
                  if (!saved.length)
                    saveLocal(
                      "club-players",
                      room.members.map((m) => m.nickname),
                    );
                  window.location.hash = "games";
                }}
              >
                식후 커피내기 하러 가기
                <ArrowRight size={17} />
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
};
