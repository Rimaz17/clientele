import { extendTheme } from '@chakra-ui/react';

// The app's Chakra theme: the default theme plus Clientele's own tokens.
const theme = extendTheme({
  fonts: {
    // Only the "Clientele" wordmark uses the brand face; body text keeps
    // Chakra's system font stack.
    brand: "'Figtree', sans-serif",
  },
  colors: {
    brand: {
      blue: '#2a78d6',
      // The blue for text: about 6.5:1 against white either way round, as
      // blue text on white or white text on blue. brand.blue only reaches
      // about 4.4:1, under the 4.5:1 AA minimum.
      blueDark: '#1a5db0',
      ink: '#1a202c',
    },
    // Chart colours. The two series hues are checked for colour-blind
    // separation and at least 3:1 contrast on the white card surface.
    chart: {
      series1: '#2a78d6',
      series2: '#eb6834',
      grid: '#e2e8f0',
      axis: '#4a5568',
    },
  },
  semanticTokens: {
    colors: {
      // Chakra's gray.500 placeholders measure about 4:1 on white; gray.600
      // clears the 4.5:1 AA minimum.
      'chakra-placeholder-color': { _light: 'gray.600', _dark: 'whiteAlpha.400' },
    },
  },
  components: {
    // Chakra 2.4's Card takes its background from a token that does not
    // resolve, which leaves cards transparent on the grey page.
    Card: {
      baseStyle: {
        container: {
          bg: 'white',
        },
      },
    },
  },
});

export default theme;
