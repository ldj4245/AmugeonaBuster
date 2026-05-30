import React, { useState, useEffect, useCallback } from 'react';
import { Flame, Compass, Users, Sparkles, MapPin, ArrowRight, CheckCircle2, RefreshCw, Star, Phone, Info } from 'lucide-react';
import TinderCard from 'react-tinder-card';
import { useWebSocket, WebSocketRoomResponse } from './hooks/useWebSocket';

// 메뉴 카테고리 정보 및 아이콘 정보 매핑
const MENU_METADATA: Record<string, { emoji: string; category: string; description: string; gradient: string }> = {
  "삼겹살": { emoji: "🥩", category: "한식 / 고기", description: "노릇노릇 잘 구워진 국민 회식 메뉴 삼겹살!", gradient: "from-orange-500 to-red-600" },
  "김치찌개": { emoji: "🍲", category: "한식 / 찌개", description: "칼칼하고 깊은 국물맛의 한국인 소울푸드!", gradient: "from-red-500 to-amber-600" },
  "치킨": { emoji: "🍗", category: "야식 / 튀김", description: "바삭함의 대명사! 오늘 저녁은 치느님 영접?", gradient: "from-amber-400 to-orange-500" },
  "초밥": { emoji: "🍣", category: "일식 / 해산물", description: "신선한 횟감과 알맞은 밥알의 깔끔한 조화!", gradient: "from-cyan-400 to-blue-500" },
  "돈카츠": { emoji: "🐷", category: "일식 / 튀김", description: "두툼한 등심을 바삭하게 튀겨낸 겉바속촉 카츠!", gradient: "from-amber-500 to-yellow-600" },
  "라멘": { emoji: "🍜", category: "일식 / 면류", description: "진한 돈코츠 육수에 차슈가 듬뿍 들어간 라멘!", gradient: "from-yellow-500 to-amber-600" },
  "짜장면": { emoji: "🥢", category: "중식 / 면류", description: "달콤 짭조름한 춘장 소스에 슥슥 비벼 먹는 별미!", gradient: "from-zinc-700 to-black" },
  "짬뽕": { emoji: "🌶️", category: "중식 / 매콤면", description: "해물 베이스의 얼큰하고 불맛 가득한 빨간 국물!", gradient: "from-red-600 to-rose-700" },
  "마라탕": { emoji: "🥘", category: "아시안 / 매운맛", description: "혀끝이 얼얼해지는 중독성 최강의 트렌디 마라탕!", gradient: "from-rose-500 to-red-700" },
  "피자": { emoji: "🍕", category: "양식 / 피자", description: "고소한 치즈가 길게 늘어나는 맛의 끝판왕 피자!", gradient: "from-yellow-400 to-red-500" },
  "파스타": { emoji: "🍝", category: "양식 / 면류", description: "크림, 토마토, 오일 등 취향대로 고르는 우아한 파스타!", gradient: "from-emerald-400 to-teal-600" },
  "스테이크": { emoji: "🥩", category: "양식 / 고기", description: "육즙을 꽉 잡아 미디엄으로 구워낸 명품 스테이크!", gradient: "from-stone-600 to-red-900" },
  "떡볶이": { emoji: "🌶️", category: "분식 / 매운맛", description: "쫄깃한 떡과 어묵에 매콤달콤 양념이 쏙 벤 국민 분식!", gradient: "from-rose-500 to-amber-500" },
  "쌀국수": { emoji: "🍜", category: "아시안 / 면류", description: "깔끔하고 담백한 육수에 고수와 양지가 어우러진 쌀국수!", gradient: "from-teal-400 to-emerald-600" },
  "팟타이": { emoji: "🍳", category: "아시안 / 볶음면", description: "새콤달콤 소스에 새우와 두부를 볶아낸 태국 대표 요리!", gradient: "from-amber-400 to-emerald-500" }
};

const BASE_URL = 'http://localhost:8080/api/rooms';

