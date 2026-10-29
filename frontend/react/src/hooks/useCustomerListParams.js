import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

export const PAGE_SIZES = [10, 20, 50];
export const DEFAULT_PAGE_SIZE = 20;
export const DEFAULT_SORT = 'id,asc';
export const MIN_AGE = 16;
export const MAX_AGE = 100;
export const MAX_SEARCH_LENGTH = 100;

// Mirrors the API's sort whitelist.
export const SORT_OPTIONS = [
  { value: 'id,asc', label: 'First added' },
  { value: 'id,desc', label: 'Recently added' },
  { value: 'name,asc', label: 'Name, A to Z' },
  { value: 'name,desc', label: 'Name, Z to A' },
  { value: 'email,asc', label: 'Email, A to Z' },
  { value: 'email,desc', label: 'Email, Z to A' },
  { value: 'age,asc', label: 'Age, youngest first' },
  { value: 'age,desc', label: 'Age, oldest first' },
];

const GENDERS = ['FEMALE', 'MALE'];

const parseInteger = (value) => (value !== null && /^\d+$/.test(value) ? Number(value) : undefined);

const parseAge = (value) => {
  const age = parseInteger(value);
  return age !== undefined && age >= MIN_AGE && age <= MAX_AGE ? age : undefined;
};

// Reads the list state from the URL. Values the API would reject are ignored,
// so an edited or outdated link still loads a list instead of an error.
export const readListParams = (searchParams) => {
  const page = parseInteger(searchParams.get('page'));
  const size = parseInteger(searchParams.get('size'));
  const sort = searchParams.get('sort');
  const gender = searchParams.get('gender');
  const minAge = parseAge(searchParams.get('minAge'));
  const maxAge = parseAge(searchParams.get('maxAge'));
  return {
    page: page !== undefined && page >= 1 ? page : 1,
    size: PAGE_SIZES.includes(size) ? size : DEFAULT_PAGE_SIZE,
    sort: SORT_OPTIONS.some((option) => option.value === sort) ? sort : DEFAULT_SORT,
    search: (searchParams.get('search') ?? '').trim().slice(0, MAX_SEARCH_LENGTH),
    gender: GENDERS.includes(gender) ? gender : undefined,
    minAge,
    maxAge: minAge !== undefined && maxAge !== undefined && maxAge < minAge ? undefined : maxAge,
  };
};

// The URL counts pages from 1 because people read it; the API counts from 0.
export const toApiParams = ({ page, size, sort, search, gender, minAge, maxAge }) => {
  const params = { page: page - 1, size, sort };
  if (search) params.search = search;
  if (gender) params.gender = gender;
  if (minAge !== undefined) params.minAge = minAge;
  if (maxAge !== undefined) params.maxAge = maxAge;
  return params;
};

export const hasActiveFilters = ({ search, gender, minAge, maxAge }) =>
  Boolean(search || gender || minAge !== undefined || maxAge !== undefined);

// Leaves out empty values and defaults, so the URL only shows what was chosen.
const toSearchParams = ({ page, size, sort, search, gender, minAge, maxAge }) => {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  if (gender) params.set('gender', gender);
  if (minAge !== undefined) params.set('minAge', String(minAge));
  if (maxAge !== undefined) params.set('maxAge', String(maxAge));
  if (sort !== DEFAULT_SORT) params.set('sort', sort);
  if (size !== DEFAULT_PAGE_SIZE) params.set('size', String(size));
  if (page > 1) params.set('page', String(page));
  return params;
};

/**
 * The customer list state (page, size, sort, search and filters), kept in the
 * URL query string so a refresh, a shared link and the back button all work.
 * Changing anything except the page goes back to the first page.
 */
const useCustomerListParams = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const params = useMemo(() => readListParams(searchParams), [searchParams]);

  const updateParams = useCallback(
    (changes, { replace = false } = {}) => {
      setSearchParams(
        (current) => {
          const next = { ...readListParams(current), ...changes };
          if (!('page' in changes)) {
            next.page = 1;
          }
          return toSearchParams(next);
        },
        { replace },
      );
    },
    [setSearchParams],
  );

  const clearFilters = useCallback(
    () => updateParams({ search: '', gender: undefined, minAge: undefined, maxAge: undefined }),
    [updateParams],
  );

  return { params, updateParams, clearFilters, hasFilters: hasActiveFilters(params) };
};

export default useCustomerListParams;
