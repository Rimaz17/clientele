import { ChakraProvider } from '@chakra-ui/react';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import AuthProvider from '../context/AuthContext.jsx';
import SidebarWithHeader from './SideBar.jsx';
import { fakeToken } from '../../test/fakeToken.js';

const renderAt = (path) =>
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={[path]}>
          <SidebarWithHeader>
            <p>Page</p>
          </SidebarWithHeader>
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );

describe('SidebarWithHeader', () => {
  beforeEach(() => {
    localStorage.setItem('access_token', fakeToken({ sub: 'customer1@example.com' }));
  });

  afterEach(() => {
    localStorage.clear();
  });

  // The desktop sidebar is display: none at the base breakpoint, which jsdom
  // applies, so its links are queried with hidden: true.
  it('links to the dashboard and the customers, and to nothing else', () => {
    renderAt('/dashboard');

    const nav = screen.getByRole('link', { name: 'Dashboard', hidden: true }).parentElement;
    expect(
      within(nav)
        .getAllByRole('link', { hidden: true })
        .map((link) => link.getAttribute('href')),
    ).toEqual(['/dashboard', '/dashboard/customers']);
    expect(screen.queryByRole('link', { name: 'Settings', hidden: true })).not.toBeInTheDocument();
  });

  it('marks the link to the current page', () => {
    renderAt('/dashboard/customers');

    expect(screen.getByRole('link', { name: 'Customers', hidden: true })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.getByRole('link', { name: 'Dashboard', hidden: true })).not.toHaveAttribute(
      'aria-current',
    );
  });
});
