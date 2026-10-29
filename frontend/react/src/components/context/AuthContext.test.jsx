import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { fakeToken } from '../../test/fakeToken.js';
import AuthProvider, { useAuth } from './AuthContext.jsx';

const renderAuth = () => renderHook(() => useAuth(), { wrapper: AuthProvider });

describe('AuthContext', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('reads the email and roles from a stored token on first render', () => {
    localStorage.setItem('access_token', fakeToken({ sub: 'alex@clientele.dev' }));

    const { result } = renderAuth();

    expect(result.current.customer).toEqual({
      username: 'alex@clientele.dev',
      roles: ['ROLE_USER'],
    });
    expect(result.current.isAdmin).toBe(false);
  });

  it('recognises admins from the token scopes', () => {
    localStorage.setItem(
      'access_token',
      fakeToken({ sub: 'admin@clientele.dev', scopes: ['ROLE_ADMIN'] }),
    );

    const { result } = renderAuth();

    expect(result.current.isAdmin).toBe(true);
  });

  it('compares the current email case-insensitively', () => {
    localStorage.setItem('access_token', fakeToken({ sub: 'alex@clientele.dev' }));

    const { result } = renderAuth();

    expect(result.current.isCurrentCustomer('Alex@Clientele.dev')).toBe(true);
    expect(result.current.isCurrentCustomer('jamila@clientele.dev')).toBe(false);
    expect(result.current.isCurrentCustomer(undefined)).toBe(false);
  });

  it('treats a missing or unreadable token as logged out', () => {
    localStorage.setItem('access_token', 'not-a-jwt');

    const { result } = renderAuth();

    expect(result.current.customer).toBeNull();
    expect(result.current.isAdmin).toBe(false);
    expect(result.current.isCurrentCustomer('alex@clientele.dev')).toBe(false);
    expect(result.current.isCustomerAuthenticated()).toBe(false);
  });

  it('stores a replacement token and updates the current customer', () => {
    localStorage.setItem('access_token', fakeToken({ sub: 'old@clientele.dev' }));
    const { result } = renderAuth();
    const newToken = fakeToken({ sub: 'new@clientele.dev' });

    act(() => result.current.storeToken(newToken));

    expect(localStorage.getItem('access_token')).toBe(newToken);
    expect(result.current.customer.username).toBe('new@clientele.dev');
  });
});
