import React, { useState } from 'react';
import { useStore } from '../store/useStore';
import { Activity, Bell, Bot, Sun, Moon, User, Search, ShieldAlert, Check } from 'lucide-react';

export default function Navbar() {
  const { currentUser, demoUsers, switchUser, toggleCopilot, isCopilotOpen, theme, toggleTheme, unreadNotifications } = useStore();
  const [showRoleDropdown, setShowRoleDropdown] = useState(false);

  return (
    <header className="sticky top-0 z-30 bg-slate-900/80 backdrop-blur-md border-b border-slate-800 px-4 py-3 flex items-center justify-between">
      {/* Brand & Platform Identity */}
      <div className="flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-teal-500 to-emerald-400 flex items-center justify-center shadow-lg shadow-teal-500/20">
          <Activity className="w-6 h-6 text-slate-950 font-bold" />
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <span className="font-bold text-lg text-slate-100 tracking-tight">CareFlow</span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-teal-500/10 text-teal-400 border border-teal-500/20">AI Continuity</span>
          </div>
          <p className="text-xs text-slate-400">Append-Only Patient Timeline Engine</p>
        </div>
      </div>

      {/* Center Search & Context */}
      <div className="hidden md:flex items-center w-72 relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3" />
        <input 
          type="text" 
          placeholder="Search patient or MRN (e.g. MRN-2026-001)..." 
          className="w-full pl-9 pr-4 py-1.5 bg-slate-800/80 border border-slate-700/60 rounded-lg text-xs text-slate-200 placeholder-slate-400 focus:outline-none focus:border-teal-500 focus:ring-1 focus:ring-teal-500 transition-all"
        />
      </div>

      {/* Right Controls & Role Switcher */}
      <div className="flex items-center space-x-3">
        {/* CareFlow Copilot Trigger */}
        <button
          onClick={toggleCopilot}
          className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
            isCopilotOpen 
              ? 'bg-teal-500 text-slate-950 shadow-md shadow-teal-500/30' 
              : 'bg-teal-500/10 text-teal-300 border border-teal-500/30 hover:bg-teal-500/20'
          }`}
        >
          <Bot className="w-4 h-4" />
          <span className="hidden sm:inline">CareFlow Copilot</span>
        </button>

        {/* Notifications Bell */}
        <button className="relative p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800/80 transition-colors">
          <Bell className="w-4 h-4" />
          {unreadNotifications > 0 && (
            <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-rose-500 ring-2 ring-slate-900 animate-pulse" />
          )}
        </button>

        {/* Dark/Light Mode */}
        <button 
          onClick={toggleTheme} 
          className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800/80 transition-colors"
          title="Toggle Theme"
        >
          {theme === 'dark' ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-indigo-400" />}
        </button>

        {/* Role Switcher Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowRoleDropdown(!showRoleDropdown)}
            className="flex items-center space-x-2 pl-3 pr-2 py-1.5 bg-slate-800 border border-slate-700 rounded-lg hover:border-slate-600 transition-all"
          >
            <div className="w-6 h-6 rounded-full bg-slate-700 flex items-center justify-center text-teal-400 font-bold text-xs">
              {currentUser.fullName.charAt(0)}
            </div>
            <div className="text-left hidden sm:block">
              <p className="text-xs font-semibold text-slate-200 leading-tight">{currentUser.fullName}</p>
              <p className="text-[10px] font-medium text-teal-400 leading-tight">{currentUser.role}</p>
            </div>
          </button>

          {showRoleDropdown && (
            <div className="absolute right-0 mt-2 w-64 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50 p-2 space-y-1">
              <div className="px-3 py-1.5 border-b border-slate-800 mb-1">
                <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Select Demo Role Perspective</p>
              </div>
              {demoUsers.map((user) => (
                <button
                  key={user.id}
                  onClick={() => {
                    switchUser(user);
                    setShowRoleDropdown(false);
                  }}
                  className={`w-full text-left px-3 py-2 rounded-lg text-xs flex items-center justify-between transition-colors ${
                    currentUser.id === user.id ? 'bg-teal-500/15 text-teal-300 font-medium' : 'text-slate-300 hover:bg-slate-800'
                  }`}
                >
                  <div>
                    <p className="font-medium text-slate-100">{user.fullName}</p>
                    <p className="text-[10px] text-slate-400">{user.role} ({user.email})</p>
                  </div>
                  {currentUser.id === user.id && <Check className="w-3.5 h-3.5 text-teal-400" />}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
