import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import ChartCard from './ChartCard.jsx';
import ChartTooltip from './ChartTooltip.jsx';

const table = {
  columns: ['Month', 'Sign-ups'],
  rows: [
    ['January 2026', '3'],
    ['February 2026', '5'],
  ],
};

const renderCard = (props) =>
  render(
    <ChakraProvider>
      <ChartCard
        title="Sign-ups"
        description="Last 12 months"
        summary="8 sign-ups in 2 months."
        table={table}
        {...props}
      >
        <svg data-testid="chart" />
      </ChartCard>
    </ChakraProvider>,
  );

describe('ChartCard', () => {
  it('names the section and describes the chart for screen readers', () => {
    renderCard();

    expect(screen.getByRole('region', { name: 'Sign-ups' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '8 sign-ups in 2 months.' })).toBeInTheDocument();
    expect(screen.getByText('Last 12 months')).toBeInTheDocument();
  });

  it('switches between the chart and a table of the same values', async () => {
    const user = userEvent.setup();
    renderCard();

    await user.click(screen.getByRole('button', { name: 'Show table' }));

    const tableView = screen.getByRole('table', { name: 'Sign-ups' });
    expect(tableView).toHaveTextContent('January 2026');
    expect(tableView).toHaveTextContent('5');
    expect(screen.queryByTestId('chart')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Show chart' }));

    expect(screen.getByTestId('chart')).toBeInTheDocument();
  });

  it('shows a placeholder and disables the toggle while loading', () => {
    renderCard({ isLoading: true });

    expect(screen.queryByTestId('chart')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show table' })).toBeDisabled();
  });
});

describe('ChartTooltip', () => {
  const format = {
    formatLabel: (label) => `Month ${label}`,
    formatValue: (value) => `${value} sign-ups`,
  };

  it('shows the formatted value and label of the hovered point', () => {
    render(
      <ChakraProvider>
        <ChartTooltip active payload={[{ value: 4, name: 'count' }]} label="2026-03" {...format} />
      </ChakraProvider>,
    );

    expect(screen.getByText('4 sign-ups')).toBeInTheDocument();
    expect(screen.getByText('Month 2026-03')).toBeInTheDocument();
  });

  it('renders nothing when inactive', () => {
    const { container } = render(<ChartTooltip active={false} payload={[]} {...format} />);

    expect(container).toBeEmptyDOMElement();
  });
});
