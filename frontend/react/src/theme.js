import { extendTheme } from '@chakra-ui/react';

// The app's Chakra theme: the default theme plus Clientele's own tokens.
const theme = extendTheme({
  colors: {
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
