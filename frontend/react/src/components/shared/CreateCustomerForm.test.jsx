import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { saveCustomer } from '../../services/client.js';
import { errorNotification } from '../../services/notification.js';
import CreateCustomerForm from './CreateCustomerForm.jsx';

vi.mock('../../services/client.js', () => ({ saveCustomer: vi.fn() }));
vi.mock('../../services/notification.js', () => ({
  errorNotification: vi.fn(),
  successNotification: vi.fn(),
}));

const renderForm = (onSuccess = vi.fn()) =>
  render(
    <ChakraProvider>
      <CreateCustomerForm onSuccess={onSuccess} />
    </ChakraProvider>,
  );

const fillAndSubmitForm = async (user) => {
  await user.type(screen.getByLabelText(/^Name/), 'Alex');
  await user.type(screen.getByLabelText(/^Email Address/), 'alex@clientele.dev');
  await user.clear(screen.getByLabelText(/^Age/));
  await user.type(screen.getByLabelText(/^Age/), '30');
  await user.type(screen.getByLabelText(/^Password/), 'password');
  await user.selectOptions(screen.getByLabelText(/^Gender/), 'FEMALE');
  await user.click(screen.getByRole('button', { name: 'Submit' }));
};

describe('CreateCustomerForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows server-side field errors under the matching inputs', async () => {
    saveCustomer.mockRejectedValue({
      response: {
        status: 400,
        data: {
          message: 'Request validation failed',
          errors: [{ field: 'email', message: 'Email must be a valid email address' }],
        },
      },
    });
    const user = userEvent.setup();
    renderForm();

    await fillAndSubmitForm(user);

    const message = await screen.findByText('Email must be a valid email address');
    const emailInput = screen.getByLabelText(/^Email Address/);
    expect(emailInput).toHaveAttribute('aria-invalid', 'true');
    expect(emailInput.getAttribute('aria-describedby')).toContain(message.id);
    expect(errorNotification).toHaveBeenCalledWith(
      'Could not save customer',
      'Request validation failed',
    );
  });

  it('shows a taken email under the email field', async () => {
    saveCustomer.mockRejectedValue({
      response: { status: 409, data: { message: 'email already taken', errors: [] } },
    });
    const user = userEvent.setup();
    renderForm();

    await fillAndSubmitForm(user);

    expect(await screen.findByText('email already taken')).toBeInTheDocument();
    expect(screen.getByLabelText(/^Email Address/)).toHaveAttribute('aria-invalid', 'true');
    expect(errorNotification).toHaveBeenCalledWith(
      'Could not save customer',
      'email already taken',
    );
  });

  it('marks every field as required', () => {
    renderForm();

    ['Name', 'Email Address', 'Age', 'Password', 'Gender'].forEach((label) =>
      expect(screen.getByLabelText(new RegExp(`^${label}`))).toBeRequired(),
    );
  });

  it('passes the token to onSuccess after a successful registration', async () => {
    saveCustomer.mockResolvedValue({ headers: { authorization: 'jwt-token' } });
    const onSuccess = vi.fn();
    const user = userEvent.setup();
    renderForm(onSuccess);

    await fillAndSubmitForm(user);

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledWith('jwt-token'));
    expect(saveCustomer).toHaveBeenCalledWith({
      name: 'Alex',
      email: 'alex@clientele.dev',
      age: 30,
      password: 'password',
      gender: 'FEMALE',
    });
  });
});
