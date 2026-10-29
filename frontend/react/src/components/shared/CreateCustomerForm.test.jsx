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

const fillAndSubmitForm = async (user) => {
  await user.type(screen.getByLabelText('Name'), 'Alex');
  await user.type(screen.getByLabelText('Email Address'), 'alex@clientele.dev');
  await user.clear(screen.getByLabelText('Age'));
  await user.type(screen.getByLabelText('Age'), '30');
  await user.type(screen.getByLabelText('Password'), 'password');
  await user.selectOptions(screen.getByLabelText('Gender'), 'FEMALE');
  await user.click(screen.getByRole('button', { name: 'Submit' }));
};

describe('CreateCustomerForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows server-side field errors under the matching inputs', async () => {
    saveCustomer.mockRejectedValue({
      code: 'ERR_BAD_REQUEST',
      response: {
        data: {
          message: 'Request validation failed',
          errors: [{ field: 'email', message: 'Email must be a valid email address' }],
        },
      },
    });
    const user = userEvent.setup();
    render(
      <ChakraProvider>
        <CreateCustomerForm onSuccess={vi.fn()} />
      </ChakraProvider>,
    );

    await fillAndSubmitForm(user);

    expect(await screen.findByText('Email must be a valid email address')).toBeInTheDocument();
    expect(errorNotification).toHaveBeenCalledWith('ERR_BAD_REQUEST', 'Request validation failed');
  });

  it('shows the API message for errors without field details', async () => {
    saveCustomer.mockRejectedValue({
      code: 'ERR_BAD_REQUEST',
      response: { data: { message: 'email already taken', errors: [] } },
    });
    const user = userEvent.setup();
    render(
      <ChakraProvider>
        <CreateCustomerForm onSuccess={vi.fn()} />
      </ChakraProvider>,
    );

    await fillAndSubmitForm(user);

    await vi.waitFor(() =>
      expect(errorNotification).toHaveBeenCalledWith('ERR_BAD_REQUEST', 'email already taken'),
    );
  });

  it('passes the token to onSuccess after a successful registration', async () => {
    saveCustomer.mockResolvedValue({ headers: { authorization: 'jwt-token' } });
    const onSuccess = vi.fn();
    const user = userEvent.setup();
    render(
      <ChakraProvider>
        <CreateCustomerForm onSuccess={onSuccess} />
      </ChakraProvider>,
    );

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
