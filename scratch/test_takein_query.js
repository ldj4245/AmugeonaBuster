const https = require('https');

const targetUrl = 'https://welplan.pmh.codes/takein?date=20260601';
const cookieJson = '[{"id":"REST000039","name":"DSR","vendor":"welstory"}]';
const encodedCookie = encodeURIComponent(cookieJson);

const options = {
  headers: {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Cookie': 'welplan_restaurants=' + encodedCookie
  }
};

https.get(targetUrl, options, (res) => {
  console.log(`Status Code: ${res.statusCode}`);
  console.log(`Headers:`, res.headers);
  
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    console.log(`Data length: ${data.length}`);
    const match = data.match(/menus:\s*(\[[\s\S]*?\])\s*,\s*(date|time):/);
    if (match) {
      console.log("Found menus match! length:", match[1].length);
    } else {
      console.log("menus: [...] not found in the HTML response!");
      console.log(data.substring(0, 1000)); // 처음 1000자 출력
    }
  });
}).on('error', (e) => {
  console.error("Error:", e);
});
