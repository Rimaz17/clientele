import { useCallback, useEffect, useState } from 'react';
import { getCustomers } from '../services/client.js';
import { toApiParams } from './useCustomerListParams.js';

/**
 * Loads one page of customers for the given list params. The previous page
 * stays in `data` while the next one loads, so the list does not jump, and a
 * response that arrives after newer params is ignored.
 */
const useCustomers = (params) => {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [reloadCount, setReloadCount] = useState(0);
  // A string key, so a new params object with the same values does not refetch.
  const requestKey = JSON.stringify(toApiParams(params));

  useEffect(() => {
    let ignore = false;
    setState((current) => ({ ...current, loading: true, error: null }));
    getCustomers(JSON.parse(requestKey))
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
  }, [requestKey, reloadCount]);

  const refetch = useCallback(() => setReloadCount((count) => count + 1), []);

  return { ...state, refetch };
};

export default useCustomers;
