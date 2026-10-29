import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { deleteCustomer } from '../../services/client.js';
import { fakeToken } from '../../test/fakeToken.js';
import AuthProvider from '../context/AuthContext.jsx';
import CardWithImage from './CustomerCard.jsx';

vi.mock('../../services/client.js', () => ({
  deleteCustomer: vi.fn(),
  customerProfilePictureUrl: (id) => `/api/v1/customers/${id}/profile-image`,
}));
vi.mock('../../services/notification.js', () => ({
  errorNotification: vi.fn(),
  successNotification: vi.fn(),
}));

const jamila = {
  id: 2,
  name: 'Jamila',
  email: 'jamila@clientele.dev',
  age: 30,
  gender: 'FEMALE',
  roles: ['ROLE_USER'],
};

const renderCard = (customer, signedInAs, fetchCustomers = vi.fn()) => {
  localStorage.setItem('access_token', fakeToken(signedInAs));
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter initialEntries={['/dashboard/customers']}>
          <Routes>
            <Route
              path="/dashboard/customers"
              element={<CardWithImage {...customer} fetchCustomers={fetchCustomers} />}
            />
            <Route path="/" element={<p>Login page</p>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );
};

const confirmDelete = async (user) => {
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  const buttons = await screen.findAllByRole('button', { name: 'Delete' });
  await user.click(buttons[buttons.length - 1]);
};

const changeControls = () => [
  screen.queryByRole('button', { name: 'Update' }),
  screen.queryByRole('button', { name: 'Delete' }),
];

describe('CustomerCard', () => {
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it("hides the change controls on someone else's card for a regular user", () => {
    renderCard(jamila, { sub: 'alex@clientele.dev' });

    changeControls().forEach((control) => expect(control).not.toBeInTheDocument());
  });

  it("shows the change controls on the user's own card", () => {
    renderCard(jamila, { sub: 'Jamila@clientele.dev' });

    changeControls().forEach((control) => expect(control).toBeInTheDocument());
  });

  it('shows the change controls on every card for an admin', () => {
    renderCard(jamila, { sub: 'admin@clientele.dev', scopes: ['ROLE_ADMIN'] });

    changeControls().forEach((control) => expect(control).toBeInTheDocument());
  });

  it('marks admins with a badge', () => {
    renderCard({ ...jamila, roles: ['ROLE_ADMIN'] }, { sub: 'alex@clientele.dev' });

    expect(screen.getByText('Admin')).toBeInTheDocument();
  });

  it('shows no badge for regular users', () => {
    renderCard(jamila, { sub: 'alex@clientele.dev' });

    expect(screen.queryByText('Admin')).not.toBeInTheDocument();
  });

  it('signs users out after they delete their own account', async () => {
    deleteCustomer.mockResolvedValue({});
    const fetchCustomers = vi.fn();
    const user = userEvent.setup();
    renderCard(jamila, { sub: 'jamila@clientele.dev' }, fetchCustomers);

    await confirmDelete(user);

    expect(await screen.findByText('Login page')).toBeInTheDocument();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(fetchCustomers).not.toHaveBeenCalled();
  });

  it('refreshes the list after an admin deletes someone else', async () => {
    deleteCustomer.mockResolvedValue({});
    const fetchCustomers = vi.fn();
    const user = userEvent.setup();
    renderCard(jamila, { sub: 'admin@clientele.dev', scopes: ['ROLE_ADMIN'] }, fetchCustomers);

    await confirmDelete(user);

    await vi.waitFor(() => expect(fetchCustomers).toHaveBeenCalled());
    expect(deleteCustomer).toHaveBeenCalledWith(2);
    expect(localStorage.getItem('access_token')).not.toBeNull();
  });
});
