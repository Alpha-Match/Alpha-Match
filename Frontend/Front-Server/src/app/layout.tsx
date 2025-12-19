import type { Metadata } from "next";
import "./globals.css";
import Providers from "./providers";
import { AppInitializer } from "../components/AppInitializer";
import { Notification } from "../components/Notification";

export const metadata: Metadata = {
  title: "Alpha-Match",
  description: "Headhunter-Recruit Matching System",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <Providers>
          <AppInitializer />
          {children}
          <Notification />
        </Providers>
      </body>
    </html>
  );
}
