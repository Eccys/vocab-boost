# Vocab Website

This directory contains the React-based website for the Vocab application.

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