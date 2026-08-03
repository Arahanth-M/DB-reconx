// TICKET-ADV124 — ThemeProvider: context flips data-theme; CSS owns colours.
import React, { createContext, useContext, useEffect, useState } from 'react';

const ThemeContext = createContext({ theme: 'light', toggle: () => {} });

export function ThemeProvider({ children }) {
  // TODO(TICKET-ADV124): lazy-init from localStorage('reconx-theme') — fall back
  //                     to 'light' if nothing is stored.
  const [theme, setTheme] = useState(() => {
  return localStorage.getItem('reconx-theme') || 'light';
});

  // TODO(TICKET-ADV124): useEffect that:
  //                     1. sets document.documentElement.dataset.theme = theme
  //                     2. persists `theme` to localStorage on every change.
  useEffect(() => {
  document.documentElement.dataset.theme = theme;
  localStorage.setItem('reconx-theme', theme);
}, [theme]);

  const toggle = () => {
  setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
};

  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
