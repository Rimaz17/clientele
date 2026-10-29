import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getCustomers } from '../services/client.js';
import useCustomers from './useCustomers.js';
import { readListParams } from './useCustomerListParams.js';

vi.mock('../services/client.js', () => ({ getCustomers: vi.fn() }));

const page = (names, pageNumber = 0) => ({
  data: {
    content: names.map((name, index) => ({ id: index + 1, name })),
    page: pageNumber,
    size: 20,
    totalElements: names.length,
    totalPages: 1,
  },
});

const params = (query) => readListParams(new URLSearchParams(query));

describe('useCustomers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests the page and filters from the API', async () => {
    getCustomers.mockResolvedValue(page(['Ann']));

    const { result } = renderHook(() => useCustomers(params('page=2&gender=FEMALE')));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(getCustomers).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      sort: 'createdAt,desc',
      gender: 'FEMALE',
    });
    expect(result.current.data.content).toEqual([{ id: 1, name: 'Ann' }]);
    expect(result.current.error).toBeNull();
  });

  it('does not refetch when the params keep the same values', async () => {
    getCustomers.mockResolvedValue(page(['Ann']));
    const { result, rerender } = renderHook(({ query }) => useCustomers(params(query)), {
      initialProps: { query: 'search=ali' },
    });
    await waitFor(() => expect(result.current.loading).toBe(false));

    rerender({ query: 'search=ali' });

    expect(getCustomers).toHaveBeenCalledTimes(1);
  });

  it('ignores a response that arrives after newer params', async () => {
    let resolveFirst;
    getCustomers
      .mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
      .mockResolvedValueOnce(page(['Bob'], 1));
    const { result, rerender } = renderHook(({ query }) => useCustomers(params(query)), {
      initialProps: { query: '' },
    });

    rerender({ query: 'page=2' });
    await waitFor(() => expect(result.current.data?.content).toEqual([{ id: 1, name: 'Bob' }]));
    await act(async () => resolveFirst(page(['Ann'])));

    expect(result.current.data.content).toEqual([{ id: 1, name: 'Bob' }]);
  });

  it('keeps the previous page and reports the error when a request fails', async () => {
    const error = { response: { status: 500, data: { message: 'An unexpected error occurred' } } };
    getCustomers.mockResolvedValueOnce(page(['Ann'])).mockRejectedValueOnce(error);
    const { result } = renderHook(() => useCustomers(params('')));
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.refetch());

    await waitFor(() => expect(result.current.error).toBe(error));
    expect(result.current.data.content).toEqual([{ id: 1, name: 'Ann' }]);
    expect(getCustomers).toHaveBeenCalledTimes(2);
  });
});
