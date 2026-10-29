import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getCustomerStats } from '../services/client.js';
import useCustomerStats from './useCustomerStats.js';

vi.mock('../services/client.js', () => ({ getCustomerStats: vi.fn() }));

const stats = (totalCustomers) => ({ data: { totalCustomers } });

describe('useCustomerStats', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('starts loading and then returns the stats', async () => {
    getCustomerStats.mockResolvedValue(stats(132));

    const { result } = renderHook(() => useCustomerStats());

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual({ totalCustomers: 132 });
    expect(result.current.error).toBeNull();
  });

  it('reports an error and loads again on refetch', async () => {
    const error = { response: { status: 500 } };
    getCustomerStats.mockRejectedValueOnce(error).mockResolvedValueOnce(stats(5));
    const { result } = renderHook(() => useCustomerStats());
    await waitFor(() => expect(result.current.error).toBe(error));
    expect(result.current.data).toBeNull();

    act(() => result.current.refetch());

    await waitFor(() => expect(result.current.data).toEqual({ totalCustomers: 5 }));
    expect(result.current.error).toBeNull();
    expect(getCustomerStats).toHaveBeenCalledTimes(2);
  });

  it('ignores a response that arrives after unmounting', async () => {
    let resolve;
    getCustomerStats.mockReturnValue(new Promise((r) => (resolve = r)));
    const { result, unmount } = renderHook(() => useCustomerStats());

    unmount();
    await act(async () => resolve(stats(1)));

    expect(result.current.data).toBeNull();
  });
});
