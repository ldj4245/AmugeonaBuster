import { useEffect, useRef, useState } from 'react';
import { Compass, AlertCircle } from 'lucide-react';

interface Restaurant {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  phone?: string;
  category?: string;
  placeUrl?: string;
}

interface KakaoMapProps {
  matchedRestaurants: Restaurant[];
  location: string;
}

export function KakaoMap({ matchedRestaurants, location }: KakaoMapProps) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const apiKey = (import.meta as any).env.VITE_KAKAO_MAP_API_KEY;

    if (!apiKey || apiKey === 'YOUR_JS_API_KEY') {
      console.warn("Kakao JS API Key is missing or default. Falling back to Mock Map.");
      setHasError(true);
      setIsLoading(false);
      return;
    }

    // 2초 뒤에 맵이 로드되지 않았으면 에러 처리 (폴백 전환)
    const timeoutId = setTimeout(() => {
      const win = window as any;
      if (!win.kakao || !win.kakao.maps) {
        console.warn("Kakao Maps load timed out (2s limit). Falling back to Mock Map.");
        setHasError(true);
        setIsLoading(false);
      }
    }, 2000);

    // 스크립트가 이미 있는지 확인
    const existingScript = document.getElementById('kakao-map-sdk');
    if (existingScript) {
      initializeMap();
      clearTimeout(timeoutId);
      return;
    }

    const script = document.createElement('script');
    script.id = 'kakao-map-sdk';
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false`;
    script.async = true;

    script.onload = () => {
      const win = window as any;
      if (win.kakao && win.kakao.maps) {
        win.kakao.maps.load(() => {
          initializeMap();
          clearTimeout(timeoutId);
        });
      } else {
        setHasError(true);
        setIsLoading(false);
        clearTimeout(timeoutId);
      }
    };

    script.onerror = () => {
      console.error("Failed to load Kakao Maps script.");
      setHasError(true);
      setIsLoading(false);
      clearTimeout(timeoutId);
    };

    document.head.appendChild(script);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [matchedRestaurants]);

  const initializeMap = () => {
    if (!mapContainerRef.current) return;
    const win = window as any;
    if (!win.kakao || !win.kakao.maps) return;

    try {
      setIsLoading(false);
      setIsLoaded(true);
      setHasError(false);

      const container = mapContainerRef.current;
      
      let avgLat = 0;
      let avgLng = 0;
      let validCoordsCount = 0;

      matchedRestaurants.forEach((res) => {
        if (res.latitude && res.longitude) {
          avgLat += res.latitude;
          avgLng += res.longitude;
          validCoordsCount++;
        }
      });

      if (validCoordsCount > 0) {
        avgLat /= validCoordsCount;
        avgLng /= validCoordsCount;
      } else {
        avgLat = 37.5665;
        avgLng = 126.9780;
      }

      const options = {
        center: new win.kakao.maps.LatLng(avgLat, avgLng),
        level: 4
      };

      const map = new win.kakao.maps.Map(container, options);
      const bounds = new win.kakao.maps.LatLngBounds();

      const mapTypeControl = new win.kakao.maps.MapTypeControl();
      map.addControl(mapTypeControl, win.kakao.maps.ControlPosition.TOPRIGHT);

      const zoomControl = new win.kakao.maps.ZoomControl();
      map.addControl(zoomControl, win.kakao.maps.ControlPosition.RIGHT);

      matchedRestaurants.forEach((res, index) => {
        if (!res.latitude || !res.longitude) return;

        const markerPosition = new win.kakao.maps.LatLng(res.latitude, res.longitude);
        bounds.extend(markerPosition);

        const marker = new win.kakao.maps.Marker({
          position: markerPosition,
          map: map
        });

        const content = `
          <div style="
            padding: 12px;
            width: 220px;
            background: #ffffff;
            border-radius: 14px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.12);
            border: 1px solid #e5e7eb;
            text-align: left;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
          ">
            <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
              <span style="
                background: #f43f5e;
                color: white;
                font-weight: 800;
                font-size: 10px;
                min-width: 18px;
                height: 18px;
                border-radius: 5px;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 0 3px;
              ">${index + 1}</span>
              <strong style="color: #111827; font-size: 13px; font-weight: 700; line-height: 1.3;">${res.name}</strong>
            </div>
            ${res.category ? `<div style="display: inline-block; background: #fff7ed; color: #ea580c; font-size: 9px; font-weight: 700; padding: 2px 6px; border-radius: 10px; margin-bottom: 5px; border: 1px solid #fed7aa;">${res.category}</div>` : ''}
            <div style="font-size: 10px; color: #6b7280; margin-bottom: ${res.phone || res.placeUrl ? '6px' : '0'}; line-height: 1.4;">📍 ${res.address}</div>
            ${res.phone ? `<div style="font-size: 10px; color: #ef4444; font-weight: 600; margin-bottom: ${res.placeUrl ? '6px' : '0'};">📞 ${res.phone}</div>` : ''}
            ${res.placeUrl ? `<a href="${res.placeUrl}" target="_blank" style="
              display: block;
              background: #fee2e2;
              color: #b91c1c;
              font-size: 10px;
              font-weight: 700;
              text-align: center;
              padding: 5px 0;
              border-radius: 8px;
              text-decoration: none;
              margin-top: 2px;
            ">🗺️ 카카오맵에서 보기</a>` : ''}
          </div>
        `;

        const infowindow = new win.kakao.maps.InfoWindow({
          content: content,
          removable: true
        });

        win.kakao.maps.event.addListener(marker, 'click', () => {
          infowindow.open(map, marker);
        });
      });

      if (validCoordsCount > 1) {
        map.setBounds(bounds);
      }

    } catch (e) {
      console.error("Error initializing Kakao Map object:", e);
      setHasError(true);
      setIsLoading(false);
    }
  };

  const renderMockMap = () => {
    return (
      <div className="w-full bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl rounded-3xl p-6 relative overflow-hidden">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-sm font-bold text-slate-700 flex items-center gap-1.5">
            <Compass className="w-4 h-4 text-rose-500" />
            📍 {location} 맛집 매칭 지도
          </h3>
          
          <div className="flex items-center gap-1 bg-amber-50 border border-amber-200/50 text-[10px] text-amber-600 px-2 py-0.5 rounded-full font-bold">
            <AlertCircle className="w-3 h-3 text-amber-500 shrink-0" />
            예비 모의 지도 전환됨
          </div>
        </div>
        
        <div className="w-full h-[220px] bg-slate-100 rounded-2xl border border-slate-200/60 relative overflow-hidden shadow-inner flex items-center justify-center">
          <div className="absolute inset-0 bg-[linear-gradient(to_right,#e2e8f0_1px,transparent_1px),linear-gradient(to_bottom,#e2e8f0_1px,transparent_1px)] bg-[size:24px_24px] opacity-40" />
          
          <div className="absolute top-[30%] left-0 w-full h-4 bg-white border-y border-slate-200/50 rotate-[3deg] pointer-events-none" />
          <div className="absolute top-[70%] left-0 w-full h-6 bg-white border-y border-slate-200/50 rotate-[-2deg] pointer-events-none" />
          <div className="absolute left-[40%] top-0 w-5 h-full bg-white border-x border-slate-200/50 rotate-[12deg] pointer-events-none" />

          <div className="absolute top-4 left-4 bg-white/80 border border-slate-200/50 px-2 py-1 rounded-lg text-[10px] font-bold text-slate-500 shadow-sm">
            {location} 반경 1km
          </div>

          {matchedRestaurants.map((res, index) => {
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
                <div className="absolute bottom-8 scale-0 group-hover:scale-100 bg-slate-800 text-white text-[9px] font-bold px-2 py-1 rounded shadow-lg whitespace-nowrap transition-all z-20">
                  {res.name}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  if (hasError) {
    return renderMockMap();
  }

  return (
    <div className="w-full bg-white/80 backdrop-blur-xl border border-white/60 shadow-xl rounded-3xl p-6 relative overflow-hidden">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-sm font-bold text-slate-700 flex items-center gap-1.5">
          <Compass className="w-4 h-4 text-rose-500" />
          📍 {location} 맛집 매칭 지도
        </h3>
        {isLoaded && (
          <div className="flex items-center gap-1 bg-emerald-50 border border-emerald-200/50 text-[10px] text-emerald-600 px-2.5 py-0.5 rounded-full font-bold shadow-sm">
            <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
            카카오 실지도 연동
          </div>
        )}
      </div>

      <div className="relative w-full h-[220px]">
        {isLoading && (
          <div className="absolute inset-0 bg-slate-50 border border-slate-200/60 rounded-2xl flex flex-col items-center justify-center gap-2 z-10">
            <div className="w-8 h-8 border-4 border-rose-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-[11px] text-slate-400 font-semibold">지도를 불러오는 중...</span>
          </div>
        )}

        <div 
          ref={mapContainerRef} 
          className="w-full h-full rounded-2xl border border-slate-200/60 shadow-inner overflow-hidden"
        />
      </div>
    </div>
  );
}
