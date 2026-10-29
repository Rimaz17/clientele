import { act, renderHook } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import useCustomerListParams, { readListParams, toApiParams } from './useCustomerListParams.js';

const renderParams = (url = '/dashboard/customers') =>
  renderHook(() => ({ list: useCustomerListParams(), location: useLocation() }), {
    wrapper: ({ children }) => <MemoryRouter initialEntries={[url]}>{children}</MemoryRouter>,
  });

describe('readListParams', () => {
  it('uses the defaults for an empty query string', () => {
    expect(readListParams(new URLSearchParams())).toEqual({
      page: 1,
      size: 20,
      sort: 'id,asc',
      search: '',
      gender: undefined,
      minAge: undefined,
      maxAge: undefined,
    });
  });

  it('reads every value from the query string', () => {
    const params = new URLSearchParams(
      'page=3&size=50&sort=age,desc&search=ali&gender=FEMALE&minAge=18&maxAge=40',
    );

    expect(readListParams(params)).toEqual({
      page: 3,
      size: 50,
      sort: 'age,desc',
      search: 'ali',
      gender: 'FEMALE',
      minAge: 18,
      maxAge: 40,
    });
  });

  it('ignores values the API would reject', () => {
    const params = new URLSearchParams(
      'page=0&size=999&sort=password&gender=OTHER&minAge=12&maxAge=abc',
    );

    expect(readListParams(params)).toMatchObject({
      page: 1,
      size: 20,
      sort: 'id,asc',
      gender: undefined,
      minAge: undefined,
      maxAge: undefined,
    });
  });

  it('drops a maximum age below the minimum age', () => {
    expect(readListParams(new URLSearchParams('minAge=40&maxAge=30'))).toMatchObject({
      minAge: 40,
      maxAge: undefined,
    });
  });
});

describe('toApiParams', () => {
  it('converts the page to the API, which counts from 0, and omits empty filters', () => {
    expect(toApiParams(readListParams(new URLSearchParams('page=2&search=ali')))).toEqual({
      page: 1,
      size: 20,
      sort: 'id,asc',
      search: 'ali',
    });
  });
});

describe('useCustomerListParams', () => {
  it('writes changes to the URL and goes back to the first page', () => {
    const { result } = renderParams('/dashboard/customers?page=4');

    act(() => result.current.list.updateParams({ gender: 'MALE' }));

    expect(result.current.location.search).toBe('?gender=MALE');
    expect(result.current.list.params.page).toBe(1);
    expect(result.current.list.hasFilters).toBe(true);
  });

  it('keeps the filters when only the page changes', () => {
    const { result } = renderParams('/dashboard/customers?search=ali');

    act(() => result.current.list.updateParams({ page: 3 }));

    expect(result.current.location.search).toBe('?search=ali&page=3');
  });

  it('leaves defaults out of the URL', () => {
    const { result } = renderParams('/dashboard/customers?sort=name,asc&size=50');

    act(() => result.current.list.updateParams({ sort: 'id,asc', size: 20 }));

    expect(result.current.location.search).toBe('');
  });

  it('clears the filters but keeps the sort and page size', () => {
    const { result } = renderParams(
      '/dashboard/customers?search=ali&gender=FEMALE&minAge=20&maxAge=30&sort=name,desc&size=10&page=2',
    );

    act(() => result.current.list.clearFilters());

    expect(result.current.location.search).toBe('?sort=name%2Cdesc&size=10');
    expect(result.current.list.hasFilters).toBe(false);
  });
});
