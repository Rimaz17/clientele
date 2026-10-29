import { ChakraProvider } from '@chakra-ui/react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AuthProvider from './components/context/AuthContext.jsx';
import Dashboard from './Dashboard.jsx';
import { getCustomerStats } from './services/client.js';
import { fakeToken } from './test/fakeToken.js';

vi.mock('./services/client.js', async (importOriginal) => ({
  ...(await importOriginal()),
  getCustomerStats: vi.fn(),
}));

const months = [
  '2025-11',
  '2025-12',
  '2026-01',
  '2026-02',
  '2026-03',
  '2026-04',
  '2026-05',
  '2026-06',
  '2026-07',
  '2026-08',
  '2026-09',
  '2026-10',
];

const stats = (overrides = {}) => ({
  data: {
    totalCustomers: 1284,
    newLast30Days: 14,
    averageAge: 37.4,
    genderSplit: { MALE: 700, FEMALE: 584 },
    ageDistribution: [
      { range: '16-24', count: 200 },
      { range: '25-34', count: 400 },
      { range: '35-44', count: 300 },
      { range: '45-54', count: 200 },
      { range: '55-64', count: 120 },
      { range: '65+', count: 64 },
    ],
    signupsByMonth: months.map((month, index) => ({ month, count: index })),
    ...overrides,
  },
});

const renderPage = () =>
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={['/dashboard']}>
          <Dashboard />
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );

// Chakra's Stat renders a description list: the label is the <dt>, the value the first <dd>.
const statValue = (label) =>
  within(screen.getByText(label).closest('dl')).getAllByRole('definition')[0].textContent;

describe('Dashboard page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', fakeToken({ sub: 'customer1@example.com' }));
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('shows the three stat cards with the API values', async () => {
    getCustomerStats.mockResolvedValue(stats());

    renderPage();

    await waitFor(() => expect(statValue('Total customers')).toBe('1,284'));
    expect(statValue('New in the last 30 days')).toBe('14');
    expect(statValue('Average age')).toBe('37.4');
    expect(screen.getByText('1% of all customers')).toBeInTheDocument();
  });

  it('shows the three charts', async () => {
    getCustomerStats.mockResolvedValue(stats());

    renderPage();

    expect(await screen.findByRole('region', { name: 'Sign-ups per month' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Gender split' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Age distribution' })).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /^66 sign-ups from November 2025/ }),
    ).toBeInTheDocument();
  });

  it('shows placeholders while the stats load', () => {
    getCustomerStats.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByText('Total customers')).toBeInTheDocument();
    expect(screen.queryByText('Every account, admins included')).not.toBeInTheDocument();
    screen.getAllByRole('region').forEach((chart) => {
      expect(within(chart).queryByRole('img')).not.toBeInTheDocument();
    });
    screen
      .getAllByRole('button', { name: 'Show table' })
      .forEach((button) => expect(button).toBeDisabled());
  });

  it('shows the error with a retry that loads the stats again', async () => {
    const user = userEvent.setup();
    getCustomerStats
      .mockRejectedValueOnce({
        response: { status: 500, data: { message: 'An unexpected error occurred' } },
      })
      .mockResolvedValueOnce(stats());

    renderPage();

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Statistics could not be loaded');
    expect(alert).toHaveTextContent('An unexpected error occurred');

    await user.click(screen.getByRole('button', { name: 'Try again' }));

    await waitFor(() => expect(statValue('Total customers')).toBe('1,284'));
    expect(getCustomerStats).toHaveBeenCalledTimes(2);
  });

  it('replaces the charts with one message when there are no customers', async () => {
    getCustomerStats.mockResolvedValue(
      stats({
        totalCustomers: 0,
        newLast30Days: 0,
        averageAge: null,
        genderSplit: { MALE: 0, FEMALE: 0 },
      }),
    );

    renderPage();

    expect(await screen.findByText('No customers yet', { selector: 'p' })).toBeInTheDocument();
    expect(statValue('Total customers')).toBe('0');
    expect(statValue('Average age')).toBe('–');
    expect(screen.getByRole('link', { name: 'Go to customers' })).toHaveAttribute(
      'href',
      '/dashboard/customers',
    );
    expect(screen.queryByRole('region', { name: 'Gender split' })).not.toBeInTheDocument();
  });
});
