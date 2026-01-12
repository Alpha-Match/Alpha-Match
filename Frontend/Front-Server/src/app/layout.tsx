import type {Metadata} from "next";
import "@/app/globals.css";
import Providers from "@/app/providers";
import {Notification} from "@/components/utils/Notification";
import {ThemeManager} from "@/components/layout/ThemeManager";

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
          <ThemeManager />
          {children}
          <Notification />
        </Providers>
      </body>
    </html>
  );
}
