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
      // White body text needs this darker blue to reach 4.5:1; on brand.blue
      // it only reaches about 4.4:1.
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
