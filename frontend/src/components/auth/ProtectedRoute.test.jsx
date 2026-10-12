import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import * as AuthContextModule from '../../context/AuthContext';

describe('ProtectedRoute component', () => {
  it('redirects unauthenticated users to /login', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      loading: false
    });

    render(
      <MemoryRouter initialEntries={['/overview']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/overview" element={<div>Protected Dashboard</div>} />
          </Route>
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('renders child outlet when user is authenticated with doctor role', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { id: 'doc-1', role: 'DOCTOR', email: 'doctor@careflow.ai' },
      loading: false
    });

    render(
      <MemoryRouter initialEntries={['/overview']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/overview" element={<div>Doctor Dashboard</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Doctor Dashboard')).toBeInTheDocument();
  });

  it('blocks non-admin users from /audit and redirects to /overview', () => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: { id: 'nurse-1', role: 'NURSE', email: 'nurse@careflow.ai' },
      loading: false
    });

    render(
      <MemoryRouter initialEntries={['/audit']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/audit" element={<div>Admin Audit</div>} />
          </Route>
          <Route path="/overview" element={<div>Nurse Overview</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Nurse Overview')).toBeInTheDocument();
    expect(screen.queryByText('Admin Audit')).not.toBeInTheDocument();
  });
});
