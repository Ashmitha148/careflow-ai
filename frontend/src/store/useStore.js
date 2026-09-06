import { create } from 'zustand';

export const useStore = create((set, get) => ({
  activeTab: 'timeline', // 'timeline', 'replay', 'tasks', 'copilot', 'audit'
  selectedPatientId: 'b0000000-0000-0000-0000-000000000001',
  selectedEventType: null, // EventType filter
  isCopilotOpen: false,
  theme: 'dark',
  unreadNotifications: 0,
  latestNotification: null, // most recent socket-delivered notification (for the toast)
  realtimeStatus: 'disconnected', // 'connecting' | 'connected' | 'disconnected' | 'error'

  // Actions
  setActiveTab: (tab) => set({ activeTab: tab }),
  setSelectedPatientId: (patientId) => set({ selectedPatientId: patientId }),
  setSelectedEventType: (eventType) => set({ selectedEventType: eventType }),
  toggleCopilot: () => set((state) => ({ isCopilotOpen: !state.isCopilotOpen })),
  setRealtimeStatus: (status) => set({ realtimeStatus: status }),
  setUnreadNotifications: (count) => set({ unreadNotifications: count }),
  setLatestNotification: (notification) => set({ latestNotification: notification }),
  clearLatestNotification: () => set({ latestNotification: null }),
  toggleTheme: () => set((state) => {
    const nextTheme = state.theme === 'dark' ? 'light' : 'dark';
    if (nextTheme === 'dark') {
      document.documentElement.classList.add('dark');
      document.documentElement.classList.remove('light');
    } else {
      document.documentElement.classList.remove('dark');
      document.documentElement.classList.add('light');
    }
    return { theme: nextTheme };
  })
}));
