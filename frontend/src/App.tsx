import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Flame, Compass, Users, Sparkles, MapPin, ArrowRight, CheckCircle2, RefreshCw, Phone, Info, AlertCircle } from 'lucide-react';
import TinderCard from 'react-tinder-card';
import { useWebSocket, WebSocketRoomResponse } from './hooks/useWebSocket';
import { KakaoMap } from './components/KakaoMap';

// 메뉴 카테고리 정보 및 아이콘 정보 매핑
const MENU_METADATA: Record<string, { emoji: string; category: string; description: string; gradient: string }> = {
  "삼겹살": { emoji: "🥩", category: "한식 / 고기", description: "노릇노릇 잘 구워진 국민 회식 메뉴 삼겹살!", gradient: "from-orange-500 to-red-600" },
  "김치찌개": { emoji: "🍲", category: "한식 / 찌개", description: "칼칼하고 깊은 국물맛의 한국인 소울푸드!", gradient: "from-red-500 to-amber-600" },
  "치킨": { emoji: "🍗", category: "야식 / 튀김", description: "바삭함의 대명사! 오늘 저녁은 치느님 영접?", gradient: "from-amber-400 to-orange-500" },
  "초밥": { emoji: "🍣", category: "일식 / 해산물", description: "신선한 횟감과 알맞은 밥알의 깔끔한 조화!", gradient: "from-cyan-400 to-blue-500" },
  "돈카츠": { emoji: "🐷", category: "일식 / 튀김", description: "두툼한 등심을 바삭하게 튀겨낸 겉바속촉 카츠!", gradient: "from-amber-500 to-yellow-600" },
  "라멘": { emoji: "🍜", category: "일식 / 면류", description: "진한 돈코츠 육수에 차슈가 듬뿍 들어간 라멘!", gradient: "from-yellow-500 to-amber-600" },
  "짜장면": { emoji: "🥢", category: "중식 / 면류", description: "달콤 짭조름한 춘장 소스에 슥슥 비벼 먹는 별미!", gradient: "from-zinc-700 to-black" },
  "짬뽕": { emoji: "🌶️", category: "중식 / 매콤면", description: "해물 베이스의 얼큰하고 불맛 가득한 빨간 국물!", gradient: "from-red-600 to-red-800" },
  "마라탕": { emoji: "🥘", category: "아시안 / 매운맛", description: "혀끝이 얼얼해지는 중독성 최강의 트렌디 마라탕!", gradient: "from-red-500 to-red-700" },
  "피자": { emoji: "🍕", category: "양식 / 피자", description: "고소한 치즈가 길게 늘어나는 맛의 끝판왕 피자!", gradient: "from-yellow-400 to-red-500" },
  "파스타": { emoji: "🍝", category: "양식 / 면류", description: "크림, 토마토, 오일 등 취향대로 고르는 우아한 파스타!", gradient: "from-emerald-400 to-teal-600" },
  "스테이크": { emoji: "🥩", category: "양식 / 고기", description: "육즙을 꽉 잡아 미디엄으로 구워낸 명품 스테이크!", gradient: "from-stone-600 to-red-900" },
  "떡볶이": { emoji: "🌶️", category: "분식 / 매운맛", description: "쫄깃한 떡과 어묵에 매콤달콤 양념이 쏙 벤 국민 분식!", gradient: "from-red-500 to-amber-500" },
  "쌀국수": { emoji: "🍜", category: "아시안 / 면류", description: "깔끔하고 담백한 육수에 고수와 양지가 어우러진 쌀국수!", gradient: "from-teal-400 to-emerald-600" },
  "팟타이": { emoji: "🍳", category: "아시안 / 볶음면", description: "새콤달콤 소스에 새우와 두부를 볶아낸 태국 대표 요리!", gradient: "from-amber-400 to-emerald-500" }
};

const isLocalDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const BASE_URL = isLocalDev
  ? `http://localhost:8080/api/rooms`
  : `${window.location.origin}/api/rooms`;

