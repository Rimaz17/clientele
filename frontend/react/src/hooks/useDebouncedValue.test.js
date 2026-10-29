import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useDebouncedValue from './useDebouncedValue.js';

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns the initial value immediately', () => {
    const { result } = renderHook(() => useDebouncedValue('a', 300));

    expect(result.current).toBe('a');
  });

  it('waits until the value has not changed for the delay', () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 300), {
      initialProps: { value: 'a' },
    });

    rerender({ value: 'al' });
    act(() => vi.advanceTimersByTime(200));
    rerender({ value: 'ali' });
    act(() => vi.advanceTimersByTime(200));

    expect(result.current).toBe('a');

    act(() => vi.advanceTimersByTime(100));

    expect(result.current).toBe('ali');
  });
});
