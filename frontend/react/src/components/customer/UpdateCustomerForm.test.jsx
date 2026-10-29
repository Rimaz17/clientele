import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { updateCustomer } from '../../services/client.js';
import { errorNotification } from '../../services/notification.js';
import { fakeToken } from '../../test/fakeToken.js';
import AuthProvider from '../context/AuthContext.jsx';
import UpdateCustomerForm from './UpdateCustomerForm.jsx';

vi.mock('../../services/client.js', () => ({
  updateCustomer: vi.fn(),
  uploadCustomerProfilePicture: vi.fn(),
  customerProfilePictureUrl: (id) => `/api/v1/customers/${id}/profile-image`,
}));
vi.mock('../../services/notification.js', () => ({
  errorNotification: vi.fn(),
  successNotification: vi.fn(),
}));

const oldToken = fakeToken({ sub: 'alex@clientele.dev' });

const renderForm = (fetchCustomers = vi.fn()) =>
  render(
    <ChakraProvider>
      <AuthProvider>
        <UpdateCustomerForm
          fetchCustomers={fetchCustomers}
          initialValues={{ name: 'Alex', email: 'alex@clientele.dev', age: 30 }}
          customerId={1}
        />
      </AuthProvider>
    </ChakraProvider>,
  );

const changeEmail = async (user, email) => {
  const input = screen.getByLabelText(/^Email Address/);
  await user.clear(input);
  await user.type(input, email);
  await user.click(screen.getByRole('button', { name: 'Submit' }));
};

describe('UpdateCustomerForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('access_token', oldToken);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('stores the new token the API returns after users change their own email', async () => {
    const newToken = fakeToken({ sub: 'alexandra@clientele.dev' });
    updateCustomer.mockResolvedValue({ headers: { authorization: newToken } });
    const fetchCustomers = vi.fn();
    const user = userEvent.setup();
    renderForm(fetchCustomers);

    await changeEmail(user, 'alexandra@clientele.dev');

    await vi.waitFor(() => expect(fetchCustomers).toHaveBeenCalled());
    expect(localStorage.getItem('access_token')).toBe(newToken);
  });

  it('keeps the current token when the response has none', async () => {
    updateCustomer.mockResolvedValue({ headers: {} });
    const fetchCustomers = vi.fn();
    const user = userEvent.setup();
    renderForm(fetchCustomers);

    await changeEmail(user, 'alexandra@clientele.dev');

    await vi.waitFor(() => expect(fetchCustomers).toHaveBeenCalled());
    expect(localStorage.getItem('access_token')).toBe(oldToken);
  });

  it('explains a 403 in the error toast', async () => {
    updateCustomer.mockRejectedValue({
      response: { status: 403, data: { message: 'Access denied', errors: [] } },
    });
    const user = userEvent.setup();
    renderForm();

    await changeEmail(user, 'alexandra@clientele.dev');

    await vi.waitFor(() =>
      expect(errorNotification).toHaveBeenCalledWith(
        'Could not update customer',
        'You can only change your own account.',
      ),
    );
    expect(localStorage.getItem('access_token')).toBe(oldToken);
  });
});
