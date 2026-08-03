import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import Dashboard from './Dashboard.jsx';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import { AuthProvider } from '@context/AuthContext.jsx';

function renderWithProviders(ui) {
  return render(
    <ThemeProvider>
      <AuthProvider>
        <MemoryRouter>
          {ui}
        </MemoryRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

describe('Dashboard', () => {
  it('shows summary cards', () => {
    renderWithProviders(<Dashboard />);

    expect(
      screen.getByRole('heading', { name: /dashboard/i })
    ).toBeInTheDocument();
  });
});