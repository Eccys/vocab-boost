/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_DICTIONARY_API_KEY: string;
  readonly VITE_THESAURUS_API_KEY: string;
  // add more env variables here if needed
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
} 