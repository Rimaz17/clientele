import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import AuthProvider from '../context/AuthContext.jsx';
import Login from './Login.jsx';

const renderLogin = () =>
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter>
          <Login />
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );

describe('Login', () => {
  it('renders the sign-in form', () => {
    renderLogin();

    expect(screen.getByRole('heading', { name: 'Sign in to your account' })).toBeInTheDocument();
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Type your password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument();
  });

  it('keeps the login button disabled until the form is valid', async () => {
    renderLogin();

    expect(await screen.findByRole('button', { name: 'Login' })).toBeDisabled();
  });
});
