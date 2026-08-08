/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        medical: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          500: '#14b8a6',
          600: '#0d9488',
          700: '#0f766e',
          900: '#134e4a',
        },
        slate: {
          850: '#1e293b',
          950: '#020617',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
