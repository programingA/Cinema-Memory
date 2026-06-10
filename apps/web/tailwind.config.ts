import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        auditorium: "#11100f",
        velvet: "#7f1d1d",
        projector: "#f6d365",
        reel: "#e7e5e4",
        nitrate: "#292524"
      },
      boxShadow: {
        screen: "0 20px 80px rgba(246, 211, 101, 0.18)",
        reel: "inset 0 1px 0 rgba(255,255,255,0.18), 0 16px 50px rgba(0,0,0,0.28)"
      },
      fontFamily: {
        sans: ["system-ui", "sans-serif"]
      }
    }
  },
  plugins: []
};

export default config;
