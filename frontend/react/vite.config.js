import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  // Fast Refresh needs the dev-server preamble, which does not exist under Vitest.
  plugins: [react({ fastRefresh: !process.env.VITEST })],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    // Chakra's focus lock imports the CommonJS default export of react-focus-lock,
    // which Node resolves to an object. Letting Vite transform Chakra applies the
    // interop that the app build uses, so dialogs and drawers work in tests.
    server: {
      deps: {
        inline: [/@chakra-ui\//],
      },
    },
  },
})
