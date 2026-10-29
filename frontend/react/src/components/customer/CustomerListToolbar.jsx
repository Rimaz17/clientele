import {
  Flex,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Input,
  InputGroup,
  InputLeftElement,
  Select,
} from '@chakra-ui/react';
import { useEffect, useRef, useState } from 'react';
import { FiSearch } from 'react-icons/fi';
import useDebouncedValue from '../../hooks/useDebouncedValue.js';
import {
  MAX_AGE,
  MAX_SEARCH_LENGTH,
  MIN_AGE,
  SORT_OPTIONS,
} from '../../hooks/useCustomerListParams.js';

export const INPUT_DEBOUNCE_MS = 300;

// gray.600 keeps placeholder text above 4.5:1 on white; Chakra's gray.500 is about 4:1.
const PLACEHOLDER_STYLE = { color: 'gray.600' };

const ageText = (age) => (age === undefined ? '' : String(age));
const toAge = (text) => (text === '' ? undefined : Number(text));

const ageError = (text) =>
  text === '' || (/^\d+$/.test(text) && Number(text) >= MIN_AGE && Number(text) <= MAX_AGE)
    ? undefined
    : `Enter an age from ${MIN_AGE} to ${MAX_AGE}`;

const rangeError = ({ minAge, maxAge }) =>
  minAge !== '' && maxAge !== '' && Number(maxAge) < Number(minAge)
    ? 'Must not be below the minimum age'
    : undefined;

/**
 * Search, filter and sort controls for the customer list. Typed values reach
 * `onChange` once typing pauses; ages that the API would reject stay in the
 * input with an error and are not sent.
 */
const CustomerListToolbar = ({ params, onChange }) => {
  const [search, setSearch] = useState(params.search);
  const [ages, setAges] = useState({
    minAge: ageText(params.minAge),
    maxAge: ageText(params.maxAge),
  });
  const debouncedSearch = useDebouncedValue(search, INPUT_DEBOUNCE_MS);
  const debouncedAges = useDebouncedValue(ages, INPUT_DEBOUNCE_MS);

  // The effects below must run only when the debounced input changes, not
  // when the URL does, so they read the latest params and callback from a ref.
  const latest = useRef({ params, onChange });
  latest.current = { params, onChange };

  useEffect(() => {
    const value = debouncedSearch.trim();
    if (value !== latest.current.params.search) {
      latest.current.onChange({ search: value });
    }
  }, [debouncedSearch]);

  useEffect(() => {
    if (
      ageError(debouncedAges.minAge) ||
      ageError(debouncedAges.maxAge) ||
      rangeError(debouncedAges)
    ) {
      return;
    }
    const minAge = toAge(debouncedAges.minAge);
    const maxAge = toAge(debouncedAges.maxAge);
    const current = latest.current.params;
    if (minAge !== current.minAge || maxAge !== current.maxAge) {
      latest.current.onChange({ minAge, maxAge });
    }
  }, [debouncedAges]);

  // Follow the URL when it changes elsewhere, for example after "Clear
  // filters" or the back button, without undoing what is being typed.
  useEffect(() => {
    setSearch((current) => (current.trim() === params.search ? current : params.search));
  }, [params.search]);

  useEffect(() => {
    setAges((current) =>
      toAge(current.minAge) === params.minAge && toAge(current.maxAge) === params.maxAge
        ? current
        : { minAge: ageText(params.minAge), maxAge: ageText(params.maxAge) },
    );
  }, [params.minAge, params.maxAge]);

  const minAgeError = ageError(ages.minAge);
  const maxAgeError = ageError(ages.maxAge) ?? rangeError(ages);

  return (
    <Flex
      as="form"
      role="search"
      wrap="wrap"
      gap={4}
      align="flex-start"
      onSubmit={(event) => event.preventDefault()}
    >
      <FormControl id="customer-search" flex="1 1 16rem">
        <FormLabel fontSize="sm">Search</FormLabel>
        <InputGroup>
          <InputLeftElement pointerEvents="none" color="gray.500">
            <FiSearch aria-hidden="true" />
          </InputLeftElement>
          <Input
            type="search"
            placeholder="Name or email"
            value={search}
            maxLength={MAX_SEARCH_LENGTH}
            onChange={(event) => setSearch(event.target.value)}
            bg="white"
            _placeholder={PLACEHOLDER_STYLE}
          />
        </InputGroup>
      </FormControl>

      <FormControl id="customer-gender" flex="0 1 9rem">
        <FormLabel fontSize="sm">Gender</FormLabel>
        <Select
          value={params.gender ?? ''}
          onChange={(event) => onChange({ gender: event.target.value || undefined })}
          bg="white"
        >
          <option value="">All</option>
          <option value="FEMALE">Female</option>
          <option value="MALE">Male</option>
        </Select>
      </FormControl>

      {/* The two ages wrap together, so the range never splits across rows. */}
      <Flex gap={4} flex="0 1 15rem" minW="12rem">
        <FormControl id="customer-min-age" flex="1" isInvalid={Boolean(minAgeError)}>
          <FormLabel fontSize="sm">Min age</FormLabel>
          <Input
            type="number"
            inputMode="numeric"
            min={MIN_AGE}
            max={MAX_AGE}
            placeholder={String(MIN_AGE)}
            value={ages.minAge}
            onChange={(event) => setAges((current) => ({ ...current, minAge: event.target.value }))}
            bg="white"
            _placeholder={PLACEHOLDER_STYLE}
          />
          <FormErrorMessage color="red.600">{minAgeError}</FormErrorMessage>
        </FormControl>

        <FormControl id="customer-max-age" flex="1" isInvalid={Boolean(maxAgeError)}>
          <FormLabel fontSize="sm">Max age</FormLabel>
          <Input
            type="number"
            inputMode="numeric"
            min={MIN_AGE}
            max={MAX_AGE}
            placeholder={String(MAX_AGE)}
            value={ages.maxAge}
            onChange={(event) => setAges((current) => ({ ...current, maxAge: event.target.value }))}
            bg="white"
            _placeholder={PLACEHOLDER_STYLE}
          />
          <FormErrorMessage color="red.600">{maxAgeError}</FormErrorMessage>
        </FormControl>
      </Flex>

      <FormControl id="customer-sort" flex="0 1 13rem">
        <FormLabel fontSize="sm">Sort by</FormLabel>
        <Select
          value={params.sort}
          onChange={(event) => onChange({ sort: event.target.value })}
          bg="white"
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
      </FormControl>
    </Flex>
  );
};

export default CustomerListToolbar;
