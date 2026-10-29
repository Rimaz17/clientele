import { useEffect, useState } from 'react';

// Returns the value once it has stopped changing for `delay` milliseconds,
// for example so typing in a search box sends one request instead of many.
const useDebouncedValue = (value, delay) => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timeout);
  }, [value, delay]);

  return debouncedValue;
};

export default useDebouncedValue;
