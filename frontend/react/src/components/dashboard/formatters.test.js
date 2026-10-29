import { describe, expect, it } from 'vitest';
import {
  formatAverage,
  formatCount,
  formatLongMonth,
  formatPercent,
  formatShortMonth,
  plural,
} from './formatters.js';

describe('dashboard formatters', () => {
  it('groups thousands in counts', () => {
    expect(formatCount(0)).toBe('0');
    expect(formatCount(12840)).toBe('12,840');
  });

  it('shows the average to one decimal, or a dash without one', () => {
    expect(formatAverage(37.4)).toBe('37.4');
    expect(formatAverage(40)).toBe('40.0');
    expect(formatAverage(null)).toBe('–');
  });

  it('rounds percentages and handles an empty whole', () => {
    expect(formatPercent(62, 132)).toBe('47%');
    expect(formatPercent(0, 0)).toBe('0%');
  });

  it('formats API months in UTC whatever the local time zone', () => {
    expect(formatShortMonth('2026-01')).toBe('Jan');
    expect(formatLongMonth('2025-12')).toBe('December 2025');
  });

  it('pluralises a counted noun', () => {
    expect(plural(1, 'sign-up')).toBe('1 sign-up');
    expect(plural(1200, 'sign-up')).toBe('1,200 sign-ups');
    expect(plural(2, 'customer')).toBe('2 customers');
  });
});
