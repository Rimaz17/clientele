import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import SignupsChart, { signupsSummary } from './SignupsChart.jsx';

const signups = [
  { month: '2025-11', count: 2 },
  { month: '2025-12', count: 0 },
  { month: '2026-01', count: 7 },
  { month: '2026-02', count: 3 },
];

describe('signupsSummary', () => {
  it('names the period, the total and the busiest month', () => {
    expect(signupsSummary(signups)).toBe(
      '12 sign-ups from November 2025 to February 2026. The busiest month was January 2026 with 7 sign-ups.',
    );
  });

  it('says so when no one signed up', () => {
    expect(signupsSummary(signups.map(({ month }) => ({ month, count: 0 })))).toBe(
      'No sign-ups from November 2025 to February 2026.',
    );
  });
});

describe('SignupsChart', () => {
  it('describes the chart and lists every month in the table view', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <ChakraProvider>
        <SignupsChart signups={signups} />
      </ChakraProvider>,
    );

    expect(screen.getByRole('heading', { name: 'Sign-ups per month' })).toBeInTheDocument();
    expect(screen.getByText('November 2025 to February 2026')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: signupsSummary(signups) })).toBeInTheDocument();
    expect(container.querySelector('.recharts-area')).not.toBeNull();

    await user.click(screen.getByRole('button', { name: 'Show table' }));

    const rows = screen.getAllByRole('row').slice(1);
    expect(rows.map((row) => row.textContent)).toEqual([
      'November 20252',
      'December 20250',
      'January 20267',
      'February 20263',
    ]);
  });
});
