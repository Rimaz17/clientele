import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// jsdom has no ResizeObserver, which Recharts' ResponsiveContainer needs, and
// no layout. The stub reports a fixed size, so charts render in tests.
class ResizeObserverStub {
  constructor(callback) {
    this.callback = callback;
  }
  observe(target) {
    this.callback([{ target, contentRect: { width: 600, height: 260 } }], this);
  }
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub;

// jsdom has no matchMedia either, which Chakra's usePrefersReducedMotion reads.
// Every media query reports "no match", like a default desktop browser.
window.matchMedia ??= (query) => ({
  matches: false,
  media: query,
  onchange: null,
  addEventListener() {},
  removeEventListener() {},
  addListener() {},
  removeListener() {},
  dispatchEvent: () => false,
});

afterEach(() => {
  cleanup();
});
