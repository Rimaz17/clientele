import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import AgeDistributionChart, { ageSummary, formatRange } from './AgeDistributionChart.jsx';

const ageDistribution = [
  { range: '16-24', count: 8 },
  { range: '25-34', count: 20 },
  { range: '35-44', count: 11 },
  { range: '45-54', count: 0 },
  { range: '55-64', count: 4 },
  { range: '65+', count: 1 },
];

describe('ageSummary', () => {
  it('lists every range and names the largest', () => {
    expect(ageSummary(ageDistribution)).toBe(
      'Customers by age range. 16–24: 8, 25–34: 20, 35–44: 11, 45–54: 0, 55–64: 4, 65+: 1. ' +
        'The largest group is 25–34.',
    );
  });

  it('says so when every range is empty', () => {
    expect(ageSummary(ageDistribution.map(({ range }) => ({ range, count: 0 })))).toBe(
      'No customers in any age range.',
    );
  });
});

describe('AgeDistributionChart', () => {
  it('draws the bars and lists every range in the table view', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <ChakraProvider>
        <AgeDistributionChart ageDistribution={ageDistribution} />
      </ChakraProvider>,
    );

    expect(screen.getByRole('img', { name: ageSummary(ageDistribution) })).toBeInTheDocument();
    expect(container.querySelector('.recharts-bar')).not.toBeNull();

    await user.click(screen.getByRole('button', { name: 'Show table' }));

    const rows = screen.getAllByRole('row').slice(1);
    expect(rows.map((row) => row.textContent)).toEqual(
      ageDistribution.map(({ range, count }) => `${formatRange(range)}${count}`),
    );
  });
});
