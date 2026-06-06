import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Flame, Compass, Users, Sparkles, MapPin, ArrowRight, CheckCircle2, RefreshCw, Phone, Info, AlertCircle, Copy, X, Settings, Star, MessageSquare } from 'lucide-react';
import TinderCard from 'react-tinder-card';
import { useWebSocket, WebSocketRoomResponse } from './hooks/useWebSocket';
import { KakaoMap } from './components/KakaoMap';
const brandLogo = new URL('./amugeona_buster_logo.png', import.meta.url).href;
const meal3dIcon = new URL('./meal_3d_icon.png', import.meta.url).href;
const bell3dIcon = new URL('./bell_3d_icon.png', import.meta.url).href;
const coffee3dIcon = new URL('./coffee_3d_icon.png', import.meta.url).href;

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

const PRESETS = [
  { name: "삼성 DSR 타워 웰스토리", cotNo: "WEL_DSR", hallNo: "HALL_01" },
  { name: "삼성전자 수원디지털시티 R5", cotNo: "WEL_SUWON", hallNo: "HALL_02" },
  { name: "삼성전자 기흥캠퍼스 MR1", cotNo: "WEL_GIHEUNG", hallNo: "HALL_03" },
  { name: "삼성전자 화성캠퍼스 D1", cotNo: "WEL_HWASEONG", hallNo: "HALL_04" },
  { name: "삼성전자 서초사옥 웰스토리", cotNo: "WEL_SEOCHO", hallNo: "HALL_05" },
  { name: "삼성웰스토리 본사 식당", cotNo: "WEL_HQ", hallNo: "HALL_06" }
];

const isLocalDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const BASE_URL = isLocalDev
  ? `http://localhost:8080/api/rooms`
  : `${window.location.origin}/api/rooms`;

// 구내식당 코스별 스타일 매핑 헬퍼
const getCourseStyle = (courseName: string) => {
  const name = (courseName || '').toUpperCase();
  if (name.includes('A코스') || name.includes('한식') || name.includes('KOREAN') || name.includes('소담') || name.includes('찌개') || name.includes('탕')) {
    return {
      badgeGradient: 'from-orange-500 to-amber-500',
      courseEmoji: '🍚'
    };
  } else if (name.includes('B코스') || name.includes('일식') || name.includes('JAPANESE') || name.includes('돈카츠') || name.includes('카츠') || name.includes('초밥')) {
    return {
      badgeGradient: 'from-rose-500 to-orange-500',
      courseEmoji: '🍣'
    };
  } else if (name.includes('C코스') || name.includes('양식') || name.includes('WESTERN') || name.includes('파스타') || name.includes('스테이크') || name.includes('피자')) {
    return {
      badgeGradient: 'from-amber-500 to-yellow-500',
      courseEmoji: '🍝'
    };
  } else if (name.includes('아시안') || name.includes('중식') || name.includes('CHINESE') || name.includes('라멘') || name.includes('짜장') || name.includes('짬뽕') || name.includes('마라')) {
    return {
      badgeGradient: 'from-teal-500 to-emerald-500',
      courseEmoji: '🍜'
    };
  } else if (name.includes('테이크아웃') || name.includes('TAKEOUT') || name.includes('도시락') || name.includes('샌드위치') || name.includes('베이글') || name.includes('샐러드') || name.includes('빵')) {
    return {
      badgeGradient: 'from-emerald-500 to-teal-600',
      courseEmoji: '🥪'
    };
  }
  return {
    badgeGradient: 'from-orange-500 to-amber-500',
    courseEmoji: '🍴'
  };
};

