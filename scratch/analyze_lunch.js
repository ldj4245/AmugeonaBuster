const https = require('https');

const targetUrl = 'https://welplan.pmh.codes/takein/20260601/all';
const cookieJson = '[{"id":"REST000039","name":"DSR","vendor":"welstory"}]';
const encodedCookie = encodeURIComponent(cookieJson);

const options = {
  headers: {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Cookie': 'welplan_restaurants=' + encodedCookie
  }
};

https.get(targetUrl, options, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    const match = data.match(/menus:\s*(\[[\s\S]*?\])\s*,\s*(date|time):/);
    if (match) {
      try {
        const rawJson = match[1];
        const vm = require('vm');
        const sandbox = {};
        vm.createContext(sandbox);
        vm.runInContext(`var parsed = ${rawJson};`, sandbox);
        const menus = sandbox.parsed;

        console.log("=========================================");
        console.log("🍱 [DSR 타워 오늘(6월 1일) 실제 점심 메뉴 분석]");
        console.log("=========================================\n");

        // 1. 점심(mealTimeId === '2' 또는 '점심'에 해당하는 것)만 필터링
        // mealTimeId: "1"=아침, "2"=점심, "3"=저녁, "4"=야식
        const lunchMenus = menus.filter(item => item.mealTimeId === '2');

        console.log(`* 웰플랜 응답 중 점심 식단 개수: ${lunchMenus.length}개 (전체 ${menus.length}개 중)`);
        
        console.log("\n--- [점심 식단 전체 목록 (필터링 전)] ---");
        lunchMenus.forEach((item, idx) => {
          console.log(`[${idx+1}] ${item.name} (hallNo: ${item.hallNo})`);
        });

        // 2. 중복 분석 (메뉴 이름 기준)
        const nameCount = {};
        lunchMenus.forEach(item => {
          nameCount[item.name] = (nameCount[item.name] || 0) + 1;
        });

        console.log("\n--- [중복(겹치는) 점심 메뉴 현황] ---");
        let hasDuplicates = false;
        Object.keys(nameCount).forEach(name => {
          if (nameCount[name] > 1) {
            hasDuplicates = true;
            console.log(`⚠️ '${name}' 메뉴가 ${nameCount[name]}번 중복 존재합니다.`);
          }
        });
        if (!hasDuplicates) {
          console.log("중복 없음");
        }

      } catch (e) {
        console.error("오류:", e.message);
      }
    } else {
      console.log("식단을 찾을 수 없음");
    }
  });
});