function App() {
  // 상태 변수 정의
  const [nickname, setNickname] = useState('');
  const [location, setLocation] = useState('');
  const [roomCodeInput, setRoomCodeInput] = useState('');
  const [isJoinView, setIsJoinView] = useState(false);
  const [selectedMenus, setSelectedMenus] = useState<string[]>(Object.keys(MENU_METADATA));
  const [customCreatedMenus, setCustomCreatedMenus] = useState<string[]>([]);
  const [customMenuInput, setCustomMenuInput] = useState('');
  
  // 게임 세션 관련 정보
  const [roomId, setRoomId] = useState<string | null>(null);
  const [myMemberId, setMyMemberId] = useState<string | null>(null);
  const [roomState, setRoomState] = useState<WebSocketRoomResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // 스와이프 완료된 인덱스 추적 및 로컬 진척
  const [swipeCount, setSwipeCount] = useState(0);

  // URL에서 초대 코드가 존재하는지 확인 및 자동 뷰 세팅
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomCode = urlParams.get('room');
    if (roomCode) {
      setRoomCodeInput(roomCode);
      setIsJoinView(true);
    }
  }, []);

  // 실시간 웹소켓 수신 이벤트 바인딩
  const handleWebSocketMessage = useCallback((updatedState: WebSocketRoomResponse) => {
    console.log('📬 WebSocket state update received:', updatedState);
    setRoomState(updatedState);
    
    // 만약 방 상태가 PLAYING으로 변경되었다면, 로컬 스와이프 카드를 리셋
    if (updatedState.status === 'PLAYING' && roomState?.status === 'LOBBY') {
      setSwipeCount(0);
    }
  }, [roomState]);

  // 실시간 웹소켓 수신 활성화
  useWebSocket(roomId, handleWebSocketMessage);

  // 커스텀 메뉴 직접 추가 처리
  const handleAddCustomMenu = () => {
    const trimmed = customMenuInput.trim();
    if (!trimmed) return;

    if (Object.keys(MENU_METADATA).includes(trimmed) || customCreatedMenus.includes(trimmed)) {
      alert('이미 존재하는 메뉴입니다.');
      return;
    }

    setCustomCreatedMenus([...customCreatedMenus, trimmed]);
    setSelectedMenus([...selectedMenus, trimmed]);
    setCustomMenuInput('');
  };

  // 방 개설 API 송신
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedMenus.length < 2) {
      setErrorMessage('최소 2개 이상의 메뉴를 선택해야 방을 생성할 수 있습니다.');
      return;
    }
    setLoading(true);
    setErrorMessage(null);

    try {
      const response = await fetch(BASE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hostNickname: nickname, location, customMenus: selectedMenus })
      });

      if (!response.ok) {
        throw new Error('방 개설에 실패했습니다. 백엔드 전원을 확인하세요.');
      }

      const data = await response.json();
      console.log('✅ Room Created:', data);
      setRoomId(data.roomId);
      setMyMemberId(data.hostId); // 방장의 memberId 세팅
      setRoomState(data);
    } catch (err: any) {
      setErrorMessage(err.message || '네트워크 통신 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 대기방 참가 API 송신
  const handleJoinRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname || !roomCodeInput) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const formattedCode = roomCodeInput.trim().toUpperCase();
      const response = await fetch(`${BASE_URL}/${formattedCode}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ guestNickname: nickname })
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || '방 참가에 실패했습니다. 코드를 확인하세요.');
      }

      const data = await response.json();
      console.log('✅ Room Joined:', data);
      setRoomId(data.roomId);

      // 나 자신이 속한 memberId 획득 및 매칭
      // 최근에 합류한 닉네임과 동일한 멤버 식별
      const me = data.members.find((m: any) => m.nickname === nickname && !m.isReady);
      if (me) {
        setMyMemberId(me.id);
      } else {
        // 비상 시 마지막 멤버 매칭
        setMyMemberId(data.members[data.members.length - 1].id);
      }
      setRoomState(data);
    } catch (err: any) {
      setErrorMessage(err.message || '네트워크 통신 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 방장의 게임(투표) 시작 API 송신
  const handleStartGame = async () => {
    if (!roomId || !myMemberId) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const response = await fetch(`${BASE_URL}/${roomId}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hostId: myMemberId })
      });

      if (!response.ok) {
        throw new Error('투표 시작 실패: 방장 전용 기능입니다.');
      }

      const data = await response.json();
      setRoomState(data);
    } catch (err: any) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // 스와이프 투표 결과 제출 API 송신
  const submitSwipe = async (menuName: string, isLike: boolean) => {
    if (!roomId || !myMemberId) return;
    try {
      const response = await fetch(`${BASE_URL}/${roomId}/swipes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          memberId: myMemberId,
          menuName: menuName,
          isLike: isLike
        })
      });

      if (response.ok) {
        const data = await response.json();
        setRoomState(data);
      }
    } catch (err) {
      console.error('❌ Failed to submit swipe:', err);
    }
  };

  // 스와이프 방향을 임시 저장하는 Ref (재렌더링 방지 및 화면 이탈 시점 동기화)
  const swipeDirectionsRef = useRef<Record<string, string>>({});

  // 빠른 스와이프 발생 시 HTTP 요청이 꼬이지 않도록 직렬화 처리용 비동기 큐
  const swipeQueueRef = useRef<{ menuName: string; isLike: boolean }[]>([]);
  const isProcessingQueueRef = useRef(false);

  // 대기 중인 스와이프 큐를 순차적으로 꺼내서 서버로 전송 (JPA 동시성 손실 보장)
  const processSwipeQueue = async () => {
    if (isProcessingQueueRef.current || swipeQueueRef.current.length === 0) return;
    isProcessingQueueRef.current = true;

    try {
      while (swipeQueueRef.current.length > 0) {
        const nextSwipe = swipeQueueRef.current[0];
        // submitSwipe가 완료되어 서버 상태(roomState)가 갱신될 때까지 대기
        await submitSwipe(nextSwipe.menuName, nextSwipe.isLike);
        swipeQueueRef.current.shift(); // 성공 및 완료 후 큐에서 제거
      }
    } catch (err) {
      console.error('❌ Error processing swipe queue:', err);
    } finally {
      isProcessingQueueRef.current = false;
      // 혹시 예외 발생 등으로 큐가 남아있을 경우를 대비해 다시 가동
      if (swipeQueueRef.current.length > 0) {
        processSwipeQueue();
      }
    }
  };

  // Tinder 카드 스와이프 물리 이벤트 핸들러
  const handleCardSwipe = (direction: string, menuName: string) => {
    swipeDirectionsRef.current[menuName] = direction;
    console.log(`👉 Swiped ${menuName} to the ${direction}`);
  };

  // Tinder 카드 화면 이탈 완료 핸들러 (애니메이션이 끝난 후 상태 갱신)
  const handleCardLeftScreen = (menuName: string) => {
    const direction = swipeDirectionsRef.current[menuName] || 'left';
    const isLike = direction === 'right';
    console.log(`👉 Card left screen: ${menuName} to the ${direction}`);
    
    // 큐에 스와이프 삽입 후 순차적 처리 개시
    swipeQueueRef.current.push({ menuName, isLike });
    processSwipeQueue();

    setSwipeCount(prev => prev + 1);
  };

  // 클립보드에 초대 코드 복사 함수
  const copyInviteLink = () => {
    if (!roomId) return;
    navigator.clipboard.writeText(roomId);
    alert('🔗 초대 코드가 클립보드에 성공적으로 복사되었습니다! 친구들에게 공유해 보세요.');
  };

  // 로컬 세션 리셋하고 처음으로 돌아가기
  const resetSession = () => {
    setRoomId(null);
    setMyMemberId(null);
    setRoomState(null);
    setSwipeCount(0);
    setErrorMessage(null);
    window.history.pushState({}, document.title, window.location.pathname);
  };

  return (
    <div className="min-h-screen bg-stone-50 flex flex-col justify-between">

      {/* Header */}
      <header className="max-w-5xl mx-auto w-full px-6 py-5 flex items-center justify-between">
        <div className="flex items-center gap-2.5 cursor-pointer" onClick={resetSession}>
          <div className="w-9 h-9 rounded-lg bg-orange-500 flex items-center justify-center text-white">
            <Flame className="w-4.5 h-4.5" />
          </div>
          <div className="flex flex-col">
            <span className="text-base font-black text-zinc-800 leading-none">아무거나 버스터</span>
            <span className="text-[9px] font-semibold text-orange-500 uppercase tracking-wider mt-0.5 font-mono">Amugeona Buster</span>
          </div>
        </div>
        
        {roomId && roomState && (
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex bg-zinc-800 text-white px-3 py-1.5 rounded-lg text-xs font-medium items-center gap-1.5">
              <Users className="w-3.5 h-3.5" />
              {roomState.members.find(m => m.id === myMemberId)?.nickname}
            </div>
            <button 
              onClick={resetSession}
              className="text-xs font-medium text-zinc-500 hover:text-zinc-700 bg-white hover:bg-zinc-50 px-3 py-1.5 rounded-lg border border-zinc-200 transition-colors"
            >
              나가기
            </button>
          </div>
        )}
      </header>

      {/* Main Container */}
      <main className="max-w-5xl mx-auto w-full px-6 py-6 flex-grow flex flex-col justify-center items-center">
        
        {/* Error Notification */}
        {errorMessage && (
          <div className="w-full max-w-md bg-red-50 border border-red-200 rounded-lg p-4 mb-6 flex items-start gap-3">
            <Info className="w-4 h-4 text-red-500 mt-0.5 shrink-0" />
            <div className="flex-1">
              <h4 className="text-sm font-semibold text-red-800">오류 발생</h4>
              <p className="text-xs text-red-600 mt-0.5">{errorMessage}</p>
            </div>
          </div>
        )}

        {/* ==================== 1. LANDING PHASE ==================== */}
        {!roomId && (
          <div className="w-full grid md:grid-cols-12 gap-10 items-center">
            {/* Left: Hero Copy */}
            <div className="md:col-span-7 flex flex-col gap-5 text-center md:text-left">
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-zinc-900 leading-tight">
                약속 메뉴 정할 땐,<br />
                <span className="text-orange-500">스와이프 한 번이면 끝</span>
              </h1>
              <p className="text-zinc-500 text-sm sm:text-base max-w-md leading-relaxed mx-auto md:mx-0">
                더 이상 "아무거나"는 없습니다. 친구들과 실시간으로 메뉴 카드를 밀어서 투표하면, 모두가 만족할 메뉴를 찾아드려요.
              </p>

              {/* Feature Pills */}
              <div className="flex flex-wrap gap-3 mt-2 justify-center md:justify-start">
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Users className="w-4 h-4 text-orange-500" />
                  실시간 대기실
                </div>
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Flame className="w-4 h-4 text-orange-500" />
                  스와이프 투표
                </div>
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Compass className="w-4 h-4 text-orange-500" />
                  맛집 매칭 지도
                </div>
              </div>
            </div>

            {/* Right: Setup Form Card */}
            <div className="md:col-span-5 w-full max-w-md mx-auto">
            <div className="w-full bg-white border border-zinc-200 shadow-sm rounded-xl p-7">
              
              {/* View Tabs */}
              <div className="flex border-b border-zinc-200 mb-6">
                <button 
                  onClick={() => { setIsJoinView(false); setErrorMessage(null); }}
                  className={`flex-1 pb-3 text-sm font-medium transition-colors border-b-2 ${!isJoinView ? 'border-orange-500 text-orange-600' : 'border-transparent text-zinc-400 hover:text-zinc-600'}`}
                >
                  방 만들기
                </button>
                <button 
                  onClick={() => { setIsJoinView(true); setErrorMessage(null); }}
                  className={`flex-1 pb-3 text-sm font-medium transition-colors border-b-2 ${isJoinView ? 'border-orange-500 text-orange-600' : 'border-transparent text-zinc-400 hover:text-zinc-600'}`}
                >
                  참여하기
                </button>
              </div>

              {!isJoinView ? (
                /* CREATE ROOM FORM */
                <form onSubmit={handleCreateRoom} className="flex flex-col gap-4">
                  <p className="text-xs text-zinc-500">대기실을 만들고 친구들에게 초대 코드를 공유하세요.</p>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-medium text-zinc-600 px-1">닉네임</label>
                    <input
                      type="text"
                      required
                      placeholder="예: 김방장"
                      value={nickname}
                      onChange={(e) => setNickname(e.target.value)}
                      className="w-full px-4 py-3 rounded-lg border border-zinc-200 bg-white text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-colors"
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-medium text-zinc-600 px-1">약속 장소</label>
                    <div className="relative">
                      <input
                        type="text"
                        required
                        placeholder="예: 강남역, 홍대입구"
                        value={location}
                        onChange={(e) => setLocation(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 rounded-lg border border-zinc-200 bg-white text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-colors"
                      />
                      <MapPin className="w-4 h-4 text-zinc-400 absolute left-3.5 top-3.5" />
                    </div>
                  </div>

                  <div className="flex flex-col gap-2">
                    <div className="flex justify-between items-center px-1">
                      <label className="text-xs font-semibold text-zinc-700">투표 메뉴 커스텀 선택</label>
                      <span className="text-xs font-medium text-orange-600 font-mono">
                        {selectedMenus.length}개 선택됨
                      </span>
                    </div>

                    {/* Quick Macros */}
                    <div className="grid grid-cols-4 gap-1.5 bg-zinc-50 p-1 rounded-lg border border-zinc-200">
                      {[
                        { label: '5개 선택', action: () => setSelectedMenus(Object.keys(MENU_METADATA).slice(0, 5)) },
                        { label: '10개 선택', action: () => setSelectedMenus(Object.keys(MENU_METADATA).slice(0, 10)) },
                        { label: '전체 선택', action: () => setSelectedMenus(Object.keys(MENU_METADATA)) },
                        { label: '선택 해제', action: () => setSelectedMenus([]) }
                      ].map((macro) => (
                        <button
                          key={macro.label}
                          type="button"
                          onClick={macro.action}
                          className="py-1.5 text-[10px] font-medium rounded-md bg-white border border-zinc-200 text-zinc-600 hover:bg-zinc-50 active:scale-95 transition-all shadow-xs"
                        >
                          {macro.label}
                        </button>
                      ))}
                    </div>

                    {/* Checklist Grid */}
                    <div className="grid grid-cols-3 gap-2 max-h-56 overflow-y-auto p-2 bg-white rounded-lg border border-zinc-200">
                      {[
                        ...Object.entries(MENU_METADATA),
                        ...customCreatedMenus.map(m => [m, { emoji: "🍴", category: "커스텀", description: "방장이 직접 추가한 메뉴입니다.", gradient: "from-orange-500 to-amber-500" }] as [string, { emoji: string; category: string; description: string; gradient: string; }])
                      ].map(([menuName, meta]) => {
                        const isChecked = selectedMenus.includes(menuName);
                        const isCustom = customCreatedMenus.includes(menuName);
                        return (
                          <div key={menuName} className="relative">
                            <button
                              type="button"
                              onClick={() => {
                                if (isChecked) {
                                  setSelectedMenus(selectedMenus.filter(m => m !== menuName));
                                } else {
                                  setSelectedMenus([...selectedMenus, menuName]);
                                }
                              }}
                              className={`w-full flex flex-col items-center justify-center p-2 rounded-xl border text-center transition-all ${
                                isChecked
                                  ? 'border-orange-500 bg-orange-50/40 text-orange-950 font-semibold shadow-xs ring-1 ring-orange-500/20'
                                  : 'border-zinc-100 bg-zinc-50/50 hover:bg-zinc-50 text-zinc-600 hover:border-zinc-200'
                              }`}
                            >
                              <span className="text-xl mb-1">{meta.emoji}</span>
                              <span className="text-[10px] truncate w-full">{menuName}</span>
                              <span className={`text-[7px] mt-0.5 px-1 py-0.5 rounded-sm border ${
                                isChecked
                                  ? 'bg-orange-100/50 text-orange-700 border-orange-200/50'
                                  : 'bg-zinc-100/70 text-zinc-400 border-zinc-200/20'
                              }`}>
                                {meta.category.split('/')[0].trim()}
                              </span>
                            </button>
                            {isCustom && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setCustomCreatedMenus(customCreatedMenus.filter(m => m !== menuName));
                                  setSelectedMenus(selectedMenus.filter(m => m !== menuName));
                                }}
                                className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-red-500 text-white flex items-center justify-center text-[8px] font-bold hover:bg-red-600 shadow-sm active:scale-90 transition-all border border-white"
                              >
                                ×
                              </button>
                            )}
                          </div>
                        );
                      })}
                    </div>

                    {/* 직접 추가 입력창 */}
                    <div className="flex gap-2 mt-1">
                      <div className="relative flex-1">
                        <input
                          type="text"
                          placeholder="원하는 메뉴 직접 입력 (예: 마라엽떡)"
                          value={customMenuInput}
                          onChange={(e) => setCustomMenuInput(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              handleAddCustomMenu();
                            }
                          }}
                          className="w-full px-3 py-2 text-xs rounded-lg border border-zinc-200 bg-white text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-1 focus:ring-orange-500 focus:border-orange-500 transition-colors"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={handleAddCustomMenu}
                        className="px-4 py-2 text-xs font-semibold rounded-lg bg-zinc-800 hover:bg-zinc-900 text-white transition-colors active:scale-95 shadow-sm"
                      >
                        추가
                      </button>
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full mt-2 py-3.5 rounded-lg bg-orange-500 hover:bg-orange-600 text-white font-medium text-sm transition-colors flex items-center justify-center gap-2 disabled:opacity-60"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        대기실 만들기
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </form>
              ) : (
                /* JOIN ROOM FORM */
                <form onSubmit={handleJoinRoom} className="flex flex-col gap-4">
                  <p className="text-xs text-zinc-500">친구에게 받은 초대 코드를 입력해서 참가하세요.</p>
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-medium text-zinc-600 px-1">초대 코드</label>
                    <input
                      type="text"
                      required
                      placeholder="예: ROOM-A7B8C9"
                      value={roomCodeInput}
                      onChange={(e) => setRoomCodeInput(e.target.value)}
                      className="w-full px-4 py-3 rounded-lg border border-zinc-200 bg-white text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-colors font-mono tracking-wider"
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-medium text-zinc-600 px-1">닉네임</label>
                    <input
                      type="text"
                      required
                      placeholder="예: 홍길동"
                      value={nickname}
                      onChange={(e) => setNickname(e.target.value)}
                      className="w-full px-4 py-3 rounded-lg border border-zinc-200 bg-white text-zinc-800 placeholder-zinc-400 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-colors"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full mt-2 py-3.5 rounded-lg bg-orange-500 hover:bg-orange-600 text-white font-medium text-sm transition-colors flex items-center justify-center gap-2 disabled:opacity-60"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <>
                        참여하기
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </form>
              )}
            </div>
            </div>
          </div>
        )}

        {/* ==================== 2. LOBBY PHASE ==================== */}
        {roomId && roomState && roomState.status === 'LOBBY' && (
          <div className="w-full max-w-xl bg-white border border-zinc-200 shadow-sm rounded-xl p-7 relative">
            <div className="absolute top-5 right-5 flex items-center gap-1.5 text-emerald-600 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              실시간 연결됨
            </div>

            <div className="flex flex-col gap-1">
              <span className="text-xs font-medium text-orange-500 uppercase tracking-wide">대기실</span>
              <h2 className="text-2xl font-bold text-zinc-800">
                친구들을 기다리는 중
              </h2>
              <p className="text-zinc-500 text-xs mt-1">방장이 시작 버튼을 누르면 투표가 시작됩니다.</p>
            </div>

            {/* Room Info */}
            <div className="grid grid-cols-2 gap-3 mt-6">
              <div className="bg-zinc-50 border border-zinc-100 p-4 rounded-lg">
                <span className="text-zinc-400 text-xs block">초대 코드</span>
                <button 
                  onClick={copyInviteLink}
                  className="text-base font-mono font-bold text-zinc-800 mt-1 hover:text-orange-500 transition-colors flex items-center gap-1.5"
                >
                  {roomState.roomId}
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="bg-zinc-50 border border-zinc-100 p-4 rounded-lg">
                <span className="text-zinc-400 text-xs block">약속 장소</span>
                <span className="text-base font-medium text-zinc-800 mt-1 flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-zinc-400" />
                  {roomState.location}
                </span>
              </div>
            </div>

            {/* Member List */}
            <div className="mt-7">
              <h3 className="text-sm font-medium text-zinc-600 flex items-center gap-1.5 mb-3">
                <Users className="w-4 h-4 text-zinc-400" />
                참여자 ({roomState.members.length}명)
              </h3>
              
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {roomState.members.map((member) => {
                  const isHost = member.id === roomState.hostId;
                  const isMe = member.id === myMemberId;

                  return (
                    <div 
                      key={member.id}
                      className={`p-3.5 rounded-lg border transition-colors flex flex-col gap-1 relative overflow-hidden ${
                        isMe 
                          ? 'bg-orange-50 border-orange-200' 
                          : 'bg-white border-zinc-200'
                      }`}
                    >
                      {isHost && (
                        <span className="absolute top-0 right-0 bg-orange-500 text-white text-[9px] font-bold px-2 py-0.5 rounded-bl-lg">
                          방장
                        </span>
                      )}
                      
                      <span className="text-sm font-medium text-zinc-800 block truncate pr-8">
                        {member.nickname} {isMe && "(나)"}
                      </span>
                      
                      <span className="text-[10px] text-zinc-400 flex items-center gap-1 mt-0.5">
                        <CheckCircle2 className="w-3 h-3 text-emerald-500" />
                        대기 중
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Action Bar */}
            <div className="mt-7 pt-5 border-t border-zinc-100 flex gap-3">
              {roomState.hostId === myMemberId ? (
                <button
                  onClick={handleStartGame}
                  disabled={loading}
                  className="flex-1 py-3.5 bg-orange-500 hover:bg-orange-600 text-white font-medium rounded-lg transition-colors text-center flex items-center justify-center gap-2 text-sm"
                >
                  {loading ? (
                    <RefreshCw className="w-4 h-4 animate-spin" />
                  ) : (
                    <>
                      투표 시작하기
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              ) : (
                <div className="flex-1 py-3.5 bg-zinc-50 border border-zinc-200 text-zinc-500 text-center font-medium rounded-lg flex items-center justify-center gap-2 text-sm">
                  <RefreshCw className="w-3.5 h-3.5 animate-spin text-zinc-400" />
                  방장이 시작하기를 기다리는 중...
                </div>
              )}
            </div>
          </div>
        )}

        {/* ==================== 3. SWIPE PHASE ==================== */}
        {roomId && roomState && roomState.status === 'PLAYING' && (
          <div className="w-full max-w-md flex flex-col gap-5 items-center">
            
            {/* Top Info Bar */}
            <div className="w-full bg-white border border-zinc-200 rounded-lg p-4 shadow-sm flex items-center justify-between">
              <div>
                <span className="text-[10px] font-medium text-orange-500 block uppercase tracking-wide">투표 진행 중</span>
                <span className="text-zinc-800 text-sm font-medium">먹고 싶은 메뉴를 선택하세요</span>
              </div>
              <div className="bg-zinc-100 text-zinc-600 px-3 py-1.5 rounded-lg text-xs font-medium">
                {swipeCount} / {roomState.maxSwipeCount}
              </div>
            </div>

            {/* Real-time Group Progress Bar */}
            <div className="w-full bg-white border border-zinc-200 rounded-lg p-4 shadow-sm">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-medium text-zinc-600 flex items-center gap-1">
                  <Users className="w-3.5 h-3.5 text-zinc-400" />
                  그룹 진행률
                </span>
                <span className="text-xs font-medium text-orange-600">
                  {roomState.completedMembersCount} / {roomState.totalMembers}명 완료
                </span>
              </div>
              <div className="w-full h-2 bg-zinc-100 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-orange-500 transition-all duration-500 rounded-full"
                  style={{ width: `${(roomState.completedMembersCount / roomState.totalMembers) * 100}%` }}
                />
              </div>
              <p className="text-[10px] text-zinc-400 mt-2 text-center">전원 완료 시 자동으로 결과가 표시됩니다.</p>
            </div>

            {/* Tinder Cards Stack Container */}
            <div className="relative w-full h-[400px] flex justify-center items-center">
              {swipeCount >= roomState.maxSwipeCount ? (
                /* ALL SWIPED LOCAL WAITING */
                <div className="w-full h-full bg-white border border-zinc-200 shadow-sm rounded-xl p-8 flex flex-col justify-center items-center text-center gap-4">
                  <div className="w-14 h-14 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-500">
                    <CheckCircle2 className="w-7 h-7" />
                  </div>
                  <h3 className="text-lg font-bold text-zinc-800 mt-1">투표 완료</h3>
                  <p className="text-zinc-500 text-sm max-w-xs leading-relaxed">
                    다른 참여자들의 투표가 끝나면 결과가 자동으로 표시됩니다.
                  </p>
                  <div className="flex items-center gap-2 bg-zinc-800 text-white px-4 py-2 rounded-lg text-xs font-medium mt-1">
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    대기 중 ({roomState.completedMembersCount} / {roomState.totalMembers}명)
                  </div>
                </div>
              ) : (
                /* TINDER CARDS DECK - 역순 렌더링으로 현재 카드가 DOM 최상단(z-index 최고) */
                [...roomState.defaultMenus.slice(swipeCount)].reverse().map((menu, reversedIdx, arr) => {
                  const originalIndex = swipeCount + (arr.length - 1 - reversedIdx);
                  const meta = MENU_METADATA[menu] || { emoji: "🍴", category: "음식", description: "맛있는 음식 카드를 밀어주세요!", gradient: "from-orange-500 to-amber-500" };

                  return (
                    <TinderCard
                      className="absolute w-full h-full cursor-grab active:cursor-grabbing"
                      key={`${menu}-${originalIndex}`}
                      onSwipe={(dir) => handleCardSwipe(dir, menu)}
                      onCardLeftScreen={() => handleCardLeftScreen(menu)}
                      preventSwipe={['up', 'down']}
                    >
                      <div className={`w-full h-full bg-gradient-to-br ${meta.gradient} rounded-xl p-7 shadow flex flex-col justify-between text-white relative overflow-hidden`}>
                        {/* Subtle overlay */}
                        <div className="absolute inset-0 bg-black/10 pointer-events-none" />

                        {/* Top Category Badge */}
                        <div className="z-10 flex justify-between items-center">
                          <span className="bg-white/20 px-3 py-1 rounded-lg text-xs font-medium border border-white/10">
                            {meta.category}
                          </span>
                          <span className="text-white/50 text-xs font-mono">
                            {originalIndex + 1} / {roomState.maxSwipeCount}
                          </span>
                        </div>

                        {/* Middle Emoji & Name */}
                        <div className="z-10 text-center my-6 flex flex-col items-center gap-3">
                          <span className="text-5xl block">
                            {meta.emoji}
                          </span>
                          <h3 className="text-3xl font-bold drop-shadow-sm">
                            {menu}
                          </h3>
                        </div>

                        {/* Bottom Description */}
                        <div className="z-10 bg-white/10 border border-white/10 p-4 rounded-lg">
                          <p className="text-xs leading-relaxed text-white/90">
                            {meta.description}
                          </p>
                        </div>

                        {/* Swipe Direction Helper */}
                        <div className="z-10 flex justify-between items-center mt-2 px-1 text-[10px] font-medium text-white/50">
                          <span>← 싫어요</span>
                          <span>좋아요 →</span>
                        </div>
                      </div>
                    </TinderCard>
                  );
                })
              )}
            </div>
          </div>
        )}

        {/* ==================== 4. RESULT PHASE ==================== */}
        {roomId && roomState && roomState.status === 'COMPLETED' && (
          <div className="w-full max-w-4xl grid md:grid-cols-12 gap-6 items-start">
            
            {/* Left Col: Winner Card */}
            <div className="md:col-span-5 bg-white border border-zinc-200 shadow-sm rounded-xl p-7 text-center flex flex-col items-center gap-5 relative overflow-hidden">
              <div className="absolute top-0 inset-x-0 h-1 bg-orange-500" />
              
              <div className="inline-flex items-center gap-1 bg-orange-50 text-orange-600 text-xs font-medium px-3 py-1 rounded-lg border border-orange-100">
                <Sparkles className="w-3.5 h-3.5" /> 매칭 완료
              </div>

              <div>
                <span className="text-zinc-400 text-xs block">모두가 선택한 메뉴</span>
                <span className="text-6xl block mt-3">
                  {MENU_METADATA[roomState.winningMenu || ""]?.emoji || "🍴"}
                </span>
                <h2 className="text-3xl font-bold text-zinc-800 mt-3">
                  {roomState.winningMenu}
                </h2>
              </div>

              {/* Vote Stats */}
              {roomState.voteStats && roomState.voteStats.length > 0 && (
                <div className="w-full">
                  <p className="text-xs font-semibold text-zinc-500 mb-2.5 flex items-center gap-1.5">
                    <span>📊</span> 메뉴별 투표 결과
                  </p>
                  <div className="flex flex-col gap-2">
                    {[...roomState.voteStats]
                      .sort((a, b) => (b.likes + b.dislikes) - (a.likes + a.dislikes))
                      .map((stat) => {
                        const total = stat.likes + stat.dislikes;
                        const maxTotal = Math.max(...roomState.voteStats!.map(s => s.likes + s.dislikes));
                        const barWidth = maxTotal > 0 ? (total / maxTotal) * 100 : 0;
                        const isWinner = stat.menuName === roomState.winningMenu;
                        return (
                          <div key={stat.menuName} className="flex items-center gap-2">
                            <span className="text-xs text-zinc-600 w-14 text-right shrink-0 truncate" title={stat.menuName}>
                              {MENU_METADATA[stat.menuName]?.emoji || '🍴'} {stat.menuName}
                            </span>
                            <div className="flex-1 bg-zinc-100 rounded-full h-2 overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all duration-700 ${isWinner ? 'bg-orange-500' : 'bg-zinc-300'}`}
                                style={{ width: `${barWidth}%` }}
                              />
                            </div>
                            <span className="text-[10px] text-zinc-400 shrink-0 flex items-center gap-1">
                              <span className="text-green-500 font-bold">❤️{stat.likes}</span>
                              <span className="text-red-400 font-bold">👎{stat.dislikes}</span>
                            </span>
                          </div>
                        );
                      })}
                  </div>
                </div>
              )}

              <button
                onClick={resetSession}
                className="w-full py-3 bg-zinc-800 hover:bg-zinc-900 text-white font-medium rounded-lg text-sm transition-colors flex items-center justify-center gap-2"
              >
                새로운 투표 시작
              </button>
            </div>

            {/* Right Col: Restaurants */}
            <div className="md:col-span-7 flex flex-col gap-5">
              
              <KakaoMap 
                matchedRestaurants={roomState.matchedRestaurants} 
                location={roomState.location} 
              />

              {/* Restaurant List */}
              <div className="flex flex-col gap-2.5">
                <h3 className="text-sm font-medium text-zinc-700 flex items-center gap-1.5">
                  <Compass className="w-4 h-4 text-orange-500" />
                  주변 추천 맛집
                </h3>
                
                {roomState.matchedRestaurants.length === 0 ? (
                  <div className="bg-white border border-zinc-200 rounded-lg p-6 text-center shadow-sm flex flex-col items-center justify-center gap-3">
                    <AlertCircle className="w-8 h-8 text-zinc-400" />
                    <div>
                      <p className="text-sm font-semibold text-zinc-700">해당 위치 주변에 추천할 만한 맛집을 찾지 못했어요.</p>
                      <p className="text-xs text-zinc-500 mt-1">위치를 조금 더 구체적으로 변경해서 방을 다시 만들어보세요.</p>
                    </div>
                  </div>
                ) : (
                  roomState.matchedRestaurants.map((restaurant, index) => (
                  <div 
                    key={restaurant.id}
                    className="bg-white border border-zinc-200 hover:border-orange-300 rounded-lg p-4 shadow-sm hover:shadow transition-all flex items-center gap-4 group"
                  >
                    {/* Index */}
                    <div className="w-9 h-9 rounded-lg bg-orange-50 text-orange-600 font-bold text-sm flex items-center justify-center shrink-0 border border-orange-100">
                      {index + 1}
                    </div>

                    {/* Restaurant Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <h4 className="font-medium text-zinc-800 truncate text-sm group-hover:text-orange-600 transition-colors">
                          {restaurant.name}
                        </h4>
                        {restaurant.category && (
                          <span className="bg-orange-50 text-orange-600 text-[10px] font-medium px-1.5 py-0.5 rounded shrink-0 border border-orange-100">
                            {restaurant.category}
                          </span>
                        )}
                      </div>
                      
                      <span className="text-zinc-400 text-xs mt-1 block truncate flex items-center gap-1">
                        <MapPin className="w-3 h-3 shrink-0" />
                        {restaurant.address}
                      </span>
                    </div>

                    {/* Actions */}
                    <div className="flex gap-1.5 shrink-0">
                      {restaurant.placeUrl && (
                        <a
                          href={restaurant.placeUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="w-9 h-9 rounded-lg bg-zinc-50 hover:bg-orange-50 text-zinc-400 hover:text-orange-500 border border-zinc-200 hover:border-orange-200 flex items-center justify-center transition-colors"
                          title="카카오맵에서 보기"
                        >
                          <MapPin className="w-4 h-4" />
                        </a>
                      )}
                      {restaurant.phone && (
                        <a 
                          href={`tel:${restaurant.phone}`}
                          className="w-9 h-9 rounded-lg bg-zinc-50 hover:bg-orange-50 text-zinc-400 hover:text-orange-500 border border-zinc-200 hover:border-orange-200 flex items-center justify-center transition-colors shrink-0"
                          title={`전화: ${restaurant.phone}`}
                        >
                          <Phone className="w-4 h-4" />
                        </a>
                      )}
                    </div>
                  </div>
                )))}
              </div>
            </div>
          </div>
        )}

      </main>

      {/* Footer */}
      <footer className="w-full text-center py-5 text-xs text-zinc-400 border-t border-zinc-100">
        © 2026 아무거나 버스터 (Amugeona Buster)
      </footer>
    </div>
  );
}

export default App;
