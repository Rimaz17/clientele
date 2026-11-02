import { HStack, Icon, Text } from '@chakra-ui/react';

// Mark and wordmark colours for each kind of surface the logo sits on.
const TONES = {
  color: { mark: 'brand.blue', text: 'brand.ink' },
  ink: { mark: 'brand.ink', text: 'brand.ink' },
  white: { mark: 'white', text: 'white' },
};

/**
 * The Clientele mark: a heavy "C" with a person in its counter, drawn on a
 * 64 x 64 grid. Pass a label when the mark stands alone; next to the
 * wordmark it is decorative and hidden from screen readers.
 */
export const LogoMark = ({ size = 64, color = 'brand.blue', label, ...rest }) => (
  <Icon
    viewBox="0 0 64 64"
    boxSize={`${size}px`}
    color={color}
    flex="none"
    {...(label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true })}
    {...rest}
  >
    <path
      d="M48.85 17.86 A22 22 0 1 0 48.85 46.14"
      fill="none"
      stroke="currentColor"
      strokeWidth="12"
    />
    <circle cx="32" cy="25" r="5.5" fill="currentColor" />
    <path d="M23 42 a9 9 0 0 1 18 0 z" fill="currentColor" />
  </Icon>
);

/**
 * The full logo: the mark followed by the "Clientele" wordmark. `size` is the
 * height of the mark in pixels; the wordmark and gap scale with it in the
 * proportions of the logo design.
 */
const Logo = ({ size = 40, tone = 'color', ...rest }) => {
  const colours = TONES[tone];
  return (
    <HStack spacing={`${Math.round(size * 0.22)}px`} {...rest}>
      <LogoMark size={size} color={colours.mark} />
      <Text
        as="span"
        fontFamily="brand"
        fontWeight="semibold"
        fontSize={`${Math.round(size * 0.81)}px`}
        lineHeight="1"
        letterSpacing="-0.025em"
        pb="0.075em"
        color={colours.text}
      >
        Clientele
      </Text>
    </HStack>
  );
};

export default Logo;
