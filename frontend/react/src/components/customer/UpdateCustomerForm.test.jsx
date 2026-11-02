import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  customerProfilePictureUrl,
  updateCustomer,
  uploadCustomerProfilePicture,
} from '../../services/client.js';
import { errorNotification } from '../../services/notification.js';
import { fakeToken } from '../../test/fakeToken.js';
import AuthProvider from '../context/AuthContext.jsx';
import UpdateCustomerForm from './UpdateCustomerForm.jsx';

vi.mock('../../services/client.js', () => ({
  updateCustomer: vi.fn(),
  uploadCustomerProfilePicture: vi.fn(),
  customerProfilePictureUrl: vi.fn(
    (id, version) => `/api/v1/customers/${id}/profile-image?v=${version}`,
  ),
}));
vi.mock('../../services/notification.js', () => ({
  errorNotification: vi.fn(),
  successNotification: vi.fn(),
}));

const oldToken = fakeToken({ sub: 'alex@clientele.dev' });

const formFor = (fetchCustomers, profileImageId) => (
  <ChakraProvider>
    <AuthProvider>
      <UpdateCustomerForm
        fetchCustomers={fetchCustomers}
        initialValues={{ name: 'Alex', email: 'alex@clientele.dev', age: 30 }}
        customerId={1}
        profileImageId={profileImageId}
      />
    </AuthProvider>
  </ChakraProvider>
);

const renderForm = (fetchCustomers = vi.fn(), profileImageId = 'old-picture') =>
  render(formFor(fetchCustomers, profileImageId));

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

  it('uploads a dropped picture and refreshes the customers', async () => {
    uploadCustomerProfilePicture.mockResolvedValue({});
    const fetchCustomers = vi.fn();
    const user = userEvent.setup();
    const { container } = renderForm(fetchCustomers);
    const picture = new File(['picture'], 'alex.png', { type: 'image/png' });

    await user.upload(container.querySelector('input[type="file"]'), picture);

    await vi.waitFor(() => expect(fetchCustomers).toHaveBeenCalled());
    const [customerId, formData] = uploadCustomerProfilePicture.mock.calls[0];
    expect(customerId).toBe(1);
    expect(formData.get('file')).toBe(picture);
  });

  it('shows the new picture once the refreshed customer has a new image id', () => {
    const fetchCustomers = vi.fn();
    const { rerender } = renderForm(fetchCustomers, 'old-picture');
    expect(customerProfilePictureUrl).toHaveBeenLastCalledWith(1, 'old-picture');

    rerender(formFor(fetchCustomers, 'new-picture'));

    expect(customerProfilePictureUrl).toHaveBeenLastCalledWith(1, 'new-picture');
  });

  it('says the picture is uploading until the upload finishes', async () => {
    let finishUpload;
    uploadCustomerProfilePicture.mockReturnValue(
      new Promise((resolve) => (finishUpload = resolve)),
    );
    const user = userEvent.setup();
    const { container } = renderForm();
    const picture = new File(['picture'], 'alex.png', { type: 'image/png' });

    await user.upload(container.querySelector('input[type="file"]'), picture);

    expect(await screen.findByText('Uploading the picture…')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Upload a new profile picture' })).toHaveAttribute(
      'aria-busy',
      'true',
    );

    finishUpload({});

    expect(
      await screen.findByText('Drag a picture here, or click to choose one'),
    ).toBeInTheDocument();
  });
});
