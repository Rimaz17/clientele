import { ChakraProvider } from '@chakra-ui/react';
import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import theme from '../../theme.js';
import AuthLayout from './AuthLayout.jsx';

const renderLayout = () =>
  render(
    <ChakraProvider theme={theme}>
      <AuthLayout title="Sign in to your account" footer={<a href="/signup">Sign up</a>}>
        <form aria-label="Sign in" />
      </AuthLayout>
    </ChakraProvider>,
  );

describe('AuthLayout', () => {
  it('shows the logo, heading, form and footer in the main area', () => {
    renderLayout();

    const main = screen.getByRole('main');
    expect(within(main).getByText('Clientele')).toBeInTheDocument();
    expect(
      within(main).getByRole('heading', { name: 'Sign in to your account' }),
    ).toBeInTheDocument();
    expect(within(main).getByRole('form', { name: 'Sign in' })).toBeInTheDocument();
    expect(within(main).getByRole('link', { name: 'Sign up' })).toBeInTheDocument();
  });

  // The panel is display: none at the base breakpoint, which jsdom applies, and
  // hidden elements get no accessible name, so the label is checked directly.
  it('describes Clientele in the brand panel without linking anywhere', () => {
    renderLayout();

    const panel = screen.getByRole('complementary', { hidden: true });
    expect(panel).toHaveAttribute('aria-label', 'About Clientele');
    expect(within(panel).getAllByRole('listitem', { hidden: true })).toHaveLength(3);
    expect(within(panel).queryByRole('link', { hidden: true })).not.toBeInTheDocument();
  });
});
