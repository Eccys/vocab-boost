import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

// ESM equivalent of __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig({
  plugins: [react()],
  base: './', // Make sure this is set correctly
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      'react': resolve(__dirname, './node_modules/react'),
      'react-dom': resolve(__dirname, './node_modules/react-dom'),
      'framer-motion': resolve(__dirname, './node_modules/framer-motion'),
    },
  },
  root: '.', // This should point to the current directory (docs)
  publicDir: 'public',
  build: {
    outDir: 'dist', // Output to docs/dist during development
    emptyOutDir: true,
  },
}); 