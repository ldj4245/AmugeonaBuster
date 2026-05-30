/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fff1f2',
          500: '#f43f5e', // 메인 브랜드 로즈 컬러
          600: '#e11d48',
        }
      }
    },
  },
  plugins: [],
}
