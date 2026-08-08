import { create } from 'zustand';

export const useStore = create((set, get) => ({
  // Demo Seed Users
  demoUsers: [
    { id: 'a0000000-0000-0000-0000-000000000002', fullName: 'Dr. Alistair Smith', email: 'dr.smith@careflow.ai', role: 'DOCTOR' },
    { id: 'a0000000-0000-0000-0000-000000000004', fullName: 'Nurse Sarah Jenkins', email: 'nurse.sarah@careflow.ai', role: 'NURSE' },
    { id: 'a0000000-0000-0000-0000-000000000006', fullName: 'Maria Santos', email: 'caregiver.maria@careflow.ai', role: 'CAREGIVER' },
    { id: 'a0000000-0000-0000-0000-000000000001', fullName: 'System Administrator', email: 'admin@careflow.ai', role: 'ADMIN' },
    { id: 'a0000000-0000-0000-0000-000000000007', fullName: 'David Chen (Family)', email: 'family.chen@careflow.ai', role: 'READ_ONLY' },
  ],
  
  currentUser: {
    id: 'a0000000-0000-0000-0000-000000000002',
    fullName: 'Dr. Alistair Smith',
    email: 'dr.smith@careflow.ai',
    role: 'DOCTOR'
  },
  
  activeTab: 'timeline', // 'timeline', 'replay', 'tasks', 'copilot', 'audit'
  selectedPatientId: 'b0000000-0000-0000-0000-000000000001',
  selectedEventType: null, // EventType filter
  isCopilotOpen: false,
  theme: 'dark',
  unreadNotifications: 3,

  // Actions
  switchUser: (user) => set({ currentUser: user }),
  setActiveTab: (tab) => set({ activeTab: tab }),
  setSelectedPatientId: (patientId) => set({ selectedPatientId: patientId }),
  setSelectedEventType: (eventType) => set({ selectedEventType: eventType }),
  toggleCopilot: () => set((state) => ({ isCopilotOpen: !state.isCopilotOpen })),
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
