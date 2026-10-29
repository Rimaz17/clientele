import { ChakraProvider } from '@chakra-ui/react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AuthProvider from './components/context/AuthContext.jsx';
import Customer from './Customer.jsx';
import { getCustomers } from './services/client.js';
import { fakeToken } from './test/fakeToken.js';

vi.mock('./services/client.js', async (importOriginal) => ({
  ...(await importOriginal()),
  getCustomers: vi.fn(),
}));
vi.mock('./services/notification.js', () => ({
  errorNotification: vi.fn(),
  successNotification: vi.fn(),
}));

const customer = (id) => ({
  id,
  name: `Customer ${id}`,
  email: `customer${id}@example.com`,
  age: 30,
  gender: 'FEMALE',
  roles: ['ROLE_USER'],
});

const pageOf = ({ page = 0, size = 20, totalElements }) => {
  const first = page * size + 1;
  const count = Math.max(0, Math.min(size, totalElements - page * size));
  return {
    data: {
      content: Array.from({ length: count }, (_, index) => customer(first + index)),
      page,
      size,
      totalElements,
      totalPages: Math.ceil(totalElements / size),
    },
  };
};

const LocationProbe = () => <output data-testid="location">{useLocation().search}</output>;

const renderPage = (url = '/dashboard/customers') =>
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={[url]}>
          <Routes>
            <Route
              path="/dashboard/customers"
              element={
                <>
                  <Customer />
                  <LocationProbe />
                </>
              }
            />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );

describe('Customer page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', fakeToken({ sub: 'customer1@example.com' }));
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('loads the first page with the default sort and size', async () => {
    getCustomers.mockResolvedValue(pageOf({ totalElements: 45 }));

    renderPage();

    expect(await screen.findByText('Showing 1–20 of 45')).toBeInTheDocument();
    expect(screen.getByText('Customer 1')).toBeInTheDocument();
    expect(getCustomers).toHaveBeenCalledWith({ page: 0, size: 20, sort: 'id,asc' });
  });

  it('requests the next page and records it in the URL', async () => {
    getCustomers.mockImplementation(({ page }) =>
      Promise.resolve(pageOf({ page, totalElements: 45 })),
    );
    renderPage();
    await screen.findByText('Showing 1–20 of 45');

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(await screen.findByText('Showing 21–40 of 45')).toBeInTheDocument();
    expect(getCustomers).toHaveBeenLastCalledWith({ page: 1, size: 20, sort: 'id,asc' });
    expect(screen.getByTestId('location')).toHaveTextContent('?page=2');
  });

  it('restores the list state from the URL', async () => {
    getCustomers.mockResolvedValue(pageOf({ page: 1, size: 10, totalElements: 45 }));

    renderPage('/dashboard/customers?search=ali&gender=MALE&sort=name,desc&size=10&page=2');

    await screen.findByText('Showing 11–20 of 45');
    expect(getCustomers).toHaveBeenCalledWith({
      page: 1,
      size: 10,
      sort: 'name,desc',
      search: 'ali',
      gender: 'MALE',
    });
    expect(screen.getByLabelText('Search')).toHaveValue('ali');
    expect(screen.getByLabelText('Gender')).toHaveValue('MALE');
  });

  it('goes back to the first page when a filter changes', async () => {
    getCustomers.mockImplementation(({ page }) =>
      Promise.resolve(pageOf({ page, totalElements: 45 })),
    );
    renderPage('/dashboard/customers?page=3');
    await screen.findByText('Showing 41–45 of 45');

    await userEvent.selectOptions(screen.getByLabelText('Gender'), 'FEMALE');

    await waitFor(() =>
      expect(getCustomers).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        sort: 'id,asc',
        gender: 'FEMALE',
      }),
    );
    expect(screen.getByTestId('location')).toHaveTextContent('?gender=FEMALE');
  });

  it('says when there are no customers at all', async () => {
    getCustomers.mockResolvedValue(pageOf({ totalElements: 0 }));

    renderPage();

    expect(await screen.findByText('No customers yet')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Clear filters' })).not.toBeInTheDocument();
  });

  it('offers to clear the filters when nothing matches them', async () => {
    getCustomers.mockResolvedValue(pageOf({ totalElements: 0 }));
    renderPage('/dashboard/customers?search=nobody&minAge=90');
    await screen.findByText('No customers match these filters');

    await userEvent.click(screen.getByRole('button', { name: 'Clear filters' }));

    await waitFor(() =>
      expect(getCustomers).toHaveBeenLastCalledWith({ page: 0, size: 20, sort: 'id,asc' }),
    );
    expect(screen.getByTestId('location')).toBeEmptyDOMElement();
  });

  it('moves to the last page when the requested page is past the end', async () => {
    getCustomers.mockImplementation(({ page }) =>
      Promise.resolve(pageOf({ page, totalElements: 21 })),
    );

    renderPage('/dashboard/customers?page=3');

    expect(await screen.findByText('Showing 21–21 of 21')).toBeInTheDocument();
    expect(getCustomers).toHaveBeenLastCalledWith({ page: 1, size: 20, sort: 'id,asc' });
    expect(screen.getByTestId('location')).toHaveTextContent('?page=2');
  });

  it('offers a retry when the first load fails', async () => {
    getCustomers
      .mockRejectedValueOnce(new Error('Network Error'))
      .mockResolvedValueOnce(pageOf({ totalElements: 3 }));
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('Showing 1–3 of 3')).toBeInTheDocument();
  });
});
