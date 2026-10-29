import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { fakeToken } from '../../test/fakeToken.js';
import AuthProvider from '../context/AuthContext.jsx';
import CardWithImage from './CustomerCard.jsx';

const jamila = {
  id: 2,
  name: 'Jamila',
  email: 'jamila@clientele.dev',
  age: 30,
  gender: 'FEMALE',
  roles: ['ROLE_USER'],
};

const renderCard = (customer, signedInAs) => {
  localStorage.setItem('access_token', fakeToken(signedInAs));
  render(
    <ChakraProvider>
      <AuthProvider>
        <MemoryRouter>
          <CardWithImage {...customer} fetchCustomers={() => {}} />
        </MemoryRouter>
      </AuthProvider>
    </ChakraProvider>,
  );
};

const changeControls = () => [
  screen.queryByRole('button', { name: 'Update' }),
  screen.queryByRole('button', { name: 'Delete' }),
];

describe('CustomerCard', () => {
  afterEach(() => {
    localStorage.clear();
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
});
