import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_API_PROXY_TARGET = "https://cinema-memory-api-alb-225083117.us-east-1.elb.amazonaws.com";
const API_PROXY_TARGET = (process.env.API_PROXY_TARGET ?? DEFAULT_API_PROXY_TARGET).replace(/\/$/, "");
const ALLOW_INSECURE_API_PROXY = process.env.ALLOW_INSECURE_API_PROXY === "true";

if (process.env.NODE_ENV === "production" && API_PROXY_TARGET.startsWith("http://") && !ALLOW_INSECURE_API_PROXY) {
  throw new Error("API_PROXY_TARGET must use HTTPS in production unless ALLOW_INSECURE_API_PROXY=true.");
}

const securityHeaders = [
  {
    key: "Strict-Transport-Security",
    value: "max-age=31536000; includeSubDomains; preload"
  },
  {
    key: "X-Content-Type-Options",
    value: "nosniff"
  },
  {
    key: "X-Frame-Options",
    value: "DENY"
  },
  {
    key: "Referrer-Policy",
    value: "no-referrer"
  },
  {
    key: "Permissions-Policy",
    value: "camera=(), microphone=(), geolocation=()"
  }
];

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  turbopack: {
    root: join(__dirname, "../..")
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**"
      }
    ]
  },
  async rewrites() {
    return [
      {
        source: "/api/backend/:path*",
        destination: `${API_PROXY_TARGET}/:path*`
      },
      {
        source: "/login/oauth2/:path*",
        destination: `${API_PROXY_TARGET}/login/oauth2/:path*`
      }
    ];
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: securityHeaders
      }
    ];
  }
};

export default nextConfig;