function App() {
  // 상태 변수 정의
  const [nickname, setNickname] = useState('');
  const [location, setLocation] = useState('');
  const [roomCodeInput, setRoomCodeInput] = useState('');
  const [isJoinView, setIsJoinView] = useState(false);
  
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

  // 커스텀 훅 가동
  useWebSocket(roomId, handleWebSocketMessage);

  // 방 개설 API 송신
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname || !location) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const response = await fetch(BASE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hostNickname: nickname, location })
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

  // Tinder 카드 스와이프 물리 이벤트 핸들러
  const handleCardSwipe = (direction: string, menuName: string) => {
    const isLike = direction === 'right';
    console.log(`👉 Swiped ${menuName} to the ${direction}`);
    
    // API로 투표 전송
    submitSwipe(menuName, isLike);
    setSwipeCount(prev => prev + 1);
  };

  // 클립보드에 초대 코드 복사 함수
  const copyInviteLink = () => {
    if (!roomId) return;
    const inviteUrl = `${window.location.origin}?room=${roomId}`;
    navigator.clipboard.writeText(inviteUrl);
    alert('🔗 초대 링크가 클립보드에 성공적으로 복사되었습니다! 친구들에게 공유해 보세요.');
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
    <div className="min-h-screen bg-gradient-to-br from-rose-50 via-slate-50 to-rose-100 flex flex-col justify-between relative overflow-hidden">
      {/* Decorative Blur Orbs */}
      <div className="absolute top-[-20%] left-[-20%] w-[60%] h-[60%] rounded-full bg-rose-200/40 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-rose-300/30 blur-[100px] pointer-events-none" />

      {/* Header */}
      <header className="max-w-6xl mx-auto w-full px-6 py-5 flex items-center justify-between z-10">
        <div className="flex items-center gap-2 cursor-pointer" onClick={resetSession}>
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-rose-600 to-orange-400 flex items-center justify-center text-white shadow-lg shadow-rose-500/20">
            <Flame className="w-5 h-5 animate-pulse" />
          </div>
          <span className="text-xl font-bold tracking-tight text-slate-800">
            오늘 뭐 먹지<span className="text-rose-500 font-extrabold">?</span>
          </span>
        </div>
        
        {roomId && roomState && (
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex bg-slate-800 text-white px-3 py-1.5 rounded-full text-xs font-bold items-center gap-1.5 shadow-sm">
              <Users className="w-3.5 h-3.5" />
              {roomState.members.find(m => m.id === myMemberId)?.nickname} (참가중)
            </div>
            <button 
              onClick={resetSession}
              className="text-xs font-bold text-rose-600 hover:text-rose-700 bg-rose-100/60 hover:bg-rose-100/90 px-3 py-1.5 rounded-full border border-rose-200/40 transition-all"
            >
              처음으로
            </button>
          </div>
        )}
      </header>

      {/* Main Container */}
      <main className="max-w-6xl mx-auto w-full px-6 py-6 flex-grow flex flex-col justify-center items-center z-10">
        
        {/* Error Notification */}
        {errorMessage && (
          <div className="w-full max-w-md bg-red-50 border border-red-200 rounded-2xl p-4 mb-6 shadow-sm flex items-start gap-3 animate-bounce">
            <div className="text-red-500 mt-0.5">⚠️</div>
            <div className="flex-1">
              <h4 className="text-sm font-bold text-red-800">시스템 연동 실패</h4>
              <p className="text-xs text-red-600 mt-0.5">{errorMessage}</p>
            </div>
          </div>
        )}

        {/* ==================== 1. LANDING PHASE ==================== */}
        {!roomId && (
          <div className="w-full grid md:grid-cols-12 gap-12 items-center">
            {/* Copywriting */}
            <div className="md:col-span-7 flex flex-col gap-6 text-center md:text-left">
              <div className="inline-flex items-center gap-2 px-3 py-1 bg-rose-100/80 backdrop-blur-sm border border-rose-200/50 text-rose-600 text-xs font-bold rounded-full w-fit mx-auto md:mx-0 shadow-sm shadow-rose-100/10">
                🔥 10초 만에 끝내는 실시간 미식 의사결정
              </div>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 leading-tight tracking-tight">
                약속 메뉴 정할 땐,<br />
                <span className="bg-gradient-to-r from-rose-500 via-orange-500 to-amber-500 bg-clip-text text-transparent">
                  카드를 밀어서 스와이프!
                </span>
              </h1>
              <p className="text-slate-600 text-base sm:text-lg max-w-lg leading-relaxed mx-auto md:mx-0">
                더 이상의 "아무거나"는 거절합니다. 친구들과 실시간 방에 모여 각자 메뉴 카드를 스와이프 하세요. 
                비토(싫어요) 방지 및 보정 알고리즘을 통해 최상의 타협점 맛집을 실시간 도출합니다.
              </p>

              {/* Feature Grid */}
              <div className="grid grid-cols-3 gap-4 mt-4 max-w-md mx-auto md:mx-0">
                <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm hover:scale-105 transition-all">
                  <Users className="w-5 h-5 text-rose-500" />
                  <span className="text-xs font-bold text-slate-700">실시간 대기실</span>
                </div>
                <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm hover:scale-105 transition-all">
                  <Flame className="w-5 h-5 text-rose-500" />
                  <span className="text-xs font-bold text-slate-700">Tinder 스와이프</span>
                </div>
                <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm hover:scale-105 transition-all">
                  <Compass className="w-5 h-5 text-rose-500" />
                  <span className="text-xs font-bold text-slate-700">맛집 매칭 지도</span>
                </div>
              </div>
            </div>

            {/* Setup Form Card (Glassmorphism) */}
            <div className="md:col-span-5 w-full max-w-md mx-auto relative">
              <div className="absolute inset-0 bg-gradient-to-tr from-rose-600/10 to-amber-500/10 rounded-3xl blur-2xl pointer-events-none" />
              <div className="bg-white/80 backdrop-blur-xl border border-white/60 shadow-2xl shadow-rose-200/30 rounded-3xl p-8 relative z-10">
                
                {/* View Tabs */}
                <div className="flex border-b border-slate-200/80 mb-6">
                  <button 
                    onClick={() => { setIsJoinView(false); setErrorMessage(null); }}
                    className={`flex-1 pb-3 text-sm font-bold transition-all border-b-2 ${!isJoinView ? 'border-rose-500 text-rose-600' : 'border-transparent text-slate-400 hover:text-slate-600'}`}
                  >
                    🚀 방 개설하기
                  </button>
                  <button 
                    onClick={() => { setIsJoinView(true); setErrorMessage(null); }}
                    className={`flex-1 pb-3 text-sm font-bold transition-all border-b-2 ${isJoinView ? 'border-rose-500 text-rose-600' : 'border-transparent text-slate-400 hover:text-slate-600'}`}
                  >
                    🔗 방 참여하기
                  </button>
                </div>

                {!isJoinView ? (
                  /* CREATE ROOM FORM */
                  <form onSubmit={handleCreateRoom} className="flex flex-col gap-4">
                    <p className="text-xs text-slate-500">대기실을 만들고 친구들에게 초대 코드를 공유해 투표를 시작하세요.</p>
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-slate-600 px-1">닉네임</label>
                      <input
                        type="text"
                        required
                        placeholder="예: 김방장"
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-white/50 text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-all font-semibold"
                      />
                    </div>

                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-slate-600 px-1">약속 기준 장소</label>
                      <div className="relative">
                        <input
                          type="text"
                          required
                          placeholder="예: 강남역, 홍대입구"
                          value={location}
                          onChange={(e) => setLocation(e.target.value)}
                          className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 bg-white/50 text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-all font-semibold"
                        />
                        <MapPin className="w-5 h-5 text-slate-400 absolute left-4 top-3.5" />
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full mt-2 py-4 rounded-xl bg-gradient-to-r from-rose-600 to-orange-500 hover:from-rose-600 hover:to-orange-600 text-white font-bold text-sm tracking-wide shadow-lg shadow-rose-500/20 transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
                    >
                      {loading ? (
                        <RefreshCw className="w-5 h-5 animate-spin" />
                      ) : (
                        <>
                          대기실 생성 및 코드 발급
                          <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                        </>
                      )}
                    </button>
                  </form>
                ) : (
                  /* JOIN ROOM FORM */
                  <form onSubmit={handleJoinRoom} className="flex flex-col gap-4">
                    <p className="text-xs text-slate-500">친구에게 받은 방 고유 코드(예: ROOM-XXXXXX)를 입력해 참가하세요.</p>
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-slate-600 px-1">초대 코드</label>
                      <input
                        type="text"
                        required
                        placeholder="예: ROOM-A7B8C9"
                        value={roomCodeInput}
                        onChange={(e) => setRoomCodeInput(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-white/50 text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-all font-mono font-bold tracking-wider"
                      />
                    </div>

                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-slate-600 px-1">나의 닉네임</label>
                      <input
                        type="text"
                        required
                        placeholder="예: 홍길동"
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-white/50 text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 transition-all font-semibold"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full mt-2 py-4 rounded-xl bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-sm tracking-wide shadow-lg shadow-orange-500/20 transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
                    >
                      {loading ? (
                        <RefreshCw className="w-5 h-5 animate-spin" />
                      ) : (
                        <>
                          대기실 입장하기
                          <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
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
          <div className="w-full max-w-2xl bg-white/80 backdrop-blur-xl border border-white/60 shadow-2xl rounded-3xl p-8 relative">
            <div className="absolute top-6 right-6 flex items-center gap-1.5 bg-rose-100 text-rose-600 text-xs font-extrabold px-3 py-1 rounded-full border border-rose-200/50">
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping" />
              실시간 동기화 활성
            </div>

            <div className="flex flex-col gap-2">
              <span className="text-xs font-bold text-rose-500 tracking-wider uppercase">LOBBY 대기방</span>
              <h2 className="text-3xl font-black text-slate-800 tracking-tight flex items-center gap-2">
                친구들을 기다리는 중
              </h2>
              <p className="text-slate-500 text-xs mt-1">방장이 게임 시작 버튼을 누르면 실시간 카드 스와이프가 개시됩니다.</p>
            </div>

            {/* Room Info Cards */}
            <div className="grid grid-cols-2 gap-4 mt-6">
              <div className="bg-slate-50 border border-slate-100 p-4 rounded-2xl">
                <span className="text-slate-400 text-xs block font-medium">초대 링크 (클릭 시 복사)</span>
                <button 
                  onClick={copyInviteLink}
                  className="text-lg font-mono font-black text-slate-800 mt-1 hover:text-rose-500 transition-all flex items-center gap-2 border-b border-dashed border-slate-300"
                >
                  {roomState.roomId} 🔗
                </button>
              </div>

              <div className="bg-slate-50 border border-slate-100 p-4 rounded-2xl">
                <span className="text-slate-400 text-xs block font-medium">약속 장소 기준</span>
                <span className="text-lg font-bold text-slate-800 mt-1 block flex items-center gap-1.5">
                  📍 {roomState.location}
                </span>
              </div>
            </div>

            {/* Member List Grid */}
            <div className="mt-8">
              <h3 className="text-sm font-bold text-slate-600 flex items-center gap-1.5 mb-4">
                <Users className="w-4 h-4 text-slate-400" />
                참여 중인 친구들 ({roomState.members.length}명)
              </h3>
              
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {roomState.members.map((member) => {
                  const isHost = member.id === roomState.hostId;
                  const isMe = member.id === myMemberId;

                  return (
                    <div 
                      key={member.id}
                      className={`p-4 rounded-2xl border transition-all flex flex-col gap-1 relative overflow-hidden ${
                        isMe 
                          ? 'bg-rose-50/70 border-rose-200/50 shadow-sm shadow-rose-100' 
                          : 'bg-white border-slate-200/70'
                      }`}
                    >
                      {/* Host Tag */}
                      {isHost && (
                        <span className="absolute top-0 right-0 bg-orange-500 text-white text-[9px] font-black px-2 py-0.5 rounded-bl-lg">
                          방장
                        </span>
                      )}
                      
                      <span className="text-sm font-bold text-slate-800 block truncate pr-8">
                        {member.nickname} {isMe && "(나)"}
                      </span>
                      
                      <span className="text-[10px] font-semibold text-slate-400 flex items-center gap-1 mt-1">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                        대기 중
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Action Bar */}
            <div className="mt-8 pt-6 border-t border-slate-200/80 flex gap-4">
              {roomState.hostId === myMemberId ? (
                <button
                  onClick={handleStartGame}
                  disabled={loading}
                  className="flex-1 py-4 bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-600 hover:to-rose-600 text-white font-bold rounded-2xl shadow-lg shadow-rose-500/25 hover:shadow-rose-600/35 transition-all text-center flex items-center justify-center gap-2 group"
                >
                  {loading ? (
                    <RefreshCw className="w-5 h-5 animate-spin" />
                  ) : (
                    <>
                      게임 시작하기! (스와이프 작동)
                      <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                    </>
                  )}
                </button>
              ) : (
                <div className="flex-1 py-4 bg-slate-100 border border-slate-200 text-slate-500 text-center font-bold rounded-2xl flex items-center justify-center gap-2 animate-pulse text-sm">
                  <RefreshCw className="w-4 h-4 animate-spin text-slate-400" />
                  방장이 게임을 시작하기를 기다리는 중...
                </div>
              )}
            </div>
          </div>
        )}

        {/* ==================== 3. SWIPE PHASE ==================== */}
        {roomId && roomState && roomState.status === 'PLAYING' && (
          <div className="w-full max-w-md flex flex-col gap-6 items-center">
            
            {/* Top Info Bar */}
            <div className="w-full bg-white/80 backdrop-blur-md border border-slate-200/40 rounded-2xl p-4 shadow-md flex items-center justify-between">
              <div>
                <span className="text-[10px] font-bold text-rose-500 block uppercase tracking-wider">SWIPING</span>
                <span className="text-slate-800 text-sm font-black">먹고 싶은 메뉴를 카드로 결정</span>
              </div>
              <div className="bg-rose-100 text-rose-600 px-3 py-1.5 rounded-full text-xs font-extrabold flex items-center gap-1">
                🔥 {swipeCount} / 15 완료
              </div>
            </div>

            {/* Real-time Group Progress Bar */}
            <div className="w-full bg-white/80 backdrop-blur-md border border-slate-200/40 rounded-2xl p-4 shadow-md">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-slate-600 flex items-center gap-1">
                  <Users className="w-3.5 h-3.5 text-slate-400" />
                  실시간 그룹 투표 참여 현황
                </span>
                <span className="text-xs font-extrabold text-rose-600">
                  {roomState.completedMembersCount} / {roomState.totalMembers}명 완료
                </span>
              </div>
              <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden border border-slate-200/40">
                <div 
                  className="h-full bg-gradient-to-r from-rose-500 to-orange-400 transition-all duration-500"
                  style={{ width: `${(roomState.completedMembersCount / roomState.totalMembers) * 100}%` }}
                />
              </div>
              <p className="text-[10px] text-slate-400 mt-2 text-center">전원이 15장의 카드를 모두 스와이프하면 자동으로 매칭 결과 창이 열립니다.</p>
            </div>

            {/* Tinder Cards Stack Container */}
            <div className="relative w-full h-[400px] flex justify-center items-center">
              {swipeCount >= 15 ? (
                /* ALL SWIPED LOCAL WAITING BOARD */
                <div className="w-full h-full bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl rounded-3xl p-8 flex flex-col justify-center items-center text-center gap-4">
                  <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-500 shadow-md">
                    <CheckCircle2 className="w-8 h-8" />
                  </div>
                  <h3 className="text-xl font-black text-slate-800 mt-2">나의 투표 완료!</h3>
                  <p className="text-slate-500 text-sm max-w-xs leading-relaxed">
                    다른 친구들이 투표를 완료할 때까지 대기하고 있습니다. 실시간으로 화면이 자동 갱신됩니다.
                  </p>
                  <div className="flex items-center gap-2 bg-slate-800 text-white px-4 py-2 rounded-full text-xs font-bold mt-2 shadow-sm animate-pulse">
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    친구들 대기중 ({roomState.completedMembersCount} / {roomState.totalMembers}명 완료)
                  </div>
                </div>
              ) : (
                /* TINDER CARDS DECK */
                roomState.defaultMenus.map((menu, index) => {
                  // 이미 사용자가 스와이프한 인덱스는 숨김
                  if (index < swipeCount) return null;

                  const meta = MENU_METADATA[menu] || { emoji: "🍴", category: "음식", description: "맛있는 음식 카드를 밀어주세요!", gradient: "from-rose-500 to-orange-500" };

                  return (
                    <TinderCard
                      className="absolute w-full h-full cursor-grab active:cursor-grabbing"
                      key={menu}
                      onSwipe={(dir) => handleCardSwipe(dir, menu)}
                      preventSwipe={['up', 'down']}
                    >
                      <div className={`w-full h-full bg-gradient-to-br ${meta.gradient} rounded-3xl p-8 shadow-2xl flex flex-col justify-between text-white relative overflow-hidden`}>
                        {/* Background Overlay */}
                        <div className="absolute inset-0 bg-black/10 pointer-events-none" />
                        <div className="absolute top-[-30%] right-[-30%] w-[80%] h-[80%] rounded-full bg-white/10 blur-[80px] pointer-events-none" />

                        {/* Top Category Badge */}
                        <div className="z-10 flex justify-between items-center">
                          <span className="bg-white/20 backdrop-blur-md px-3 py-1 rounded-full text-xs font-bold border border-white/10 tracking-wide">
                            {meta.category}
                          </span>
                          <span className="text-white/40 text-xs font-black tracking-widest font-mono">
                            {index + 1} / 15
                          </span>
                        </div>

                        {/* Middle Emoji & Name */}
                        <div className="z-10 text-center my-6 flex flex-col items-center gap-4">
                          <span className="text-7xl block animate-bounce" style={{ animationDuration: '3s' }}>
                            {meta.emoji}
                          </span>
                          <h3 className="text-4xl font-black tracking-tight drop-shadow-md">
                            {menu}
                          </h3>
                        </div>

                        {/* Bottom Description */}
                        <div className="z-10 bg-white/10 backdrop-blur-md border border-white/10 p-4 rounded-2xl">
                          <p className="text-xs leading-relaxed text-white/90 font-medium">
                            {meta.description}
                          </p>
                        </div>

                        {/* Swipe Direction Helper */}
                        <div className="z-10 flex justify-between items-center mt-2 px-2 text-[10px] font-bold text-white/60">
                          <span>👈 싫어요 (Left)</span>
                          <span>좋아요 (Right) 👉</span>
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
          <div className="w-full max-w-4xl grid md:grid-cols-12 gap-8 items-start">
            
            {/* Left Col: Menu Matching Banner */}
            <div className="md:col-span-5 bg-white/80 backdrop-blur-xl border border-white/60 shadow-2xl rounded-3xl p-8 text-center flex flex-col items-center gap-6 relative overflow-hidden">
              <div className="absolute top-0 inset-x-0 h-2 bg-gradient-to-r from-rose-500 via-orange-500 to-amber-500" />
              
              <div className="inline-flex items-center gap-1 bg-amber-100 text-amber-600 text-xs font-black px-3 py-1 rounded-full border border-amber-200/50 shadow-sm shadow-amber-100/50">
                <Sparkles className="w-3.5 h-3.5" /> 최종 매칭 성공
              </div>

              <div>
                <span className="text-slate-400 text-xs block font-bold">우리 파티가 픽한 1위 메뉴는?</span>
                <span className="text-8xl block mt-4 animate-bounce" style={{ animationDuration: '4s' }}>
                  {MENU_METADATA[roomState.winningMenu || ""]?.emoji || "🍴"}
                </span>
                <h2 className="text-4xl sm:text-5xl font-black text-slate-800 tracking-tight mt-4 drop-shadow-sm bg-gradient-to-r from-rose-600 to-amber-500 bg-clip-text text-transparent">
                  {roomState.winningMenu}
                </h2>
              </div>

              <div className="bg-slate-50 border border-slate-100 p-4 rounded-2xl w-full text-left text-xs text-slate-500 leading-relaxed font-semibold flex items-start gap-2">
                <Info className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                <span>
                  모든 투표 기록을 F-401(점수) 및 F-402(거부권), F-403(구제책) 필터링 매칭 알고리즘으로 분석하여 도출된 타협 메뉴입니다.
                </span>
              </div>

              <button
                onClick={resetSession}
                className="w-full py-3.5 bg-slate-800 hover:bg-slate-900 text-white font-bold rounded-xl text-sm transition-all shadow-md flex items-center justify-center gap-2"
              >
                새로운 투표 시작하기
              </button>
            </div>

            {/* Right Col: Restaurant recommendations dashboard with mock map */}
            <div className="md:col-span-7 flex flex-col gap-6">
              
              {/* Mock Map Container */}
              <div className="w-full bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl rounded-3xl p-6 relative overflow-hidden">
                <h3 className="text-sm font-bold text-slate-700 mb-4 flex items-center gap-1.5">
                  <Compass className="w-4 h-4 text-rose-500" />
                  📍 {roomState.location} 맛집 매칭 지도
                </h3>
                
                {/* Simulated Modern Interactive Map */}
                <div className="w-full h-[220px] bg-slate-100 rounded-2xl border border-slate-200/60 relative overflow-hidden shadow-inner flex items-center justify-center">
                  {/* Grid Lines Pattern */}
                  <div className="absolute inset-0 bg-[linear-gradient(to_right,#e2e8f0_1px,transparent_1px),linear-gradient(to_bottom,#e2e8f0_1px,transparent_1px)] bg-[size:24px_24px] opacity-40" />
                  
                  {/* Map Roads Simulation */}
                  <div className="absolute top-[30%] left-0 w-full h-4 bg-white border-y border-slate-200/50 rotate-[3deg] pointer-events-none" />
                  <div className="absolute top-[70%] left-0 w-full h-6 bg-white border-y border-slate-200/50 rotate-[-2deg] pointer-events-none" />
                  <div className="absolute left-[40%] top-0 w-5 h-full bg-white border-x border-slate-200/50 rotate-[12deg] pointer-events-none" />

                  {/* Area Label */}
                  <div className="absolute top-4 left-4 bg-white/80 border border-slate-200/50 px-2 py-1 rounded-lg text-[10px] font-bold text-slate-500 shadow-sm">
                    {roomState.location} 반경 1km
                  </div>

                  {/* Winning Restaurant Pins (Mock coordinates mapping) */}
                  {roomState.matchedRestaurants.map((res, index) => {
                    const pinOffsets = [
                      { top: '40%', left: '30%' },
                      { top: '65%', left: '60%' },
                      { top: '25%', left: '50%' },
                      { top: '80%', left: '20%' },
                      { top: '50%', left: '80%' }
                    ];
                    const offset = pinOffsets[index] || { top: '50%', left: '50%' };

                    return (
                      <div 
                        key={res.id}
                        className="absolute cursor-pointer group flex flex-col items-center"
                        style={{ top: offset.top, left: offset.left }}
                      >
                        <div className="w-6 h-6 rounded-full bg-rose-500 text-white font-extrabold text-[10px] flex items-center justify-center border-2 border-white shadow-lg hover:scale-125 transition-all animate-bounce" style={{ animationDuration: `${2 + index * 0.5}s` }}>
                          {index + 1}
                        </div>
                        {/* Tooltip on hover */}
                        <div className="absolute bottom-8 scale-0 group-hover:scale-100 bg-slate-800 text-white text-[9px] font-bold px-2 py-1 rounded shadow-lg whitespace-nowrap transition-all z-20">
                          {res.name}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Restaurant List Feed */}
              <div className="flex flex-col gap-3">
                <h3 className="text-sm font-bold text-slate-700 flex items-center gap-1.5">
                  <Compass className="w-4 h-4 text-rose-500" />
                  추천 연동 주변 5대 맛집 목록
                </h3>
                
                {roomState.matchedRestaurants.map((restaurant, index) => (
                  <div 
                    key={restaurant.id}
                    className="bg-white border border-slate-200/70 hover:border-rose-300 rounded-2xl p-4 shadow-sm hover:shadow-md transition-all flex items-center gap-4 relative overflow-hidden group"
                  >
                    {/* Index Circle */}
                    <div className="w-10 h-10 rounded-xl bg-rose-100 text-rose-600 font-extrabold text-sm flex items-center justify-center shrink-0 border border-rose-200/30">
                      {index + 1}
                    </div>

                    {/* Restaurant Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-slate-800 truncate text-sm sm:text-base group-hover:text-rose-600 transition-colors">
                          {restaurant.name}
                        </h4>
                        <span className="bg-amber-50 text-amber-600 text-[10px] font-extrabold px-2 py-0.5 rounded-full flex items-center gap-0.5 shrink-0">
                          <Star className="w-3 h-3 fill-amber-500 text-amber-500" /> 4.8
                        </span>
                      </div>
                      
                      <span className="text-slate-500 text-xs mt-1 block truncate">
                        📍 {restaurant.address}
                      </span>
                    </div>

                    {/* Contact Button */}
                    {restaurant.phone && (
                      <a 
                        href={`tel:${restaurant.phone}`}
                        className="w-10 h-10 rounded-xl bg-slate-50 hover:bg-rose-50 text-slate-400 hover:text-rose-600 border border-slate-200/60 hover:border-rose-200 flex items-center justify-center transition-all shrink-0 shadow-sm"
                        title={`전화문의: ${restaurant.phone}`}
                      >
                        <Phone className="w-4 h-4" />
                      </a>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

      </main>

      {/* Footer */}
      <footer className="w-full text-center py-5 text-xs text-slate-500 font-medium border-t border-slate-200/40 bg-white/20 backdrop-blur-sm z-10">
        © 2026 오늘 뭐 먹지? Project. Built with ⚡ Java Spring Boot & React.
      </footer>
    </div>
  );
}

export default App;
