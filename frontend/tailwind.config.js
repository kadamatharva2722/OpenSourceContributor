/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#0D0D0D',
        paper: '#0D0D0D',
        surface: {
          DEFAULT: '#1A1A1A',
          container: '#201F1F',
          lowest: '#0E0E0E',
          low: '#1C1B1B',
          high: '#2A2A2A',
          highest: '#353534',
          dim: '#131313',
          bright: '#3A3939',
        },
        ink: {
          DEFAULT: '#F1ECE2',
          primary: '#F1ECE2',
          muted: '#A7A29A',
          outline: '#A88A7F',
          line: 'rgba(241, 236, 226, 0.18)',
        },
        primary: {
          DEFAULT: '#F06522', // Wax Orange
          container: '#F06522',
          hover: '#F4C13D',
          text: '#0D0D0D',
        },
        secondary: {
          DEFAULT: '#F4C13D', // Catalog Yellow
          container: '#BF9200',
          hover: '#FFD154',
          text: '#0D0D0D',
        },
        tertiary: {
          DEFAULT: '#5B86F2', // Cobalt Print
          container: '#618CF8',
          text: '#0D0D0D',
        },
        pink: {
          DEFAULT: '#F29BB8', // Sleeve Pink
          label: '#F29BB8',
        },
        'on-surface': '#F1ECE2',
        'on-surface-variant': '#A7A29A',
        'inverse-surface': '#E5E2E1',
        'inverse-on-surface': '#313030',
        'error-container': '#93000A',
        'on-error-container': '#FFDAD6',
      },
      fontFamily: {
        heading: ['Anton', 'sans-serif'],
        display: ['Anton', 'sans-serif'],
        sans: ['"Archivo Narrow"', 'Archivo', 'sans-serif'],
        body: ['"Archivo Narrow"', 'Archivo', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
        code: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '0px',
        none: '0px',
        sm: '0px',
        md: '0px',
        lg: '0px',
        xl: '0px',
        '2xl': '0px',
        full: '0px',
      },
      boxShadow: {
        sharp: '4px 4px 0px 0px #1A1A1A',
        'sharp-orange': '4px 4px 0px 0px #F06522',
        'sharp-yellow': '4px 4px 0px 0px #F4C13D',
        'sharp-blue': '4px 4px 0px 0px #5B86F2',
        'sharp-white': '4px 4px 0px 0px #F1ECE2',
      },
    },
  },
  plugins: [],
};
