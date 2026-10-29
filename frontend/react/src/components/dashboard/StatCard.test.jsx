import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import StatCard from './StatCard.jsx';

const renderCard = (props) =>
  render(
    <ChakraProvider>
      <StatCard label="Total customers" value="132" detail="All accounts" {...props} />
    </ChakraProvider>,
  );

describe('StatCard', () => {
  it('shows the label, value and detail', () => {
    renderCard();

    expect(screen.getByText('Total customers')).toBeInTheDocument();
    expect(screen.getByText('132')).toBeVisible();
    expect(screen.getByText('All accounts')).toBeInTheDocument();
  });

  it('keeps the label but hides the detail while loading', () => {
    renderCard({ isLoading: true });

    expect(screen.getByText('Total customers')).toBeInTheDocument();
    expect(screen.queryByText('All accounts')).not.toBeInTheDocument();
  });
});
