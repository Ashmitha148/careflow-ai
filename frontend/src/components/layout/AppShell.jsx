import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import CopilotSidePanel from "../clinical/CopilotSidePanel";

export default function AppShell() {
  return (
    <div className="min-h-screen bg-[var(--bg-primary)] text-[var(--text-primary)]">
      <Sidebar />

      <div className="lg:pl-[248px]">
        <Topbar />

        <main className="min-h-[calc(100vh-72px)] px-4 pb-8 pt-4 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>

      <CopilotSidePanel />
    </div>
  );
}
