import { useCallback, useEffect, useState } from 'react';
import { getCustomerStats } from '../services/client.js';

/**
 * Loads the dashboard statistics. Earlier data stays in `data` while a retry
 * loads, and a response that arrives after the component unmounts is ignored.
 */
const useCustomerStats = () => {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    let ignore = false;
    setState((current) => ({ ...current, loading: true, error: null }));
    getCustomerStats()
      .then((res) => {
        if (!ignore) {
          setState({ data: res.data, loading: false, error: null });
        }
      })
      .catch((error) => {
        if (!ignore) {
          setState((current) => ({ ...current, loading: false, error }));
        }
      });
    return () => {
      ignore = true;
    };
  }, [reloadCount]);

  const refetch = useCallback(() => setReloadCount((count) => count + 1), []);

  return { ...state, refetch };
};

export default useCustomerStats;
