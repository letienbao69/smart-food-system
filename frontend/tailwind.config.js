/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', '-apple-system', 'sans-serif'],
        display: ['"Fraunces"', 'Georgia', 'serif'],
        serif: ['"Cormorant Garamond"', 'Georgia', 'serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      colors: {
        // Trung tính (xám) — sạch, tươi sáng
        ink: {
          50:  '#fafafa',
          100: '#f4f5f5',
          200: '#e6e8e8',
          300: '#d2d6d6',
          400: '#9ca3a3',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
          950: '#0a0e16',
        },
        // Xanh lá tươi sáng — màu thương hiệu chính
        accent: {
          50:  '#ecfdf3',
          100: '#d1fadf',
          200: '#a6f3c4',
          300: '#6ee7a0',
          400: '#34d27a',
          500: '#1a8d46',
          600: '#15803d',
          700: '#137334',
          800: '#13602e',
          900: '#114f28',
        },
        // Vàng champagne — điểm nhấn nhẹ (tươi hơn)
        gold: {
          50:  '#fefce8',
          100: '#fef6c7',
          200: '#fde889',
          300: '#fbd54f',
          400: '#f5c518',
          500: '#e0ad1a',
          600: '#c2890f',
          700: '#9a6410',
          800: '#7f5113',
          900: '#6c4415',
        },
        // Promo — warm terracotta (replaces bright orange)
        promo: {
          50:  '#fdf4ef',
          100: '#fae3d6',
          400: '#dd8a5f',
          500: '#c96f41',
          600: '#ad5832',
        },
        warm: {
          50:  '#f7f4ed',
          100: '#efeae0',
        },
        success: {
          50:  '#f1f6ef',
          500: '#5b8a4f',
          600: '#477038',
          700: '#37592c',
        },
        danger: {
          50:  '#fbf2ef',
          500: '#bf5340',
          600: '#a23d2c',
          700: '#822f21',
        },
      },
      borderRadius: {
        DEFAULT: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '20px',
        '3xl': '24px',
      },
      boxShadow: {
        'subtle': '0 1px 2px 0 rgb(60 40 20 / 0.04), 0 1px 3px 0 rgb(60 40 20 / 0.06)',
        'card': '0 2px 10px -2px rgb(60 40 20 / 0.08), 0 6px 20px -6px rgb(60 40 20 / 0.10)',
        'pop': '0 18px 50px -12px rgb(40 30 15 / 0.22), 0 6px 16px -6px rgb(40 30 15 / 0.12)',
        'glow': '0 0 0 1px rgb(179 131 58 / 0.18), 0 8px 24px -6px rgb(87 124 71 / 0.18)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        'scale-in': 'scaleIn 0.2s ease-out',
        'bounce-in': 'bounceIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.96)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        bounceIn: {
          '0%': { opacity: '0', transform: 'scale(0.3)' },
          '50%': { transform: 'scale(1.05)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
    },
  },
  plugins: [],
}
