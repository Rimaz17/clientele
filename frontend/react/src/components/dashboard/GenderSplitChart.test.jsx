import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import GenderSplitChart, { genderRows, genderSummary } from './GenderSplitChart.jsx';

describe('genderRows', () => {
  it('lists every gender in a fixed order with its share', () => {
    expect(genderRows({ FEMALE: 62, MALE: 70 })).toMatchObject([
      { key: 'MALE', label: 'Male', count: 70, percent: '53%' },
      { key: 'FEMALE', label: 'Female', count: 62, percent: '47%' },
    ]);
  });

  it('counts a missing gender as zero', () => {
    expect(genderRows({ FEMALE: 3 })).toMatchObject([
      { key: 'MALE', count: 0, percent: '0%' },
      { key: 'FEMALE', count: 3, percent: '100%' },
    ]);
  });
});

describe('genderSummary', () => {
  it('reads out both counts and shares', () => {
    expect(genderSummary(genderRows({ MALE: 70, FEMALE: 62 }))).toBe(
      '70 male (53%) and 62 female (47%).',
    );
  });
});

describe('GenderSplitChart', () => {
  it('shows a legend with counts and a table of the same values', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <ChakraProvider>
        <GenderSplitChart genderSplit={{ MALE: 70, FEMALE: 62 }} />
      </ChakraProvider>,
    );

    expect(
      screen.getByRole('img', { name: '70 male (53%) and 62 female (47%).' }),
    ).toBeInTheDocument();
    expect(container.querySelector('.recharts-pie')).not.toBeNull();
    expect(screen.getByText('Male')).toBeInTheDocument();
    expect(screen.getByText('53%')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Show table' }));

    const rows = screen.getAllByRole('row').slice(1);
    expect(rows.map((row) => row.textContent)).toEqual(['Male70 (53%)', 'Female62 (47%)']);
  });
});
