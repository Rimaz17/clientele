import { ChakraProvider } from '@chakra-ui/react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { readListParams } from '../../hooks/useCustomerListParams.js';
import CustomerListToolbar, { INPUT_DEBOUNCE_MS } from './CustomerListToolbar.jsx';

const renderToolbar = (query = '', onChange = vi.fn()) => {
  const view = render(
    <ChakraProvider>
      <CustomerListToolbar
        params={readListParams(new URLSearchParams(query))}
        onChange={onChange}
      />
    </ChakraProvider>,
  );
  const rerenderWith = (nextQuery) =>
    view.rerender(
      <ChakraProvider>
        <CustomerListToolbar
          params={readListParams(new URLSearchParams(nextQuery))}
          onChange={onChange}
        />
      </ChakraProvider>,
    );
  return { onChange, rerenderWith };
};

describe('CustomerListToolbar', () => {
  let user;

  beforeEach(() => {
    vi.useFakeTimers();
    user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('debounces the search before reporting it', async () => {
    const { onChange } = renderToolbar();

    await user.type(screen.getByLabelText('Search'), 'ali');
    expect(onChange).not.toHaveBeenCalled();

    act(() => vi.advanceTimersByTime(INPUT_DEBOUNCE_MS));

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ search: 'ali' });
  });

  it('reports gender and sort changes immediately', async () => {
    const { onChange } = renderToolbar();

    await user.selectOptions(screen.getByLabelText('Gender'), 'FEMALE');
    await user.selectOptions(screen.getByLabelText('Sort by'), 'age,desc');

    expect(onChange).toHaveBeenCalledWith({ gender: 'FEMALE' });
    expect(onChange).toHaveBeenCalledWith({ sort: 'age,desc' });
  });

  it('reports "All" genders as no filter', async () => {
    const { onChange } = renderToolbar('gender=MALE');

    await user.selectOptions(screen.getByLabelText('Gender'), 'All');

    expect(onChange).toHaveBeenCalledWith({ gender: undefined });
  });

  it('reports a valid age range once typing pauses', async () => {
    const { onChange } = renderToolbar();

    await user.type(screen.getByLabelText('Min age'), '18');
    await user.type(screen.getByLabelText('Max age'), '40');
    act(() => vi.advanceTimersByTime(INPUT_DEBOUNCE_MS));

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ minAge: 18, maxAge: 40 });
  });

  it('shows an error and sends nothing for an age outside 16 to 100', async () => {
    const { onChange } = renderToolbar();

    await user.type(screen.getByLabelText('Min age'), '12');
    act(() => vi.advanceTimersByTime(INPUT_DEBOUNCE_MS));

    expect(screen.getByText('Enter an age from 16 to 100')).toBeInTheDocument();
    expect(screen.getByLabelText('Min age')).toHaveAttribute('aria-invalid', 'true');
    expect(onChange).not.toHaveBeenCalled();
  });

  it('shows an error when the maximum age is below the minimum age', async () => {
    const { onChange } = renderToolbar();

    await user.type(screen.getByLabelText('Min age'), '40');
    await user.type(screen.getByLabelText('Max age'), '30');
    act(() => vi.advanceTimersByTime(INPUT_DEBOUNCE_MS));

    expect(screen.getByText('Must not be below the minimum age')).toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });

  it('follows the URL when the filters are cleared elsewhere', () => {
    const { onChange, rerenderWith } = renderToolbar('search=ali&minAge=20');

    rerenderWith('');
    act(() => vi.advanceTimersByTime(INPUT_DEBOUNCE_MS));

    expect(screen.getByLabelText('Search')).toHaveValue('');
    expect(screen.getByLabelText('Min age')).toHaveValue(null);
    expect(onChange).not.toHaveBeenCalled();
  });
});