// 구내식당 메뉴 카테고리별 프리미엄 Unsplash 이미지 매핑 헬퍼 (하드코딩 방지 및 비주얼 완성)
const getCourseImage = (courseName: string, apiImageUrl?: string) => {
  if (apiImageUrl && apiImageUrl.startsWith('http')) {
    return apiImageUrl;
  }
  const name = (courseName || '').toUpperCase();
  if (name.includes('한식') || name.includes('소담') || name.includes('찌개') || name.includes('탕') || name.includes('국밥')) {
    return 'https://images.unsplash.com/photo-1569562211093-4ed0d0758f12?auto=format&fit=crop&w=600&q=80';
  } else if (name.includes('일식') || name.includes('돈카츠') || name.includes('카츠') || name.includes('초밥') || name.includes('라멘')) {
    return 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=600&q=80';
  } else if (name.includes('양식') || name.includes('파스타') || name.includes('스테이크') || name.includes('피자') || name.includes('샐러드')) {
    return 'https://images.unsplash.com/photo-1546549032-9571cd6b27df?auto=format&fit=crop&w=600&q=80';
  } else if (name.includes('중식') || name.includes('짜장') || name.includes('짬뽕') || name.includes('마라')) {
    return 'https://images.unsplash.com/photo-1525755662778-989d0524087e?auto=format&fit=crop&w=600&q=80';
  } else if (name.includes('테이크아웃') || name.includes('도시락') || name.includes('샌드위치') || name.includes('베이글') || name.includes('빵')) {
    return 'https://images.unsplash.com/photo-1509722747041-616f39b57569?auto=format&fit=crop&w=600&q=80';
  }
  return 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80';
};

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
  const [copied, setCopied] = useState(false);

  // 🍱 웰스토리 알림 설정 관련 상태 변수
  const [isWelstoryModalOpen, setIsWelstoryModalOpen] = useState(false);
  const [welstorySettings, setWelstorySettings] = useState<any>(null);



  // 🍱 웰스토리 알림 설정 전용 폼 상태 변수
  const [selectedPreset, setSelectedPreset] = useState<number | 'custom'>(0);
  const [customCotNo, setCustomCotNo] = useState('');
  const [customHallNo, setCustomHallNo] = useState('');
  const [customCafeteriaName, setCustomCafeteriaName] = useState('');
  const [activeDays, setActiveDays] = useState<number[]>([1, 2, 3, 4, 5]); // 월~금 기본값
  const [timeStr, setTimeStr] = useState('11:30');
  const [isAlertEnabled, setIsAlertEnabled] = useState(true);

  const [testResult, setTestResult] = useState<string | null>(null);
  const [testSuccess, setTestSuccess] = useState<boolean | null>(null);
  const [saveLoading, setSaveLoading] = useState(false);
  const [testLoading, setTestLoading] = useState(false);
  
  const isEscapingRef = useRef(false);

  // 🍱 웰스토리 오늘 전체 메뉴판 뷰어 관련 상태 변수
  const [isMenuDetailOpen, setIsMenuDetailOpen] = useState(false);
  const [menuDetailCafeteriaName, setMenuDetailCafeteriaName] = useState('');
  const [menuDetailDate, setMenuDetailDate] = useState('');
  const [menuDetailCourses, setMenuDetailCourses] = useState<any[]>([]);
  const [menuDetailLoading, setMenuDetailLoading] = useState(false);
  const [menuDetailError, setMenuDetailError] = useState<string | null>(null);
  const [menuDetailCotNo, setMenuDetailCotNo] = useState('');
  const [menuDetailHallNo, setMenuDetailHallNo] = useState('');
  const [menuMealFilter, setMenuMealFilter] = useState<'all' | 'breakfast' | 'lunch' | 'dinner'>('all');

  // ⭐ 웰스토리 식단별 별점 및 한줄평 후기 관련 상태 변수
  const [reviewStats, setReviewStats] = useState<Record<string, { averageRating: number; reviewCount: number }>>({});
  const [expandedReviewCourseName, setExpandedReviewCourseName] = useState<string | null>(null);
  const [courseReviews, setCourseReviews] = useState<any[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);
  const [activeReviewWriteCourseName, setActiveReviewWriteCourseName] = useState<string | null>(null);
  const [newReviewRating, setNewReviewRating] = useState(5);
  const [newReviewComment, setNewReviewComment] = useState('');
  const [newReviewNickname, setNewReviewNickname] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [submittedReviewCourses, setSubmittedReviewCourses] = useState<Record<string, boolean>>({});
  const [isReviewInputFocused, setIsReviewInputFocused] = useState(false);

  // 📱 PWA 설치 관련 상태 변수 및 로직
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showInstallBanner, setShowInstallBanner] = useState(false);
  const [isIOS, setIsIOS] = useState(false);

  useEffect(() => {
    // 1. 이미 앱 형태로 설치되어 실행 중인지 확인
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches 
      || (window.navigator as any).standalone 
      || document.referrer.includes('android-app://');
    
    if (isStandalone) {
      return;
    }

    // 2. iOS 기기 여부 판별
    const userAgent = window.navigator.userAgent.toLowerCase();
    const ios = /iphone|ipad|ipod/.test(userAgent);
    setIsIOS(ios);

    // 3. 안드로이드 / 크롬 브라우저의 설치 프롬프트 대기
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallBanner(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    // 4. iOS의 경우 2초 뒤 가이드 노출 (하루 1회만 제안하여 도배성 UX 방지)
    if (ios) {
      const timer = setTimeout(() => {
        const dismissed = localStorage.getItem('pwa_dismissed_date');
        const today = new Date().toISOString().split('T')[0];
        if (dismissed !== today) {
          setShowInstallBanner(true);
        }
      }, 2000);
      return () => clearTimeout(timer);
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    console.log(`User response to the install prompt: ${outcome}`);
    setDeferredPrompt(null);
    setShowInstallBanner(false);
  };

  const handleDismissInstall = () => {
    setShowInstallBanner(false);
    const today = new Date().toISOString().split('T')[0];
    localStorage.setItem('pwa_dismissed_date', today);
  };

  // 🎮 커피빵 내기 미니게임 관련 상태 변수
  const [isCoffeeGameOpen, setIsCoffeeGameOpen] = useState(false);
  const [playerCount, setPlayerCount] = useState(4);
  const [cupStates, setCupStates] = useState<{ flipped: boolean; isSalt: boolean }[]>([]);
  const [gameStatus, setGameStatus] = useState<'ready' | 'playing' | 'gameover'>('ready');
  const [looserName, setLooserName] = useState('');
  const [showReceipt, setShowReceipt] = useState(false);

  // 커피 내기 게임 컵 랜덤 셔플 초기화
  const initCoffeeGame = (count: number) => {
    const saltIdx = Math.floor(Math.random() * count);
    const initialCups = Array.from({ length: count }, (_, idx) => ({
      flipped: false,
      isSalt: idx === saltIdx
    }));
    setCupStates(initialCups);
    setGameStatus('playing');
    setLooserName('');
    setShowReceipt(false);
    setIsCoffeeGameOpen(true);
  };

  // 사운드 재생 헬퍼
  const playAudio = (url: string) => {
    try {
      const audio = new Audio(url);
      audio.volume = 0.4;
      audio.play();
    } catch (err) {
      console.warn("Audio play blocked by browser policy:", err);
    }
  };

  // 컵 터치 이벤트 처리
  const handleCupClick = (idx: number) => {
    if (gameStatus !== 'playing' || cupStates[idx].flipped) return;

    const nextCups = [...cupStates];
    nextCups[idx].flipped = true;
    setCupStates(nextCups);

    if (nextCups[idx].isSalt) {
      setGameStatus('gameover');
      // 꽝 효과음
      playAudio('https://assets.mixkit.co/active_storage/sfx/2869/2869-200.wav');
      // 모바일 기기 진동 API 연동 (vibrate)
      if (navigator.vibrate) {
        navigator.vibrate([200, 100, 200, 100, 300]);
      }
    } else {
      // 커피 드립 효과음
      playAudio('https://assets.mixkit.co/active_storage/sfx/2452/2452-200.wav');
    }
  };

  // 카카오톡 단톡방 골든벨 박제 공유 처리
  const handleKakaoShareReceipt = () => {
    const shareText = `[🚨 커피 골든벨 속보]\n오늘의 썩은 소금 아메리카노 당첨자는 바로 [${looserName}]님입니다! 🔔💸\n\n품명: 소금 아메리카노 1잔 (시가 ₩55,000)\n\n"오늘 커피는 ${looserName}님이 시원하게 쏘십니다! 다들 감사한 마음으로 카페로 집결하세요! 😍☕"\n\n👉 지금 나도 내기 참여하기: ${window.location.origin}/?view-menu=true&cotNo=${menuDetailCotNo}&hallNo=${menuDetailHallNo}&name=${encodeURIComponent(menuDetailCafeteriaName)}`;

    const k = (window as any).Kakao;
    if (k && k.isInitialized && k.isInitialized()) {
      try {
        k.Share.sendDefault({
          objectType: 'feed',
          content: {
            title: '🚨 커피 골든벨 당첨 안내',
            description: `오늘의 커피빵 주인공은 [${looserName}]님입니다! 🔔💸`,
            imageUrl: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500',
            link: {
              mobileWebUrl: window.location.href,
              webUrl: window.location.href,
            },
          },
          buttons: [
            {
              title: '나도 내기 참여하기 🎮',
              link: {
                mobileWebUrl: window.location.href,
                webUrl: window.location.href,
              },
            },
          ],
        });
        alert('카카오톡으로 골든벨 소식이 전송되었습니다! 📢');
        return;
      } catch (err) {
        console.error('Kakao share default failed, falling back to clipboard:', err);
      }
    }

    // 폴백: 클립보드 복사
    navigator.clipboard.writeText(shareText).then(() => {
      alert('📢 골든벨 당첨 명단과 복불복 영수증 텍스트가 클립보드에 복사되었습니다! 카톡 단톡방에 붙여넣기(Ctrl+V)해서 당첨자를 박제하세요! 💸');
    }).catch(err => {
      console.error('Clipboard copy failed:', err);
      alert(`[결과] 오늘 커피 쏠 사람: ${looserName}님!`);
    });
  };

  // 실시간 식단 상세 조회 API 핸들러
  const triggerFetchMenuDetails = async (cotNo: string, hallNo: string, name: string) => {
    setMenuMealFilter('all');
    setMenuDetailLoading(true);
    setMenuDetailError(null);
    setIsMenuDetailOpen(true);
    setMenuDetailCafeteriaName(name);
    setMenuDetailCotNo(cotNo);
    setMenuDetailHallNo(hallNo);
    try {
      const response = await fetch(`${window.location.origin}/api/welstory/menu-details?cotNo=${cotNo}&hallNo=${hallNo}&cafeteriaName=${encodeURIComponent(name)}`);
      if (!response.ok) {
        throw new Error('오늘의 식단 리스트를 실시간으로 가져오는 데 실패했습니다.');
      }
      const data = await response.json();
      setMenuDetailCafeteriaName(data.cafeteriaName || name);
      setMenuDetailDate(data.dateStr || '오늘');
      setMenuDetailCourses(data.courses || []);
      // 💬 식단 조회 성공 시 해당 날짜/지점의 별점 평점 현황도 같이 로드!
      fetchReviewStats(data.cafeteriaName || name, data.dateStr || '오늘');
    } catch (err: any) {
      setMenuDetailError(err.message || '식단 정보 수집 도중 예상치 못한 오류가 발생했습니다.');
    } finally {
      setMenuDetailLoading(false);
    }
  };

  // 📅 날짜 문자열을 YYYY-MM-DD ISO 형식으로 표준화하는 헬퍼 함수 (한글 날짜 헤더 등 예외 대응)
  const getIsoDateString = (date: string) => {
    if (date && date.includes('-') && date.length === 10) {
      return date;
    }
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // ⭐ 웰스토리 지점/날짜별 누적 평점 통계 획득 API 연동
  const fetchReviewStats = async (cafeteriaName: string, date: string) => {
    try {
      const formattedDate = getIsoDateString(date);
      const res = await fetch(`/api/welstory/reviews/stats?cafeteriaName=${encodeURIComponent(cafeteriaName)}&menuDate=${formattedDate}`);
      if (res.ok) {
        const stats = await res.json();
        setReviewStats(stats || {});
      }
    } catch (e) {
      console.error('Failed to load review stats', e);
    }
  };

  // 💬 특정 식단의 한줄평 목록 로드 API 연동
  const fetchCourseReviews = async (cafeteriaName: string, date: string) => {
    setLoadingReviews(true);
    try {
      const formattedDate = getIsoDateString(date);
      const res = await fetch(`/api/welstory/reviews?cafeteriaName=${encodeURIComponent(cafeteriaName)}&menuDate=${formattedDate}`);
      if (res.ok) {
        const data = await res.json();
        setCourseReviews(data || []);
      }
    } catch (e) {
      console.error('Failed to load reviews', e);
    } finally {
      setLoadingReviews(false);
    }
  };

  // 한줄평 작성 창 열기 핸들러
  const handleOpenReviewWrite = (courseName: string) => {
    setActiveReviewWriteCourseName(courseName);
    // 닉네임 기본 세팅 (카카오 가입자 정보가 있으면 그것을, 없으면 귀여운 사내 닉네임 임시 추천)
    const kakaoNickname = welstorySettings?.nickname;
    if (kakaoNickname) {
      setNewReviewNickname(kakaoNickname);
    } else {
      const adjectives = ['배고픈', '든든한', '행복한', '바쁜', '신선한', '피곤한', '열정적인'];
      const nouns = ['사우', '프로', '디자이너', '엔지니어', '웰스토리러', '동료'];
      const randAdj = adjectives[Math.floor(Math.random() * adjectives.length)];
      const randNoun = nouns[Math.floor(Math.random() * nouns.length)];
      const randNum = Math.floor(Math.random() * 900) + 100;
      setNewReviewNickname(`${randAdj} ${randNoun} ${randNum}`);
    }
    setNewReviewRating(5);
    setNewReviewComment('');
  };

  // 🚀 한줄평 등록 제출 API 연동
  const handleReviewSubmit = async (courseName: string, menuDetails: string) => {
    if (!newReviewComment.trim()) {
      alert('한줄평 내용을 입력해 주세요.');
      return;
    }
    setSubmittingReview(true);
    const formattedDate = getIsoDateString(menuDetailDate);
    try {
      let fingerprint = localStorage.getItem('user_fingerprint');
      if (!fingerprint) {
        fingerprint = 'fingerprint-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now();
        localStorage.setItem('user_fingerprint', fingerprint);
      }

      const nicknameToUse = newReviewNickname.trim() || '익명의 사우';

      const payload = {
        cafeteriaName: menuDetailCafeteriaName,
        menuDate: formattedDate,
        courseName: courseName,
        menuDetails: menuDetails,
        nickname: nicknameToUse,
        rating: newReviewRating,
        comment: newReviewComment.trim(),
        userFingerprint: fingerprint
      };

      const res = await fetch('/api/welstory/reviews', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || '후기 제출에 실패했습니다.');
      }

      // 로컬 중복 제출 체크 등록
      setSubmittedReviewCourses(prev => ({
        ...prev,
        [`${menuDetailCafeteriaName}-${formattedDate}-${courseName}`]: true
      }));

      // 작성 폼 닫기 및 필드 초기화
      setNewReviewComment('');
      setActiveReviewWriteCourseName(null);
      setIsReviewInputFocused(false);

      // 통계와 리스트 즉각 실시간 새로고침!
      await fetchReviewStats(menuDetailCafeteriaName, menuDetailDate);
      await fetchCourseReviews(menuDetailCafeteriaName, menuDetailDate);

      alert('한줄평 후기가 성공적으로 등록되었습니다! 🎉');
    } catch (e: any) {
      alert(e.message || '후기 등록에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setSubmittingReview(false);
    }
  };

  // 🛡️ 모바일 인앱 브라우저(카카오톡 등) 및 iOS Safari 배경 스크롤 누수 완전 차단 (Scroll Position Memory Lock)
  useEffect(() => {
    const isAnyModalOpen = isWelstoryModalOpen || isMenuDetailOpen || isCoffeeGameOpen;
    
    if (isAnyModalOpen) {
      const scrollY = window.scrollY;
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
      document.body.dataset.scrollY = scrollY.toString();
    } else {
      const savedScrollY = document.body.dataset.scrollY;
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.width = '';
      document.body.style.overflow = '';
      delete document.body.dataset.scrollY;
      
      if (savedScrollY) {
        window.scrollTo(0, parseInt(savedScrollY, 10));
      }
    }
    
    return () => {
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.width = '';
      document.body.style.overflow = '';
      delete document.body.dataset.scrollY;
    };
  }, [isWelstoryModalOpen, isMenuDetailOpen, isCoffeeGameOpen]);

  // URL 파라미터 감지 및 자동 기동 효과
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const roomCode = urlParams.get('room');
    const escape = urlParams.get('escape');
    const escapeLocation = urlParams.get('location');
    const welstoryParam = urlParams.get('welstory');
    const code = urlParams.get('code');
    const viewMenuParam = urlParams.get('view-menu');
    const viewCotNo = urlParams.get('cotNo');
    const viewHallNo = urlParams.get('hallNo');
    const viewName = urlParams.get('name');

    // 0. 웰스토리 오늘 전체 메뉴판 뷰어 자동 기동 (?view-menu=true&cotNo=...&hallNo=...)
    if (viewMenuParam === 'true' && viewCotNo && viewHallNo) {
      const decodedName = viewName ? decodeURIComponent(viewName) : '삼성웰스토리';
      triggerFetchMenuDetails(viewCotNo, viewHallNo, decodedName);
      
      // URL 파라미터 정리
      const cleanUrl = window.location.origin + window.location.pathname;
      window.history.replaceState({}, document.title, cleanUrl);
      return;
    }

    // 1. 구식 탈출 자동 매칭 (?escape=true&location=...)
    if (escape === 'true' && escapeLocation && !isEscapingRef.current) {
      isEscapingRef.current = true;
      const decodedLoc = decodeURIComponent(escapeLocation);
      setLocation(decodedLoc);
      setNickname('구식탈출러');
      
      // URL 파라미터 정리
      const cleanUrl = window.location.origin + window.location.pathname;
      window.history.replaceState({}, document.title, cleanUrl);
      
      autoTriggerEscapeFlow(decodedLoc);
      return;
    }

    // 2. 카카오 로그인 리다이렉트 처리 (?code=...)
    if (code) {
      setLoading(true);
      const cleanUrl = window.location.origin + window.location.pathname;
      window.history.replaceState({}, document.title, cleanUrl);

      fetch(`${window.location.origin}/api/welstory/token-exchange`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, redirectUri: window.location.origin })
      })
        .then(async (res) => {
          if (res.ok) {
            const data = await res.json();
            localStorage.setItem('welstory_kakao_id', data.kakaoId);
            setWelstorySettings(data);
            setIsWelstoryModalOpen(true); // 설정창 자동 열기
          } else {
            throw new Error('카카오 토큰 교환 실패');
          }
        })
        .catch(err => {
          console.error(err);
          alert('카카오 인증 연동에 실패했습니다. 다시 시도해 주세요.');
        })
        .finally(() => {
          setLoading(false);
        });
      return;
    }

    // 3. 알림 수동 변경 파라미터 감지 (?welstory=true)
    if (welstoryParam === 'true') {
      setIsWelstoryModalOpen(true);
      // URL 정리
      const cleanUrl = window.location.origin + window.location.pathname;
      window.history.replaceState({}, document.title, cleanUrl);
    }

    // 4. 기존 로드 (초대코드)
    if (roomCode) {
      const formattedCode = roomCode.trim().toUpperCase();
      setRoomCodeInput(formattedCode);
      setIsJoinView(true);

      setLoading(true);
      fetch(`${BASE_URL}/${formattedCode}`)
        .then(async (res) => {
          if (res.ok) {
            const data = await res.json();
            console.log('📬 Loaded shared room state on mount:', data);
            if (data.status === 'COMPLETED') {
              setRoomId(data.roomId);
              setRoomState(data);
            }
          }
        })
        .catch(err => {
          console.error('Failed to pre-fetch room state:', err);
        })
        .finally(() => {
          setLoading(false);
        });
    }

    // 5. 이미 로그인된 웰스토리 카카오 세션이 있으면 설정 백그라운드 선형 프리페치
    const savedKakaoId = localStorage.getItem('welstory_kakao_id');
    if (savedKakaoId) {
      fetch(`${window.location.origin}/api/welstory/settings?kakaoId=${savedKakaoId}`)
        .then(async (res) => {
          if (res.ok) {
            const data = await res.json();
            setWelstorySettings(data);
          }
        })
        .catch(err => {
          console.error('Failed to pre-fetch welstory settings:', err);
        });
    }
  }, []);

  // 구내식당 탈출 자동 연동 플로우 (방생성 -> 게임시작 -> 전원 자동 스와이프 완료 -> 결과 로딩)
  const autoTriggerEscapeFlow = async (loc: string) => {
    setLoading(true);
    setErrorMessage(null);
    try {
      // 1. 방 개설 API
      const createResponse = await fetch(BASE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          hostNickname: '구식탈출고양이',
          location: loc,
          customMenus: Object.keys(MENU_METADATA)
        })
      });

      if (!createResponse.ok) {
        throw new Error('구내식당 탈출 방 생성을 완료하지 못했습니다.');
      }

      const roomData = await createResponse.json();
      const createdRoomId = roomData.roomId;
      const hostId = roomData.hostId;

      setRoomId(createdRoomId);
      setMyMemberId(hostId);
      setRoomState(roomData);

      // 2. 대기실 게임 시작
      const startResponse = await fetch(`${BASE_URL}/${createdRoomId}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ hostId: hostId })
      });

      if (!startResponse.ok) {
        throw new Error('구내식당 탈출 투표 기동에 실패했습니다.');
      }

      const startedData = await startResponse.json();
      setRoomState(startedData);

      // 3. 모든 기본 메뉴 좋아요 자동 스와이프 전송 (즉시 완료 마크)
      const menus = roomData.defaultMenus;
      for (const menuName of menus) {
        await fetch(`${BASE_URL}/${createdRoomId}/swipes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            memberId: hostId,
            menuName: menuName,
            isLike: true
          })
        });
      }

      // 4. 최종 완성 데이터 동기화 조회
      const finalResponse = await fetch(`${BASE_URL}/${createdRoomId}`);
      if (finalResponse.ok) {
        const finalData = await finalResponse.json();
        setRoomState(finalData);
      }
    } catch (err: any) {
      setErrorMessage(err.message || '구내식당 탈출 도중 예상치 못한 통신 오류가 생겼습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 카카오 계정 연동 개시 (OAuth 리다이렉트 주소 획득)
  const handleKakaoLink = async () => {
    try {
      const response = await fetch(`${window.location.origin}/api/welstory/auth-url?redirectUri=${window.location.origin}`);
      if (response.ok) {
        const data = await response.json();
        window.location.href = data.url;
      } else {
        alert('카카오 인증 게이트웨이 호출 실패');
      }
    } catch (err) {
      console.error(err);
      alert('서버 게이트웨이 통신 실패');
    }
  };

  // 🍱 모달이 켜지거나 설정 정보가 세팅될 때 폼 값 초기화
  useEffect(() => {
    if (isWelstoryModalOpen && welstorySettings) {
      const presetIdx = PRESETS.findIndex(p => p.cotNo === welstorySettings.cotNo && p.hallNo === welstorySettings.hallNo);
      if (presetIdx !== -1) {
        setSelectedPreset(presetIdx);
      } else {
        setSelectedPreset('custom');
        setCustomCotNo(welstorySettings.cotNo || '');
        setCustomHallNo(welstorySettings.hallNo || '');
        setCustomCafeteriaName(welstorySettings.cafeteriaName || '');
      }

      if (welstorySettings.scheduledDays) {
        const days = welstorySettings.scheduledDays.split(',').map((d: string) => parseInt(d, 10));
        setActiveDays(days);
      }
      
      setTimeStr(welstorySettings.scheduledTime || '11:30');
      setIsAlertEnabled(welstorySettings.enabled !== undefined ? welstorySettings.enabled : true);
      setTestResult(null);
      setTestSuccess(null);
    }
  }, [isWelstoryModalOpen, welstorySettings]);

  // 🍱 알림 설정 저장 처리
  const handleSaveSettings = async () => {
    if (!welstorySettings?.kakaoId) return;
    setSaveLoading(true);
    setTestResult(null);

    let finalCotNo = '';
    let finalHallNo = '';
    let finalName = '';

    if (selectedPreset === 'custom') {
      finalCotNo = customCotNo.trim();
      finalHallNo = customHallNo.trim();
      finalName = customCafeteriaName.trim() || '사내식당';
    } else {
      const preset = PRESETS[selectedPreset as number];
      finalCotNo = preset.cotNo;
      finalHallNo = preset.hallNo;
      finalName = preset.name;
    }

    if (!finalCotNo || !finalHallNo) {
      alert('구내식당 회사 및 식당 코드를 입력해 주세요.');
      setSaveLoading(false);
      return;
    }

    const payload = {
      kakaoId: welstorySettings.kakaoId,
      nickname: welstorySettings.nickname || '사용자',
      kakaoAccessToken: welstorySettings.kakaoAccessToken,
      kakaoRefreshToken: welstorySettings.kakaoRefreshToken,
      cotNo: finalCotNo,
      hallNo: finalHallNo,
      cafeteriaName: finalName,
      scheduledDays: activeDays.sort().join(','),
      scheduledTime: timeStr,
      isEnabled: isAlertEnabled
    };

    try {
      const response = await fetch(`${window.location.origin}/api/welstory/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        const saved = await response.json();
        setWelstorySettings(saved);
        alert('🎉 알림 설정이 안전하게 저장되었습니다!');
        setIsWelstoryModalOpen(false);
      } else {
        alert('알림 설정 저장에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      alert('서버와 통신하는 중 오류가 생겼습니다.');
    } finally {
      setSaveLoading(false);
    }
  };

  // 🍱 테스트 즉시 발송
  const handleTestSend = async () => {
    if (!welstorySettings?.kakaoId) return;
    setTestLoading(true);
    setTestResult(null);
    setTestSuccess(null);

    let finalCotNo = '';
    let finalHallNo = '';
    let finalName = '';

    if (selectedPreset === 'custom') {
      finalCotNo = customCotNo.trim();
      finalHallNo = customHallNo.trim();
      finalName = customCafeteriaName.trim() || '사내식당';
    } else {
      const preset = PRESETS[selectedPreset as number];
      finalCotNo = preset.cotNo;
      finalHallNo = preset.hallNo;
      finalName = preset.name;
    }

    const savePayload = {
      kakaoId: welstorySettings.kakaoId,
      nickname: welstorySettings.nickname || '사용자',
      kakaoAccessToken: welstorySettings.kakaoAccessToken,
      kakaoRefreshToken: welstorySettings.kakaoRefreshToken,
      cotNo: finalCotNo,
      hallNo: finalHallNo,
      cafeteriaName: finalName,
      scheduledDays: activeDays.sort().join(','),
      scheduledTime: timeStr,
      isEnabled: isAlertEnabled
    };

    try {
      // 선행 저장
      await fetch(`${window.location.origin}/api/welstory/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(savePayload)
      });

      // 발송 요청
      const response = await fetch(`${window.location.origin}/api/welstory/test-send?kakaoId=${welstorySettings.kakaoId}`, {
        method: 'POST'
      });

      const data = await response.json();
      setTestSuccess(data.success);
      setTestResult(data.message);
    } catch (err: any) {
      console.error(err);
      setTestSuccess(false);
      setTestResult('발송 중 오류 발생: ' + err.message);
    } finally {
      setTestLoading(false);
    }
  };

  // 🍱 카카오 연동 해제
  const handleDisconnect = () => {
    if (window.confirm('정말 카카오 연동을 해제하시겠습니까?\n해제하시면 매일 구동되는 자동 알림도 함께 비활성화됩니다.')) {
      localStorage.removeItem('welstory_kakao_id');
      setWelstorySettings(null);
      alert('카카오 연동이 해제되었습니다.');
    }
  };

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

  // 카카오톡 결과 공유하기
  const shareToKakao = () => {
    const apiKey = (import.meta as any).env.VITE_KAKAO_MAP_API_KEY;
    if (!apiKey) {
      alert('카카오 API 키가 설정되지 않았습니다.');
      return;
    }

    const PROD_URL = 'https://amugeona-buster-6eda848df67d.herokuapp.com';
    const shareUrl = `${PROD_URL}/?room=${roomId}`;
    const winningMenu = roomState?.winningMenu || '';
    const emoji = MENU_METADATA[winningMenu]?.emoji || '🍴';
    const loc = roomState?.location || '선택한 위치';

    const sendFeed = () => {
      const win = window as any;
      if (win.Kakao) {
        if (!win.Kakao.isInitialized()) {
          win.Kakao.init(apiKey);
        }
        win.Kakao.Share.sendDefault({
          objectType: 'feed',
          content: {
            title: '아무거나 버스터 매칭 완료! 🎯',
            description: `📍 위치: ${loc}\n🏆 최종 매칭 메뉴: ${emoji} ${winningMenu}\n친구들과 함께 고른 최고의 메뉴와 맛집을 확인해 보세요!`,
            imageUrl: `${PROD_URL}/favicon.png`,
            link: {
              mobileWebUrl: shareUrl,
              webUrl: shareUrl,
            },
          },
          buttons: [
            {
              title: '결과 확인하기',
              link: {
                mobileWebUrl: shareUrl,
                webUrl: shareUrl,
              },
            },
          ],
        });
      }
    };

    const win = window as any;
    if (win.Kakao) {
      sendFeed();
    } else {
      const script = document.createElement('script');
      script.src = 'https://t1.kakaocdn.net/kakao_js_sdk/2.7.2/kakao.min.js';
      script.crossOrigin = 'anonymous';
      script.onload = () => {
        sendFeed();
      };
      script.onerror = () => {
        alert('카카오 SDK 로드에 실패했습니다.');
      };
      document.head.appendChild(script);
    }
  };

  // 결과 요약 클립보드 복사
  const handleCopyLink = () => {
    if (!roomId) return;
    const PROD_URL = 'https://amugeona-buster-6eda848df67d.herokuapp.com';
    const shareUrl = `${PROD_URL}/?room=${roomId}`;
    const winningMenu = roomState?.winningMenu || '';
    const emoji = MENU_METADATA[winningMenu]?.emoji || '🍴';
    const loc = roomState?.location || '선택한 위치';
    
    const text = `🎯 [아무거나 버스터] 실시간 스와이프 매칭 완료!\n\n📍 위치: ${loc}\n🏆 최종 매칭 메뉴: ${emoji} ${winningMenu}\n\n🔗 매칭 결과 및 맛집 정보 확인하기:\n${shareUrl}`;
    
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }).catch(err => {
      console.error('Failed to copy text: ', err);
    });
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
          <div className="w-9 h-9 rounded-lg overflow-hidden border border-zinc-200 shadow-xs bg-white">
            <img src={brandLogo} alt="아무거나 버스터 로고" className="w-full h-full object-cover" />
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

        {!roomId && (() => {
          const activeCotNo = welstorySettings?.cotNo || 'WEL_DSR';
          const activeHallNo = welstorySettings?.hallNo || 'HALL_01';
          const activeCafeteriaName = welstorySettings?.cafeteriaName || '삼성 DSR 타워 웰스토리';
          return (
            <button
              onClick={() => triggerFetchMenuDetails(activeCotNo, activeHallNo, activeCafeteriaName)}
              className="bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white px-4 py-2 rounded-xl text-xs font-black shadow-sm transition-all scale-100 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              오늘의 식단표
            </button>
          );
        })()}
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
          <>
            <div className="w-full grid md:grid-cols-12 gap-10 items-center">
            {/* Left: Hero Copy */}
            <div className="md:col-span-7 flex flex-col gap-5 text-center md:text-left">
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-zinc-900 leading-tight">
                약속 메뉴 정할 땐,<br />
                <span className="text-orange-500">스와이프 한 번이면 끝</span>
              </h1>
              <p className="text-zinc-500 text-sm sm:text-base max-w-md leading-relaxed mx-auto md:mx-0 font-medium">
                더 이상 무엇을 먹을지 고민하며 방황하지 마세요. 스와이프 투표로 모두가 만족하는 최고의 메뉴를 결정하고, 커피 내기 복불복 게임을 통해 유쾌하게 골든벨 주인공을 가려드립니다.
              </p>

              {/* Feature Pills */}
              <div className="flex flex-wrap gap-3 mt-2 justify-center md:justify-start">
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Users className="w-4 h-4 text-zinc-400" />
                  실시간 대기실
                </div>
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Flame className="w-4 h-4 text-orange-400" />
                  스와이프 투표
                </div>
                <div className="flex items-center gap-2 bg-white border border-zinc-200 px-4 py-2.5 rounded-lg text-sm text-zinc-700">
                  <Compass className="w-4 h-4 text-teal-400" />
                  맛집 매칭 지도
                </div>
                <div className="flex items-center gap-2 bg-white border border-rose-200 shadow-xs hover:border-rose-300 px-4 py-2.5 rounded-lg text-sm text-rose-600 font-black">
                  커피빵 미니게임
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

          {/* 독립 프리미엄 위젯 3형제 */}
          <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-6 mt-12 animate-slide-up">
            {/* 위젯 1: 실시간 식단표 */}
            {(() => {
              const activeCotNo = welstorySettings?.cotNo || 'WEL_DSR';
              const activeHallNo = welstorySettings?.hallNo || 'HALL_01';
              const activeCafeteriaName = welstorySettings?.cafeteriaName || '삼성 DSR 타워 웰스토리';
              return (
                <div className="bg-white border border-zinc-200 shadow-sm rounded-2xl p-6 flex flex-col justify-between gap-4 transition-all hover:shadow-md hover:border-orange-200">
                  <div className="flex flex-col gap-2">
                    <div className="w-12 h-12 flex items-center justify-center">
                      <img src={meal3dIcon} alt="Meal Icon" className="w-full h-full object-contain" />
                    </div>
                    <h3 className="font-bold text-zinc-800 text-base mt-2">오늘의 구내식당 식단표</h3>
                    <p className="text-xs text-zinc-500 leading-relaxed">
                      현재 설정된 <span className="font-bold text-orange-600">[{activeCafeteriaName}]</span>의 실시간 코너별 식단과 메인 요리 이미지를 고해상도로 즉시 확인하세요. (지점 변경은 알림 설정을 완료하시면 자동으로 연동됩니다.)
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => triggerFetchMenuDetails(activeCotNo, activeHallNo, activeCafeteriaName)}
                    className="w-full py-2.5 bg-orange-500 hover:bg-orange-600 text-white text-xs font-bold rounded-xl transition-all shadow-sm flex items-center justify-center cursor-pointer"
                  >
                    식단표 보기 🍱
                  </button>
                </div>
              );
            })()}

            {/* 위젯 2: 스마트 카톡 식단 알림 */}
            <div className="bg-white border border-zinc-200 shadow-sm rounded-2xl p-6 flex flex-col justify-between gap-4 transition-all hover:shadow-md hover:border-amber-200">
              <div className="flex flex-col gap-2">
                <div className="w-12 h-12 flex items-center justify-center">
                  <img src={bell3dIcon} alt="Bell Icon" className="w-full h-full object-contain" />
                </div>
                <div className="flex items-center gap-2 mt-2">
                  <h3 className="font-bold text-zinc-800 text-base">카톡 식단 알림 신청</h3>
                  {welstorySettings ? (
                    <span className="text-[9px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded-md">
                      🟢 연동 중 ({welstorySettings.userName || '세션'})
                    </span>
                  ) : (
                    <span className="text-[9px] font-bold text-zinc-400 bg-zinc-100 border border-zinc-200 px-1.5 py-0.5 rounded-md">
                      🔴 계정 미연동
                    </span>
                  )}
                </div>
                <p className="text-xs text-zinc-500 leading-relaxed">
                  매일 지정한 요일과 시간에 오늘 제공되는 구내식당 식단표를 내 카카오톡 메시지로 편리하게 자동 수신합니다.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsWelstoryModalOpen(true)}
                className="w-full py-2.5 bg-zinc-800 hover:bg-zinc-900 text-white text-xs font-bold rounded-xl transition-all shadow-sm flex items-center justify-center cursor-pointer"
              >
                식단 알림 신청 및 설정
              </button>
            </div>

            {/* 위젯 3: 커피빵 미니게임 */}
            <div className="bg-white border border-zinc-200 shadow-sm rounded-2xl p-6 flex flex-col justify-between gap-4 transition-all hover:shadow-md hover:border-rose-200">
              <div className="flex flex-col gap-2">
                <div className="w-12 h-12 flex items-center justify-center">
                  <img src={coffee3dIcon} alt="Coffee Icon" className="w-full h-full object-contain" />
                </div>
                <h3 className="font-bold text-zinc-800 text-base mt-2">커피 내기 복불복</h3>
                <p className="text-xs text-zinc-500 leading-relaxed">
                  동료들과 식후 땡 커피 골든벨을 쏠 주인공을 즉시 정해보세요! 소금 폭탄 아메리카노 💀를 고르면 당첨입니다.
                </p>
              </div>
              <button
                type="button"
                onClick={() => initCoffeeGame(4)}
                className="w-full py-2.5 bg-rose-500 hover:bg-rose-600 text-white text-xs font-bold rounded-xl transition-all shadow-sm flex items-center justify-center cursor-pointer"
              >
                커피 복불복 내기 시작
              </button>
            </div>
          </div>
        </>
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

              {/* Share Actions */}
              <div className="w-full flex flex-col gap-2 pt-4 border-t border-zinc-100">
                <p className="text-[11px] font-semibold text-zinc-400 text-left mb-1">📢 친구에게 결과 공유하기</p>
                <div className="flex gap-2">
                  <button
                    onClick={shareToKakao}
                    className="flex-1 py-2.5 bg-[#FEE500] hover:bg-[#FDD000] active:bg-[#E2CC00] text-[#191919] font-bold rounded-lg text-xs transition-all flex items-center justify-center gap-1.5 shadow-sm border border-[#EBE39B]"
                  >
                    <span className="text-sm">💬</span> 카카오톡 공유
                  </button>
                  <button
                    onClick={handleCopyLink}
                    className="flex-1 py-2.5 bg-zinc-100 hover:bg-zinc-200 active:bg-zinc-300 text-zinc-700 font-bold rounded-lg text-xs transition-all flex items-center justify-center gap-1.5 border border-zinc-200"
                  >
                    {copied ? (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        복사 완료!
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        결과 요약 복사
                      </>
                    )}
                  </button>
                </div>
              </div>

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

      {/* 🍱 웰스토리 식단 알림 설정 모달 */}
      {isWelstoryModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white border border-zinc-100 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col max-h-[90vh] scale-100 transition-all duration-300">
            {/* Modal Cover Header */}
            <div className="bg-gradient-to-r from-orange-500 to-amber-500 text-white p-6 relative flex flex-col gap-1 shrink-0">
              <button 
                onClick={() => setIsWelstoryModalOpen(false)}
                className="absolute top-4 right-4 text-white/80 hover:text-white bg-white/10 hover:bg-white/20 p-1.5 rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
              <span className="text-xl font-bold flex items-center gap-2">🍱 웰스토리 식단 알림 설정</span>
              <p className="text-xs text-white/85">매번 식단 앱 켜지 않고 내 카카오톡으로 오늘 메뉴 자동 배달!</p>
            </div>

            {/* Modal Body with Scroll */}
            <div className="overflow-y-auto p-6 flex flex-col gap-6">
              {!welstorySettings ? (
                /* STEP 1: 카카오 로그인 연동 전 */
                <div className="flex flex-col items-center text-center gap-5 py-4">
                  <div className="w-16 h-16 rounded-full bg-orange-50 flex items-center justify-center text-orange-500">
                    <Sparkles className="w-8 h-8" />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <h4 className="font-bold text-zinc-800 text-base">카카오 계정을 한 번만 연동해 주세요</h4>
                    <p className="text-xs text-zinc-500 max-w-xs leading-relaxed">
                      연동 완료 후 오늘의 구내식당 지점 코드 및 배달받고 싶은 요일, 시간을 언제든 설정할 수 있습니다.
                    </p>
                  </div>
                  <button
                    onClick={handleKakaoLink}
                    disabled={loading}
                    className="w-full bg-[#FEE500] hover:bg-[#FDD000] text-[#191919] font-bold py-3.5 px-6 rounded-xl flex items-center justify-center gap-2.5 shadow-sm hover:shadow transition-all duration-200 cursor-pointer disabled:opacity-50"
                  >
                    {loading ? (
                      <RefreshCw className="w-4 h-4 animate-spin text-[#191919]" />
                    ) : (
                      <>
                        <span className="w-4 h-4 rounded-full bg-[#191919] text-[#FEE500] text-[9px] font-black flex items-center justify-center font-mono">TALK</span>
                        카카오 계정으로 연동하기
                      </>
                    )}
                  </button>
                </div>
              ) : (
                /* STEP 2: 설정 세팅 화면 */
                <div className="flex flex-col gap-5 text-zinc-700">
                  
                  {/* 카카오 연동 유저 프로필 헤더 */}
                  <div className="bg-zinc-50 border border-zinc-100 p-3.5 rounded-xl flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-[#FEE500] text-[#191919] font-bold text-xs flex items-center justify-center">
                        {welstorySettings.nickname ? welstorySettings.nickname.charAt(0) : 'U'}
                      </div>
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-zinc-800">{welstorySettings.nickname}님</span>
                        <span className="text-[9px] text-zinc-400 font-mono">ID: {welstorySettings.kakaoId}</span>
                      </div>
                    </div>
                    <button 
                      onClick={handleDisconnect}
                      className="text-[10px] font-semibold text-zinc-400 hover:text-red-500 border border-zinc-200 hover:border-red-100 bg-white px-2 py-1 rounded-lg transition-colors cursor-pointer"
                    >
                      연동 해제
                    </button>
                  </div>

                  {/* 1. 지점 검색 및 입력 */}
                  <div className="flex flex-col gap-2">
                    <label className="text-xs font-bold text-zinc-800 flex items-center gap-1">📍 1. 구내식당 지점 코드 선택</label>
                    <div className="grid grid-cols-2 gap-2">
                      {PRESETS.map((p, idx) => (
                        <button
                          key={p.name}
                          type="button"
                          onClick={() => setSelectedPreset(idx)}
                          className={`p-2.5 rounded-lg border text-left text-xs font-medium transition-all cursor-pointer ${
                            selectedPreset === idx 
                              ? 'bg-orange-50 border-orange-300 text-orange-600 font-bold shadow-xs' 
                              : 'bg-white border-zinc-200 text-zinc-600 hover:border-zinc-300'
                          }`}
                        >
                          {p.name.replace(' 웰스토리', '').replace('식당', '')}
                        </button>
                      ))}
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedPreset('custom');
                          if (customCotNo === '') {
                            setCustomCotNo('WEL_CUSTOM');
                            setCustomHallNo('HALL_99');
                            setCustomCafeteriaName('사내 웰스토리');
                          }
                        }}
                        className={`p-2.5 rounded-lg border text-left text-xs font-medium transition-all cursor-pointer ${
                          selectedPreset === 'custom'
                            ? 'bg-orange-50 border-orange-300 text-orange-600 font-bold shadow-xs'
                            : 'bg-white border-zinc-200 text-zinc-600 hover:border-zinc-300'
                        }`}
                      >
                        ⚙️ 코드 직접 입력
                      </button>
                    </div>

                    {/* 직접 입력 시 상세 필드 노출 */}
                    {selectedPreset === 'custom' && (
                      <div className="bg-zinc-50 border border-zinc-200 p-4 rounded-xl flex flex-col gap-3.5 mt-1 animate-in fade-in-50 slide-in-from-top-2 duration-200">
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold text-zinc-500">회사 코드 (cotNo)</label>
                          <input 
                            type="text" 
                            placeholder="예: WEL_DSR"
                            value={customCotNo}
                            onChange={(e) => setCustomCotNo(e.target.value)}
                            className="w-full px-3 py-2 text-xs rounded border border-zinc-200 focus:outline-none focus:ring-1 focus:ring-orange-500"
                          />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold text-zinc-500">식당 번호 (hallNo)</label>
                          <input 
                            type="text" 
                            placeholder="예: HALL_01"
                            value={customHallNo}
                            onChange={(e) => setCustomHallNo(e.target.value)}
                            className="w-full px-3 py-2 text-xs rounded border border-zinc-200 focus:outline-none focus:ring-1 focus:ring-orange-500"
                          />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold text-zinc-500">식당 표기 명칭</label>
                          <input 
                            type="text" 
                            placeholder="예: 삼성 DSR 웰스토리"
                            value={customCafeteriaName}
                            onChange={(e) => setCustomCafeteriaName(e.target.value)}
                            className="w-full px-3 py-2 text-xs rounded border border-zinc-200 focus:outline-none focus:ring-1 focus:ring-orange-500"
                          />
                        </div>
                      </div>
                    )}
                  </div>

                  {/* 2. 발송 요일 설정 */}
                  <div className="flex flex-col gap-2">
                    <label className="text-xs font-bold text-zinc-800">📅 2. 알림 예약 요일</label>
                    <div className="flex justify-between gap-1.5 bg-zinc-50 p-1.5 rounded-xl border border-zinc-200">
                      {[
                        { val: 1, label: '월' },
                        { val: 2, label: '화' },
                        { val: 3, label: '수' },
                        { val: 4, label: '목' },
                        { val: 5, label: '금' },
                        { val: 6, label: '토' },
                        { val: 7, label: '일' }
                      ].map((day) => {
                        const active = activeDays.includes(day.val);
                        return (
                          <button
                            key={day.val}
                            type="button"
                            onClick={() => {
                              if (active) {
                                setActiveDays(activeDays.filter(d => d !== day.val));
                              } else {
                                setActiveDays([...activeDays, day.val]);
                              }
                            }}
                            className={`w-9 h-9 rounded-lg font-bold text-xs flex items-center justify-center transition-all cursor-pointer ${
                              active 
                                ? 'bg-orange-500 text-white shadow-sm scale-105' 
                                : 'bg-white hover:bg-zinc-100 text-zinc-500 border border-zinc-200'
                            }`}
                          >
                            {day.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* 3. 시간 설정 */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-bold text-zinc-800">⏰ 3. 알림 발송 시간</label>
                      <input
                        type="time"
                        value={timeStr}
                        onChange={(e) => setTimeStr(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-zinc-200 bg-white text-zinc-800 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 font-mono font-bold text-sm text-center"
                      />
                    </div>
                    
                    {/* 4. 활성화 여부 토글 */}
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-bold text-zinc-800">📴 4. 알림 작동 여부</label>
                      <button
                        type="button"
                        onClick={() => setIsAlertEnabled(!isAlertEnabled)}
                        className={`w-full py-3 rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 transition-all border cursor-pointer ${
                          isAlertEnabled 
                            ? 'bg-emerald-50 hover:bg-emerald-100 text-emerald-600 border-emerald-200' 
                            : 'bg-zinc-50 hover:bg-zinc-100 text-zinc-400 border-zinc-200'
                        }`}
                      >
                        <CheckCircle2 className={`w-4 h-4 ${isAlertEnabled ? 'text-emerald-500' : 'text-zinc-300'}`} />
                        {isAlertEnabled ? '매일 알림 구동 중' : '일시 정지됨'}
                      </button>
                    </div>
                  </div>

                  {/* 테스트 발송 피드백 박스 */}
                  {testResult && (
                    <div className={`p-4 rounded-xl border flex items-start gap-2.5 mt-1 text-xs animate-in fade-in-50 duration-200 ${
                      testSuccess 
                        ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                        : 'bg-red-50 border-red-200 text-red-700'
                    }`}>
                      <Info className={`w-4 h-4 shrink-0 mt-0.5 ${testSuccess ? 'text-emerald-500' : 'text-red-500'}`} />
                      <span>{testResult}</span>
                    </div>
                  )}

                  {/* 제어 버튼 그룹 */}
                  <div className="mt-4 pt-4 border-t border-zinc-100 flex gap-3">
                    <button
                      type="button"
                      onClick={handleTestSend}
                      disabled={testLoading || saveLoading}
                      className="flex-1 py-3.5 bg-zinc-800 hover:bg-zinc-900 disabled:opacity-50 text-white font-bold rounded-xl transition-colors text-center text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
                    >
                      {testLoading ? (
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        '테스트 전송 💬'
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={handleSaveSettings}
                      disabled={testLoading || saveLoading}
                      className="flex-1 py-3.5 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 disabled:opacity-50 text-white font-bold rounded-xl transition-colors text-center text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-md"
                    >
                      {saveLoading ? (
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        '설정 저장하기 💾'
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 🍱 웰스토리 실시간 전체 식단표 프리미엄 뷰어 모달 */}
      {isMenuDetailOpen && (
        <div className={`fixed inset-0 bg-black/75 backdrop-blur-md z-50 flex justify-center p-4 overflow-y-auto overscroll-contain transition-all duration-300 ${
          isReviewInputFocused ? 'items-start pt-2 pb-64 sm:items-center sm:pt-4 sm:pb-4' : 'items-center'
        }`}>
          <div className={`bg-slate-50 border border-zinc-200 rounded-3xl shadow-2xl w-full max-w-4xl overflow-hidden flex flex-col scale-100 transition-all duration-300 ${
            isReviewInputFocused ? 'max-h-[85vh] sm:max-h-[90vh]' : 'max-h-[90vh]'
          }`}>
            
            {/* 1. 프리미엄 배너 헤더 */}
            <div className="bg-gradient-to-br from-orange-500 via-amber-500 to-red-500 text-white p-6 sm:p-8 relative flex flex-col gap-2 shrink-0 shadow-lg">
              <div className="absolute top-4 sm:top-6 right-4 sm:right-6 flex items-center gap-2">
                {/* 알림 설정창 워프 링크 */}
                <button 
                  onClick={() => {
                    setIsMenuDetailOpen(false);
                    setIsWelstoryModalOpen(true);
                    setIsReviewInputFocused(false);
                  }}
                  title="알림 주기 및 지점 변경 설정"
                  className="text-white/80 hover:text-white bg-white/10 hover:bg-white/20 p-2 rounded-xl transition-all cursor-pointer flex items-center justify-center shadow-xs"
                >
                  <Settings className="w-4 h-4 sm:w-5 h-5 animate-hover-spin" />
                </button>
                {/* 창 닫기 */}
                <button 
                  onClick={() => {
                    setIsMenuDetailOpen(false);
                    setIsReviewInputFocused(false);
                  }}
                  title="메뉴판 닫기"
                  className="text-white/80 hover:text-white bg-white/10 hover:bg-white/20 p-2 rounded-xl transition-all cursor-pointer flex items-center justify-center shadow-xs"
                >
                  <X className="w-4 h-4 sm:w-5 h-5" />
                </button>
              </div>
              <span className="text-[10px] font-black uppercase tracking-wider text-orange-950 bg-yellow-100 self-start px-3 py-1 rounded-full shadow-inner">
                🍱 실시간 구내식당 메뉴판
              </span>
              <h3 className="text-xl sm:text-2xl font-black tracking-tight mt-1 flex items-center gap-2">
                {menuDetailCafeteriaName}
              </h3>
              <p className="text-xs sm:text-sm text-white/90 font-medium flex items-center gap-1.5 mt-0.5">
                <span className="font-semibold text-yellow-200">📅 {menuDetailDate}</span>
              </p>
            </div>

            {/* 시간대별 4단 식단 필터 탭 바 (아침 / 점심 / 저녁 / 전체보기) */}
            {!menuDetailLoading && !menuDetailError && menuDetailCourses.length > 0 && (
              <div className="bg-white border-b border-zinc-200/80 px-5 py-3 shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-3 select-none animate-slide-down">
                <span className="text-xs font-black text-zinc-700 tracking-tight flex items-center gap-1.5 whitespace-nowrap shrink-0">
                  🕒 오늘 제공 식단 필터
                </span>
                <div className="flex bg-zinc-100/80 p-1 rounded-xl border border-zinc-200/30 gap-1 w-full sm:w-auto justify-between sm:justify-start shadow-inner">
                  {[
                    { key: 'all', label: '전체보기' },
                    { key: 'breakfast', label: '아침 ☀️' },
                    { key: 'lunch', label: '점심 🌤️' },
                    { key: 'dinner', label: '저녁 🌙' }
                  ].map((tab) => (
                    <button
                      key={tab.key}
                      onClick={() => setMenuMealFilter(tab.key as any)}
                      className={`flex-1 sm:flex-none px-3.5 py-1.5 rounded-lg text-xs font-black transition-all cursor-pointer whitespace-nowrap text-center ${
                        menuMealFilter === tab.key
                          ? 'bg-white text-orange-600 shadow-sm border border-zinc-200/50 scale-[1.02]'
                          : 'text-zinc-500 hover:text-zinc-700'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* 2. 실시간 식단 데이터 영역 */}
                {/* 2. 로딩 상태 */}
                {menuDetailLoading && (
                  <div className="flex flex-col items-center justify-center py-24 px-6 gap-5 text-center flex-grow">
                    <RefreshCw className="w-10 h-10 animate-spin text-orange-500" />
                    <div className="flex flex-col gap-1.5">
                      <h4 className="font-bold text-zinc-800 text-base">실시간 식단 데이터 수집 중</h4>
                      <p className="text-xs text-zinc-500 max-w-sm leading-relaxed">
                        삼성 웰스토리 플러스의 실시간 코너 정보를 가공하여 최상의 가독성으로 변환 중입니다. 잠시만 기다려 주세요!
                      </p>
                    </div>
                  </div>
                )}

                {/* 3. 에러 발생 상태 */}
                {menuDetailError && (
                  <div className="flex flex-col items-center justify-center py-20 px-6 gap-6 text-center max-w-md mx-auto flex-grow">
                    <div className="w-16 h-16 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-500 shadow-sm border border-rose-100">
                      <AlertCircle className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col gap-2">
                      <h4 className="font-bold text-zinc-800 text-base">식단 정보를 가져올 수 없습니다</h4>
                      <p className="text-xs text-rose-600 leading-relaxed font-semibold">{menuDetailError}</p>
                    </div>
                    <div className="flex gap-3 w-full">
                      <button
                        onClick={() => triggerFetchMenuDetails(menuDetailCotNo, menuDetailHallNo, menuDetailCafeteriaName)}
                        className="flex-1 py-3.5 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-xl transition-all text-xs cursor-pointer shadow-md"
                      >
                        새로고침 🔄
                      </button>
                      <button
                        onClick={() => {
                          setIsMenuDetailOpen(false);
                          setIsWelstoryModalOpen(true);
                        }}
                        className="flex-1 py-3.5 bg-zinc-800 hover:bg-zinc-900 text-white font-bold rounded-xl transition-all text-xs cursor-pointer shadow-sm"
                      >
                        지점 변경 ⚙️
                      </button>
                    </div>
                  </div>
                )}

                {/* 4. 데이터가 존재하지 않는 경우 */}
                {!menuDetailLoading && !menuDetailError && menuDetailCourses.length === 0 && (
                  <div className="flex flex-col items-center justify-center py-24 px-6 gap-5 text-center flex-grow">
                    <div className="w-16 h-16 rounded-full bg-amber-50 flex items-center justify-center text-amber-500 border border-amber-100">
                      <Info className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <h4 className="font-bold text-zinc-800 text-base">오늘 등록된 식단이 없습니다</h4>
                      <p className="text-xs text-zinc-500 max-w-xs leading-relaxed">
                        주말/공휴일이거나 해당 식당 지점의 식단 등록이 완료되지 않았습니다. 설정에서 지점을 변경해 보세요!
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setIsMenuDetailOpen(false);
                        setIsWelstoryModalOpen(true);
                      }}
                      className="mt-2 py-2.5 px-5 bg-zinc-800 hover:bg-zinc-900 text-white font-bold rounded-xl transition-colors text-xs cursor-pointer shadow-sm"
                    >
                      구내식당 지점 변경하러 가기 ⚙️
                    </button>
                  </div>
                )}

                {/* 5. 정상 식단 목록 (스크롤) */}
                {!menuDetailLoading && !menuDetailError && menuDetailCourses.length > 0 && (() => {
                  // 식단 시간대 판별 헬퍼
                  const filteredCourses = menuDetailCourses.filter((course: any) => {
                    const name = (course.courseName || '').toLowerCase();
                    const isBreakfast = name.includes('조식') || name.includes('아침') || name.includes('breakfast');
                    const isDinner = name.includes('석식') || name.includes('저녁') || name.includes('dinner');
                    // 아침이나 저녁에 해당되지 않으면 점심으로 기본 분류
                    const isLunch = name.includes('중식') || name.includes('점심') || name.includes('lunch') || (!isBreakfast && !isDinner);
                    
                    if (menuMealFilter === 'breakfast') return isBreakfast;
                    if (menuMealFilter === 'dinner') return isDinner;
                    if (menuMealFilter === 'lunch') return isLunch;
                    return true; // 'all'
                  });

                  if (filteredCourses.length === 0) {
                    return (
                      <div className="flex flex-col items-center justify-center py-28 px-6 gap-5 text-center flex-grow bg-slate-50/50">
                        <div className="w-16 h-16 rounded-full bg-zinc-100 flex items-center justify-center text-zinc-400 border border-zinc-200/50 text-2xl shadow-xs">
                          {menuMealFilter === 'breakfast' ? '☀️' : menuMealFilter === 'dinner' ? '🌙' : '🌤️'}
                        </div>
                        <div className="flex flex-col gap-1.5">
                          <h4 className="font-bold text-zinc-800 text-base">
                            오늘 준비된 {menuMealFilter === 'breakfast' ? '아침' : menuMealFilter === 'dinner' ? '저녁' : '점심'} 식단이 없습니다
                          </h4>
                          <p className="text-xs text-zinc-500 max-w-xs leading-relaxed mx-auto">
                            해당 시간대에는 식사를 운영하지 않거나 식단 정보가 등록되지 않았습니다. 전체보기를 통해 다른 시간대의 식단을 확인해 보세요!
                          </p>
                        </div>
                      </div>
                    );
                  }

                  return (
                    <div className="overflow-y-auto overscroll-contain p-5 sm:p-8 flex-grow bg-slate-50/50">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {filteredCourses.map((course: any, idx: number) => {
                          const { badgeGradient, courseEmoji } = getCourseStyle(course.courseName);
                          
                          const dishes = course.menuDetails
                            ? course.menuDetails.split(',').map((d: string) => d.trim()).filter((d: string) => d.length > 0)
                            : [];
                          
                          const mainDish = dishes[0] || '식단 준비 중';
                          const sideDishes = dishes.slice(1);
                          
                          const caloriePercentage = Math.min(100, Math.max(10, (course.calories / 1200) * 100));
                          const calorieColorClass = course.calories < 600 
                            ? 'bg-emerald-500' 
                            : course.calories < 850 
                              ? 'bg-orange-500' 
                              : 'bg-rose-500';

                          return (
                            <div 
                              key={idx} 
                              className="relative bg-white border border-zinc-200 hover:border-orange-300 rounded-3xl p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between gap-5 overflow-hidden group"
                            >
                              <div className={`absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r ${badgeGradient}`} />

                              <div className="flex flex-col gap-4">
                                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between items-start gap-3 w-full">
                                  <span className={`text-xs font-black tracking-tight text-white bg-gradient-to-r ${badgeGradient} px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow-sm w-full sm:w-auto whitespace-normal break-all`}>
                                    <span className="shrink-0">{courseEmoji}</span>
                                    <span>{course.courseName}</span>
                                  </span>
                                  <div className="flex items-center gap-1.5 select-none w-full sm:w-auto justify-start sm:justify-end">
                                    {course.price && (
                                      <span className="text-xs font-bold text-zinc-600 bg-zinc-100/80 border border-zinc-200/40 px-3 py-1.5 rounded-xl font-mono shadow-xs whitespace-nowrap shrink-0">
                                        💰 {course.price}
                                      </span>
                                    )}
                                    <button
                                      onClick={() => {
                                        const isOpen = expandedReviewCourseName === course.courseName;
                                        setExpandedReviewCourseName(isOpen ? null : course.courseName);
                                        if (!isOpen) {
                                          fetchCourseReviews(menuDetailCafeteriaName, menuDetailDate);
                                        }
                                      }}
                                      className="text-xs font-black text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200/50 px-3 py-1.5 rounded-xl flex items-center gap-1 shadow-xs transition-colors cursor-pointer whitespace-nowrap shrink-0"
                                    >
                                      <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400 shrink-0" />
                                      <span>{reviewStats[course.courseName]?.averageRating || '0.0'}</span>
                                      <span className="text-zinc-400 text-[10px] font-normal">({reviewStats[course.courseName]?.reviewCount || 0})</span>
                                    </button>
                                  </div>
                                </div>

                                <div className="relative w-full h-44 rounded-2xl overflow-hidden bg-zinc-100 shadow-inner border border-zinc-100">
                                  <img 
                                    src={getCourseImage(course.courseName, course.imageUrl)} 
                                    alt={course.courseName} 
                                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
                                    onError={(e) => {
                                      e.currentTarget.src = getCourseImage(course.courseName);
                                    }}
                                  />
                                  <div className="absolute inset-0 bg-gradient-to-t from-black/35 via-transparent to-transparent pointer-events-none" />
                                </div>

                                <div className="flex flex-col gap-3.5">
                                  <div className="bg-orange-50/50 border border-orange-100/50 p-3.5 rounded-2xl">
                                    <span className="text-[9px] font-black text-orange-500 bg-orange-100/60 px-2 py-0.5 rounded-md uppercase tracking-wider block w-fit mb-1.5">
                                      👑 오늘의 메인 요리
                                    </span>
                                    <h4 className="text-sm font-black text-zinc-800 leading-snug">
                                      {mainDish}
                                    </h4>
                                  </div>

                                  {sideDishes.length > 0 && (
                                    <div className="flex flex-col gap-2 px-1">
                                      <span className="text-[10px] font-extrabold text-zinc-400 uppercase tracking-wider block mb-1">
                                        🥗 함께 제공되는 찬류 및 사이드
                                      </span>
                                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2">
                                        {sideDishes.map((side: string, sIdx: number) => (
                                          <div key={sIdx} className="flex items-center gap-2 text-xs font-bold text-zinc-600">
                                            <span className="w-1.5 h-1.5 rounded-full bg-orange-400 shrink-0 shadow-xs" />
                                            <span className="truncate" title={side}>{side}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              </div>

                              {course.calories > 0 && (
                                <div className="border-t border-zinc-100 pt-3 flex flex-col gap-1.5">
                                  <div className="flex items-center justify-between text-[11px] font-black text-zinc-400">
                                    <span className="flex items-center gap-1">🔥 총 칼로리</span>
                                    <span className="font-mono text-zinc-700 text-xs font-bold">{course.calories} kcal</span>
                                  </div>
                                  <div className="w-full h-2.5 bg-zinc-100 rounded-full overflow-hidden shadow-inner relative">
                                    <div 
                                      className={`h-full rounded-full transition-all duration-500 ${calorieColorClass}`} 
                                      style={{ width: `${caloriePercentage}%` }}
                                    />
                                  </div>
                                </div>
                              )}

                              {/* ⭐ 한줄평 후기 시스템 UI 영역 */}
                              <div className="border-t border-zinc-100 pt-4 flex flex-col gap-3">
                                <div className="flex items-center justify-between gap-2">
                                  <button
                                    onClick={() => {
                                      const isOpen = expandedReviewCourseName === course.courseName;
                                      setExpandedReviewCourseName(isOpen ? null : course.courseName);
                                      if (!isOpen) {
                                        fetchCourseReviews(menuDetailCafeteriaName, menuDetailDate);
                                      }
                                    }}
                                    className="text-[11px] font-black text-zinc-500 hover:text-zinc-700 flex items-center gap-1 transition-colors cursor-pointer select-none"
                                  >
                                    <MessageSquare className="w-3.5 h-3.5 shrink-0" />
                                    <span>한줄평 보기 ({reviewStats[course.courseName]?.reviewCount || 0})</span>
                                    <span className="text-[10px] text-zinc-400 font-bold">
                                      {expandedReviewCourseName === course.courseName ? '▼' : '▶'}
                                    </span>
                                  </button>
                                  
                                  {!submittedReviewCourses[`${menuDetailCafeteriaName}-${menuDetailDate}-${course.courseName}`] ? (
                                    <button
                                      onClick={() => {
                                        if (activeReviewWriteCourseName === course.courseName) {
                                          setActiveReviewWriteCourseName(null);
                                          setIsReviewInputFocused(false);
                                        } else {
                                          handleOpenReviewWrite(course.courseName);
                                        }
                                      }}
                                      className="text-[10px] font-black text-white bg-orange-500 hover:bg-orange-600 px-3 py-1.5 rounded-xl flex items-center gap-1 shadow-sm transition-all cursor-pointer select-none"
                                    >
                                      ✍️ 후기 남기기
                                    </button>
                                  ) : (
                                    <span className="text-[10px] font-black text-emerald-600 bg-emerald-50 border border-emerald-100 px-3 py-1.5 rounded-xl select-none">
                                      👍 평가 완료
                                    </span>
                                  )}
                                </div>

                                {/* 1) 후기 작성 폼 영역 */}
                                {activeReviewWriteCourseName === course.courseName && (
                                  <div className="bg-slate-50/70 border border-zinc-200/60 p-4 rounded-2xl flex flex-col gap-3.5 animate-slide-down shadow-inner">
                                    <div className="flex items-center justify-between gap-2">
                                      <span className="text-[10px] font-black text-zinc-700">✍️ 오늘의 식단 한줄평</span>
                                      {/* 1~5점 황금 별 선택 */}
                                      <div className="flex items-center gap-1">
                                        {[1, 2, 3, 4, 5].map((num) => (
                                          <button
                                            key={num}
                                            type="button"
                                            onClick={() => setNewReviewRating(num)}
                                            className="focus:outline-hidden cursor-pointer p-0.5"
                                          >
                                            <Star
                                              className={`w-4 h-4 transition-colors ${
                                                num <= newReviewRating ? 'fill-amber-400 text-amber-400' : 'text-zinc-300'
                                              }`}
                                            />
                                          </button>
                                        ))}
                                      </div>
                                    </div>

                                    <div className="flex gap-2">
                                      <div className="flex flex-col gap-1 w-2/5">
                                        <span className="text-[9px] font-black text-zinc-400 tracking-wider">닉네임</span>
                                        <input
                                          type="text"
                                          value={newReviewNickname}
                                          onChange={(e) => setNewReviewNickname(e.target.value.substring(0, 15))}
                                          onFocus={() => {
                                            setIsReviewInputFocused(true);
                                            setTimeout(() => {
                                              document.activeElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                            }, 150);
                                          }}
                                          onBlur={() => setIsReviewInputFocused(false)}
                                          className="bg-white border border-zinc-200 rounded-xl px-2.5 py-1.5 text-[11px] font-extrabold text-zinc-700 focus:outline-hidden focus:border-orange-400 shadow-2xs"
                                          placeholder="익명의 사우"
                                        />
                                      </div>
                                      <div className="flex flex-col gap-1 w-3/5">
                                        <span className="text-[9px] font-black text-zinc-400 tracking-wider">별점</span>
                                        <span className="text-[11px] font-black text-amber-600 px-1 py-1.5">{newReviewRating}점 만점! ⭐</span>
                                      </div>
                                    </div>

                                    <div className="flex flex-col gap-1">
                                      <span className="text-[9px] font-black text-zinc-400 tracking-wider">후기 내용 (최대 100자)</span>
                                      <textarea
                                        value={newReviewComment}
                                        onChange={(e) => setNewReviewComment(e.target.value.substring(0, 100))}
                                        onFocus={() => {
                                          setIsReviewInputFocused(true);
                                          setTimeout(() => {
                                            document.activeElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                          }, 150);
                                        }}
                                        onBlur={() => setIsReviewInputFocused(false)}
                                        className="bg-white border border-zinc-200 rounded-xl p-2.5 text-[11px] font-bold text-zinc-700 focus:outline-hidden focus:border-orange-400 shadow-2xs h-16 resize-none"
                                        placeholder="오늘 식단 어땠나요? 반찬 구성, 간, 맛에 대한 생생한 후기를 남겨주세요."
                                      />
                                      <div className="flex items-center justify-between text-[9px] font-bold text-zinc-400 px-1 mt-0.5">
                                        <span>{newReviewComment.length} / 100자</span>
                                        <button
                                          onClick={() => handleReviewSubmit(course.courseName, course.menuDetails)}
                                          disabled={submittingReview}
                                          className="px-3.5 py-1.5 bg-zinc-800 hover:bg-zinc-900 text-white rounded-lg transition-colors font-black flex items-center gap-1 shadow-xs cursor-pointer disabled:opacity-50"
                                        >
                                          {submittingReview ? '등록 중...' : '후기 등록 🚀'}
                                        </button>
                                      </div>
                                    </div>
                                  </div>
                                )}

                                {/* 2) 한줄평 목록 리스트업 영역 (아코디언 형태) */}
                                {expandedReviewCourseName === course.courseName && (
                                  <div className="bg-slate-50/40 border border-zinc-100 p-3 rounded-2xl flex flex-col gap-2.5 animate-slide-down max-h-56 overflow-y-auto overscroll-contain shadow-inner">
                                    {loadingReviews ? (
                                      <div className="text-center py-6 text-zinc-400 font-bold text-[10px] flex items-center justify-center gap-1.5">
                                        <RefreshCw className="w-3.5 h-3.5 animate-spin text-orange-500" />
                                        <span>최신 한줄평 불러오는 중...</span>
                                      </div>
                                    ) : (
                                      (() => {
                                        const reviewsForCourse = courseReviews.filter((r: any) => r.courseName === course.courseName);
                                        if (reviewsForCourse.length === 0) {
                                          return (
                                            <div className="text-center py-6 text-zinc-400 font-extrabold text-[10px] leading-relaxed">
                                              📢 아직 오늘 작성된 한줄평 후기가 없습니다.<br />
                                              <span className="text-orange-500 font-black">오늘 첫 번째 후기의 주인공이 되어주세요! ✍️</span>
                                            </div>
                                          );
                                        }
                                        return reviewsForCourse.map((review: any) => (
                                          <div key={review.id} className="bg-white border border-zinc-100 p-2.5 rounded-xl shadow-2xs flex flex-col gap-1.5 animate-fade-in">
                                            <div className="flex items-center justify-between gap-2 text-[10px]">
                                              <div className="flex items-center gap-1.5">
                                                <span className="font-extrabold text-zinc-700">{review.nickname}</span>
                                                <span className="text-zinc-300 font-normal">|</span>
                                                <div className="flex items-center gap-0.5 text-amber-500 font-black">
                                                  <Star className="w-3 h-3 fill-amber-400 text-amber-400 shrink-0" />
                                                  <span>{review.rating}</span>
                                                </div>
                                              </div>
                                              <span className="text-zinc-400 text-[9px] font-medium">
                                                {new Date(review.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                                              </span>
                                            </div>
                                            <p className="text-[11px] font-bold text-zinc-600 leading-relaxed pl-1.5 border-l-2 border-orange-200">
                                              {review.comment}
                                            </p>
                                          </div>
                                        ));
                                      })()
                                    )}
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                      {isReviewInputFocused && (
                        <div className="h-72 sm:h-0 w-full shrink-0 transition-all duration-300" />
                      )}
                    </div>
                  );
                })()}

            {/* 6. 모달 하단 퀵 액션 */}
            <div className="bg-zinc-50/80 border-t border-zinc-200 p-4 shrink-0 flex flex-col sm:flex-row sm:items-center justify-end gap-4 text-xs font-bold text-zinc-500 sm:px-6">
              <button 
                onClick={() => {
                  setIsMenuDetailOpen(false);
                  setIsReviewInputFocused(false);
                }}
                className="px-4 py-2 bg-zinc-800 hover:bg-zinc-900 text-white rounded-lg transition-colors cursor-pointer shrink-0 align-self-end sm:align-self-auto"
              >
                닫기
              </button>
            </div>

          </div>
        </div>
      )}

      {/* ==================== 8. 커피빵 내기 미니게임 모달 (isCoffeeGameOpen) ==================== */}
      {isCoffeeGameOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/60 backdrop-blur-md animate-fade-in select-none">
          <div className="bg-white border border-zinc-200/80 shadow-2xl rounded-3xl w-full max-w-2xl h-[85vh] sm:h-[80vh] flex flex-col overflow-hidden animate-scale-up">
            
            {/* 1. 모달 헤더 */}
            <div className="bg-gradient-to-br from-rose-500 via-pink-500 to-amber-500 text-white p-6 sm:p-8 relative flex flex-col gap-2 shrink-0 shadow-lg">
              <div className="absolute top-4 sm:top-6 right-4 sm:right-6">
                <button 
                  onClick={() => setIsCoffeeGameOpen(false)}
                  title="게임 종료"
                  className="text-white/80 hover:text-white bg-white/10 hover:bg-white/20 p-2 rounded-xl transition-all cursor-pointer flex items-center justify-center shadow-xs"
                >
                  <X className="w-4 h-4 sm:w-5 h-5" />
                </button>
              </div>
              <span className="text-[10px] font-black uppercase tracking-wider text-rose-950 bg-rose-100 self-start px-3 py-1 rounded-full shadow-inner">
                🎮 커피빵 미니게임
              </span>
              <h3 className="text-xl sm:text-2xl font-black tracking-tight mt-1 flex items-center gap-2">
                소금 아메리카노 복불복 ☕
              </h3>
              <p className="text-xs sm:text-sm text-white/95 font-medium mt-0.5 leading-relaxed">
                엎어진 컵 아래에 숨겨진 썩은 소금 아메리카노(꽝)를 피해 동료들과 쫄깃한 긴장감을 느껴보세요!
              </p>
            </div>

            {/* 2. 게임 플레이 스크롤 영역 */}
            <div className="overflow-y-auto p-5 sm:p-8 flex-grow bg-slate-50/50 flex flex-col items-center justify-start gap-6">
              
              {/* A. 상단 컨트롤 패널 */}
              <div className="w-full max-w-md bg-white border border-zinc-200/80 p-5 rounded-3xl shadow-sm flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs sm:text-sm font-black text-zinc-700 flex items-center gap-1.5">
                    👥 내기 참여 인원
                  </span>
                  <span className="text-sm sm:text-base font-black text-rose-500 font-mono bg-rose-50 px-3 py-1 rounded-xl">
                    {playerCount}명
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <button 
                    onClick={() => {
                      const val = Math.max(3, playerCount - 1);
                      setPlayerCount(val);
                      initCoffeeGame(val);
                    }}
                    className="w-10 h-10 rounded-xl bg-zinc-100 hover:bg-zinc-200 border border-zinc-200/50 text-zinc-600 font-black text-lg flex items-center justify-center cursor-pointer select-none"
                  >
                    -
                  </button>
                  <input 
                    type="range" 
                    min="3" 
                    max="8" 
                    value={playerCount} 
                    onChange={(e) => {
                      const val = parseInt(e.target.value, 10);
                      setPlayerCount(val);
                      initCoffeeGame(val);
                    }}
                    className="flex-grow h-2 bg-zinc-100 rounded-lg appearance-none cursor-pointer accent-rose-500"
                  />
                  <button 
                    onClick={() => {
                      const val = Math.min(8, playerCount + 1);
                      setPlayerCount(val);
                      initCoffeeGame(val);
                    }}
                    className="w-10 h-10 rounded-xl bg-zinc-100 hover:bg-zinc-200 border border-zinc-200/50 text-zinc-600 font-black text-lg flex items-center justify-center cursor-pointer select-none"
                  >
                    +
                  </button>
                </div>
                <button 
                  onClick={() => initCoffeeGame(playerCount)}
                  className="w-full py-3 bg-rose-500 hover:bg-rose-600 text-white font-black text-xs rounded-2xl transition-all shadow-md shadow-rose-500/20 cursor-pointer"
                >
                  🔄 컵 다시 섞기
                </button>
              </div>

              {/* B. 게임 플레이 영역 (하이브리드 3D 쉘 렌더링 적용) */}
              {gameStatus === 'playing' && (
                <div className="w-full max-w-lg mt-2 animate-slide-up">
                  <div className="grid grid-cols-3 sm:grid-cols-4 gap-4 justify-items-center">
                    {cupStates.map((cup, idx) => (
                      <div 
                        key={idx}
                        onClick={() => handleCupClick(idx)}
                        className="relative w-20 h-24 sm:w-24 sm:h-28 cursor-pointer perspective-1000 group select-none"
                      >
                        <div 
                          className={`relative w-full h-full rounded-2xl transition-transform duration-500 preserve-3d shadow-sm ${
                            cup.flipped ? '[transform:rotateY(180deg)]' : 'group-hover:scale-105'
                          }`}
                        >
                          {/* 컵 앞면 (엎어져 있는 상태) */}
                          <div className="absolute inset-0 bg-gradient-to-br from-zinc-800 to-zinc-900 border border-zinc-700 rounded-2xl flex flex-col items-center justify-center gap-1 backface-hidden text-white">
                            <span className="text-xl sm:text-2xl animate-pulse">☕</span>
                            <span className="text-[10px] font-black font-mono text-zinc-400">CUP {idx + 1}</span>
                          </div>

                          {/* 컵 뒷면 (뒤집힌 상태 - 커피 또는 소금) */}
                          {/* 앞뒷면 컨테이너는 항상 존재하지만 내부 이모지와 라벨은 flipped 상태일 때만 생성하여 정보 원천 차단 */}
                          <div className={`absolute inset-0 border rounded-2xl flex flex-col items-center justify-center [transform:rotateY(180deg)] backface-hidden ${
                            cup.isSalt 
                              ? 'bg-gradient-to-br from-red-50 to-rose-100 border-red-300 text-red-500' 
                              : 'bg-gradient-to-br from-amber-50 to-orange-100 border-orange-300 text-amber-800'
                          }`}>
                            {cup.flipped && (
                              cup.isSalt ? (
                                <>
                                  <span className="text-3xl animate-bounce">💀🧂</span>
                                  <span className="text-[9px] font-black text-red-600 bg-red-100 px-1.5 py-0.5 rounded-md mt-1 font-mono">폭탄 당첨</span>
                                </>
                              ) : (
                                <>
                                  <span className="text-2xl">☕✨</span>
                                  <span className="text-[9px] font-black text-amber-700 bg-amber-100 px-1.5 py-0.5 rounded-md mt-1 font-mono">SAFE</span>
                                </>
                              )
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* C. 꽝 발생: 당첨자 이름 입력 화면 */}
              {gameStatus === 'gameover' && !showReceipt && (
                <div className="w-full max-w-md bg-white border border-rose-100 p-6 rounded-3xl shadow-xl flex flex-col items-center gap-5 text-center border-t-4 border-t-rose-500 animate-slide-up mt-2">
                  <span className="text-5xl animate-bounce">💀🧂</span>
                  <div className="flex flex-col gap-1.5">
                    <h4 className="text-base font-black text-zinc-800">소금 폭탄 아메리카노 당첨!</h4>
                    <p className="text-xs text-zinc-500 font-medium">영광의 커피 골든벨을 울릴 주인공의 성함/닉네임을 입력하세요.</p>
                  </div>
                  <input 
                    type="text" 
                    value={looserName} 
                    onChange={(e) => setLooserName(e.target.value)}
                    placeholder="예: 홍대리, 김과장"
                    maxLength={10}
                    className="w-full px-4 py-3 border border-zinc-200 focus:border-rose-500 rounded-2xl font-bold text-center text-zinc-800 focus:outline-none shadow-sm transition-all"
                  />
                  <button 
                    onClick={() => {
                      if (!looserName.trim()) return;
                      setShowReceipt(true);
                      playAudio('https://assets.mixkit.co/active_storage/sfx/1657/1657-200.wav');
                    }}
                    disabled={!looserName.trim()}
                    className="w-full py-3.5 bg-rose-500 hover:bg-rose-600 disabled:bg-zinc-300 text-white font-black text-xs rounded-2xl transition-all shadow-md cursor-pointer select-none"
                  >
                    🧾 골든벨 영수증 발급하기
                  </button>
                </div>
              )}

              {/* D. 최종 골든벨 영수증 렌더링 */}
              {gameStatus === 'gameover' && showReceipt && (
                <div className="w-full max-w-xs bg-white border-2 border-dashed border-zinc-300 p-6 rounded-3xl shadow-2xl flex flex-col gap-4 text-zinc-800 relative font-mono overflow-hidden animate-receipt-roll mt-2">
                  <div className="absolute top-0 left-0 right-0 h-1 bg-[repeating-linear-gradient(90deg,#000,#000_10px,transparent_10px,transparent_20px)] opacity-10" />
                  
                  <div className="text-center flex flex-col gap-1 border-b border-dashed border-zinc-300 pb-4">
                    <h3 className="text-sm font-black tracking-widest text-zinc-800 uppercase">☕ [아무거나 커피숍] ☕</h3>
                    <span className="text-[9px] font-bold text-zinc-400">AMUGEONA COFFEE SHOP (DSR BLDG)</span>
                    <span className="text-[9px] font-bold text-zinc-400">TEL: 02-1234-5678</span>
                  </div>

                  <div className="flex flex-col gap-1.5 text-[10px] font-bold border-b border-dashed border-zinc-300 pb-4">
                    <div className="flex justify-between">
                      <span>발행일시:</span>
                      <span>{new Date().toISOString().replace('T', ' ').substring(0, 19)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>주문번호:</span>
                      <span># {Math.floor(Math.random() * 90000) + 10000}</span>
                    </div>
                    <div className="flex justify-between text-rose-500">
                      <span>당첨구분:</span>
                      <span>소금 커피 골든벨 당첨 🔔</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2.5 text-[10px] font-bold border-b border-dashed border-zinc-300 pb-4">
                    <div className="flex justify-between text-zinc-400 font-extrabold text-[9px] uppercase">
                      <span>상품명 [QTY]</span>
                      <span>금액</span>
                    </div>
                    <div className="flex justify-between text-zinc-800">
                      <span>💀 소금 아메리카노 [1]</span>
                      <span>₩ 55,000</span>
                    </div>
                    <div className="flex justify-between text-zinc-800">
                      <span>💖 동료들의 사랑/박수 [{playerCount - 1}]</span>
                      <span>₩ 0 (Priceless)</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2 text-xs font-black pt-2 text-center">
                    <div className="flex justify-between text-rose-600 border-b border-dashed border-zinc-200 pb-2">
                      <span>최종 결제자:</span>
                      <span>{looserName} 💸</span>
                    </div>
                    <p className="text-[10px] text-zinc-500 mt-2 font-black leading-relaxed">
                      "오늘 커피는 {looserName}님이 시원하게 쏘십니다! 다들 감사히 잘 먹겠습니다! 😍☕"
                    </p>
                  </div>

                  <div className="flex flex-col gap-2 mt-4 pt-4 border-t border-zinc-200">
                    <button 
                      onClick={handleKakaoShareReceipt}
                      className="w-full py-3 bg-[#FEE500] hover:bg-[#FDD000] text-zinc-900 font-black text-xs rounded-2xl flex items-center justify-center gap-2 cursor-pointer transition-colors shadow-xs"
                    >
                      💬 단톡방에 골든벨 박제하기
                    </button>
                    <button 
                      onClick={() => initCoffeeGame(playerCount)}
                      className="w-full py-3 bg-zinc-800 hover:bg-zinc-900 text-white font-black text-xs rounded-2xl cursor-pointer transition-colors shadow-xs"
                    >
                      🔄 한 판 더 하기!
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* 3. 모달 하단 퀵 액션 */}
            <div className="bg-zinc-50/80 border-t border-zinc-200 p-4 shrink-0 flex items-center justify-between gap-4 text-xs font-bold text-zinc-500 sm:px-6">
              <span className="text-rose-500 flex items-center gap-1 font-black">⚡ 엎어진 커피컵들 중 소금 폭탄 아메리카노 💀가 숨겨져 있습니다! 한 명씩 터치하세요!</span>
              <button 
                onClick={() => setIsCoffeeGameOpen(false)}
                className="px-4 py-2 bg-zinc-800 hover:bg-zinc-900 text-white rounded-lg transition-colors cursor-pointer shrink-0"
              >
                닫기
              </button>
            </div>

          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="w-full text-center py-5 text-xs text-zinc-400 border-t border-zinc-100">
        © 2026 아무거나 버스터 (Amugeona Buster)
      </footer>

      {/* PWA Install Banner */}
      {showInstallBanner && (
        <div className="fixed bottom-4 left-4 right-4 sm:left-auto sm:right-4 sm:w-96 bg-white/95 backdrop-blur-md border border-zinc-200 p-4 rounded-2xl shadow-2xl z-[9999] flex flex-col gap-3 transition-all duration-300">
          <div className="flex items-start justify-between">
            <div className="flex gap-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-rose-500 to-orange-500 flex items-center justify-center text-white text-lg font-black shadow-md shrink-0">
                🍱
              </div>
              <div>
                <h4 className="text-sm font-bold text-zinc-900">아무거나 버스터 앱 설치</h4>
                <p className="text-[11px] text-zinc-500 mt-0.5">바탕화면에 추가하여 1초 만에 식단을 확인하세요!</p>
              </div>
            </div>
            <button 
              onClick={handleDismissInstall}
              className="text-zinc-400 hover:text-zinc-600 p-1 cursor-pointer transition-colors text-sm"
            >
              ✕
            </button>
          </div>

          {isIOS ? (
            <div className="bg-zinc-50 border border-zinc-100 rounded-xl p-3 text-[11px] text-zinc-600 leading-relaxed font-medium">
              💡 <span className="font-bold text-rose-500">아이폰(Safari) 설치 방법:</span><br />
              하단 공유 버튼 <span className="font-bold text-zinc-800">📤(공유)</span>을 누르고 아래로 스크롤하여 <span className="font-bold text-zinc-800">‘홈 화면에 추가’</span>를 클릭하세요.
            </div>
          ) : (
            <div className="flex gap-2 justify-end mt-1">
              <button 
                onClick={handleDismissInstall}
                className="px-3 py-1.5 text-xs font-bold text-zinc-500 hover:bg-zinc-100 rounded-lg cursor-pointer transition-colors"
              >
                나중에
              </button>
              <button 
                onClick={handleInstallClick}
                className="px-4 py-1.5 text-xs font-black text-white bg-rose-500 hover:bg-rose-600 rounded-lg cursor-pointer transition-colors shadow-xs shadow-rose-200"
              >
                앱 설치하기
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default App;
