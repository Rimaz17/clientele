import { Flex, Heading, List, ListIcon, ListItem, Stack, Text } from '@chakra-ui/react';
import { FiBarChart2, FiSearch, FiShield } from 'react-icons/fi';
import Logo from './Logo.jsx';

// What Clientele does, in the order a first-time visitor meets it.
const HIGHLIGHTS = [
  { icon: FiSearch, text: 'Search, filter and sort every customer' },
  { icon: FiShield, text: 'Roles decide who can change which records' },
  { icon: FiBarChart2, text: 'A dashboard built from the live records' },
];

// The blue panel beside the form, and the only logo from md up. Hidden on
// small screens, where the logo sits above the form instead.
const BrandPanel = () => (
  <Flex
    as="aside"
    aria-label="About Clientele"
    flex={1}
    p={10}
    display={{ base: 'none', md: 'flex' }}
    alignItems="center"
    justifyContent="center"
    bg="brand.blueDark"
    color="white"
  >
    <Stack spacing={8} maxW="sm">
      <Logo size={56} tone="white" />
      <Text fontSize="xl" lineHeight="short">
        One place to find, update and manage your customers.
      </Text>
      <List spacing={4}>
        {HIGHLIGHTS.map(({ icon, text }) => (
          <ListItem key={text} display="flex" alignItems="center">
            <ListIcon as={icon} boxSize={5} mr={3} aria-hidden />
            {text}
          </ListItem>
        ))}
      </List>
    </Stack>
  </Flex>
);

/**
 * Page frame for signing in and signing up: a heading and the form on the
 * left, and the brand panel on the right from the md breakpoint up. Below md
 * the panel is hidden, so the logo moves above the heading instead; the page
 * never shows the logo twice.
 */
const AuthLayout = ({ title, children, footer }) => (
  <Stack minH="100vh" direction={{ base: 'column', md: 'row' }} spacing={0}>
    <Flex as="main" p={8} flex={1} alignItems="center" justifyContent="center">
      <Stack spacing={4} w="full" maxW="md">
        <Logo size={40} mb={6} display={{ base: 'flex', md: 'none' }} />
        <Heading as="h1" fontSize="2xl">
          {title}
        </Heading>
        {children}
        {footer}
      </Stack>
    </Flex>
    <BrandPanel />
  </Stack>
);

export default AuthLayout;
