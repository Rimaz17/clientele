import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Logo, { LogoMark } from './Logo.jsx';

const renderWithChakra = (ui) => render(<ChakraProvider>{ui}</ChakraProvider>);

describe('Logo', () => {
  it('shows the wordmark and hides the decorative mark', () => {
    const { container } = renderWithChakra(<Logo />);

    expect(screen.getByText('Clientele')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('sizes the mark from the size prop', () => {
    const { container } = renderWithChakra(<Logo size={32} />);

    expect(container.querySelector('svg')).toHaveStyle({ width: '32px', height: '32px' });
  });
});

describe('LogoMark', () => {
  it('is announced as an image when it has a label', () => {
    renderWithChakra(<LogoMark label="Clientele" />);

    expect(screen.getByRole('img', { name: 'Clientele' })).toBeInTheDocument();
  });
});
