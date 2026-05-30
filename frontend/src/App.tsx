import React, { useState } from 'react';
import { Flame, Compass, Users, Sparkles, MapPin, ArrowRight } from 'lucide-react';

function App() {
  const [nickname, setNickname] = useState('');
  const [location, setLocation] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const handleCreateRoom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nickname || !location) return;
    setIsCreating(true);
    setTimeout(() => {
      alert(`🎉 방이 성공적으로 개설되었습니다! (방장: ${nickname}, 장소: ${location})\n추대 코드는 ROOM-7392 입니다!`);
      setIsCreating(false);
    }, 1200);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-rose-50 via-slate-50 to-rose-100 flex flex-col justify-between relative overflow-hidden">
      {/* Background Decorative Gradients */}
      <div className="absolute top-[-20%] left-[-20%] w-[60%] h-[60%] rounded-full bg-rose-200/40 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-rose-300/30 blur-[100px] pointer-events-none" />

      {/* Header */}
      <header className="max-w-6xl mx-auto w-full px-6 py-6 flex items-center justify-between z-10">
        <div className="flex items-center gap-2">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-rose-400 flex items-center justify-center text-white shadow-lg shadow-rose-500/20">
            <Flame className="w-5 h-5 animate-pulse" />
          </div>
          <span className="text-xl font-bold tracking-tight text-slate-800">
            오늘 뭐 먹지<span className="text-brand-500 font-extrabold">?</span>
          </span>
        </div>
        <div className="bg-white/80 backdrop-blur-md px-4 py-1.5 rounded-full border border-slate-200/80 text-xs font-semibold text-rose-600 flex items-center gap-1.5 shadow-sm">
          <Sparkles className="w-3.5 h-3.5" /> Github Student Pack Active
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto w-full px-6 py-12 grid md:grid-cols-12 gap-12 items-center flex-grow z-10">
        {/* Left Side: Copywriting */}
        <div className="md:col-span-7 flex flex-col gap-6 text-center md:text-left">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-rose-100/80 backdrop-blur-sm border border-rose-200/50 text-brand-600 text-xs font-bold rounded-full w-fit mx-auto md:mx-0 shadow-sm shadow-rose-100/10">
            🔥 10초 만에 끝내는 미식 의사결정
          </div>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 leading-tight tracking-tight">
            약속 메뉴 정할 땐,<br className="hidden sm:inline" />
            <span className="bg-gradient-to-r from-brand-500 via-rose-500 to-amber-500 bg-clip-text text-transparent">
              카드를 밀어서 스와이프!
            </span>
          </h1>
          <p className="text-slate-600 text-base sm:text-lg max-w-lg leading-relaxed mx-auto md:mx-0">
            더 이상의 "아무거나"는 거절합니다. 친구들과 실시간 방에 모여 각자 메뉴를 스와이프 하세요. 비토(비선호)를 방지하는 알고리즘으로 최상의 타협 맛집을 도출합니다.
          </p>

          {/* Feature Grid */}
          <div className="grid grid-cols-3 gap-4 mt-4 max-w-md mx-auto md:mx-0">
            <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm">
              <Users className="w-5 h-5 text-brand-500" />
              <span className="text-xs font-bold text-slate-700">실시간 대기실</span>
            </div>
            <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm">
              <Flame className="w-5 h-5 text-brand-500" />
              <span className="text-xs font-bold text-slate-700">메뉴 월드컵</span>
            </div>
            <div className="bg-white/60 backdrop-blur-sm p-4 rounded-2xl border border-slate-200/50 flex flex-col items-center md:items-start gap-2 shadow-sm">
              <Compass className="w-5 h-5 text-brand-500" />
              <span className="text-xs font-bold text-slate-700">지도 매칭 추천</span>
            </div>
          </div>
        </div>

        {/* Right Side: Setup Card Form (Premium Glassmorphism) */}
        <div className="md:col-span-5 w-full max-w-md mx-auto relative">
          <div className="absolute inset-0 bg-gradient-to-tr from-brand-600/10 to-amber-500/10 rounded-3xl blur-2xl pointer-events-none" />
          <div className="bg-white/80 backdrop-blur-xl border border-white/60 shadow-2xl shadow-rose-200/50 rounded-3xl p-8 relative z-10">
            <h2 className="text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-2">
              🚀 파티 시작하기
            </h2>
            <p className="text-xs text-slate-500 mt-1">방을 만들어 친구들에게 초대 링크를 공유하세요.</p>

            <form className="mt-8 flex flex-col gap-5" onSubmit={handleCreateRoom}>
              {/* Nickname Input */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-600 px-1">닉네임</label>
                <input
                  type="text"
                  required
                  placeholder="예: 김스프"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-white/50 backdrop-blur-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all font-medium"
                />
              </div>

              {/* Location Input */}
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-bold text-slate-600 px-1">약속 위치</label>
                <div className="relative">
                  <input
                    type="text"
                    required
                    placeholder="예: 강남역, 판교역"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 bg-white/50 backdrop-blur-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all font-medium"
                  />
                  <MapPin className="w-5 h-5 text-slate-400 absolute left-4 top-3.5" />
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={isCreating}
                className="w-full mt-2 py-4 rounded-xl bg-gradient-to-r from-brand-600 to-rose-500 hover:from-brand-600 hover:to-rose-600 text-white font-bold text-sm tracking-wide shadow-lg shadow-rose-500/25 hover:shadow-rose-600/35 transition-all duration-300 transform active:scale-[0.98] flex items-center justify-center gap-2 group disabled:opacity-75 disabled:pointer-events-none"
              >
                {isCreating ? (
                  <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    방 개설 및 초대코드 발급
                    <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full text-center py-6 text-xs text-slate-500 font-medium border-t border-slate-200/40 bg-white/20 backdrop-blur-sm z-10">
        © 2026 오늘 뭐 먹지? Project. Built with ⚡ Java Spring Boot & React.
      </footer>
    </div>
  );
}

export default App;
