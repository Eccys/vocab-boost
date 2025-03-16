# Vocab App Website

This is the official website for the Vocab language learning application. It showcases the app's features, provides download links, and offers additional resources for users.

## Features

- Responsive design for all device sizes
- Interactive UI elements that demonstrate app functionality
- Daily word showcase
- Practice quiz using the Merriam-Webster API

## Tech Stack

- React
- TypeScript
- Vite
- CSS (with custom animations)

## Setup

1. Clone the repository
2. Install dependencies:
```bash
npm install
```
3. Set up your environment variables:
   - Copy `.env.example` to `.env`
   - Replace the placeholder values with your actual API keys:
     - Get your Merriam-Webster Dictionary API Key: [dictionaryapi.com](https://dictionaryapi.com/register/index)
     - Get your Merriam-Webster Thesaurus API Key: [dictionaryapi.com](https://dictionaryapi.com/register/index)

```bash
# .env file
VITE_DICTIONARY_API_KEY=your-dictionary-api-key-here
VITE_THESAURUS_API_KEY=your-thesaurus-api-key-here
```

4. Run the development server:
```bash
npm run dev
```

## API Integration

The website uses the Merriam-Webster API for the Practice Quiz feature:
- The Thesaurus API is used to fetch synonyms for quiz questions
- The Dictionary API provides words of the same part of speech for quiz options

For more information on the APIs, visit [dictionaryapi.com](https://dictionaryapi.com/).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Project Structure

- `/src` - React source code
- `/public` - Static assets
- `/original` - Original documentation files

## Development

To work on the website:

1. Navigate to the docs directory:
   ```
   cd docs
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm run dev
   ```

4. Open [http://localhost:5173](http://localhost:5173) in your browser.

## Deployment

To deploy the website to GitHub Pages:

1. Run the deploy script:
   ```
   npm run deploy
   ```

This will:
- Build the React application
- Copy the built files to the root of the docs directory
- Clean up temporary files

GitHub Pages will then serve these files from your repository.

## Original Documentation

The original documentation content has been preserved in the `/original` directory. 