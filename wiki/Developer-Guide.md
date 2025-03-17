# Developer Guide

This guide is intended for developers who want to contribute to Vocab or build upon its codebase. It covers the project structure, development environment setup, and best practices.

## Project Architecture

Vocab follows a modern React architecture with the following key components:

### Frontend (React/Vite)

- **UI Components**: Reusable interface elements
- **State Management**: Context API for global state
- **Routing**: React Router for navigation
- **Styling**: CSS modules with responsive design
- **API Integration**: Fetch API with async/await pattern

### Backend Services

- **Firebase Integration**: Authentication and real-time database
- **Local Storage**: IndexedDB for offline capabilities
- **REST API**: For vocabulary and quiz services
- **Background Processing**: Web Workers for neural processing

## Directory Structure

```
/vocab-boost/
├── docs/                # Documentation and website
│   ├── src/             # React components for documentation site
│   └── public/          # Static assets
├── src/                 # Main application source code
│   ├── components/      # Reusable UI components
│   ├── contexts/        # React contexts for state management
│   ├── hooks/           # Custom React hooks
│   ├── pages/           # Screen components
│   ├── services/        # API and data services
│   ├── utils/           # Helper functions
│   └── App.tsx          # Application entry point
├── public/              # Static assets
├── android/             # Android-specific code
├── ios/                 # iOS-specific code (future)
└── tests/               # Unit and integration tests
```

## Development Environment Setup

### Prerequisites

- Node.js 16.x or later
- npm 8.x or later (or yarn 1.22+)
- Git
- Android Studio (for mobile development)
- Firebase account (for backend services)

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Eccys/vocab-boost.git
   cd vocab-boost
   ```

2. **Install dependencies**
   ```bash
   npm install
   # or
   yarn
   ```

3. **Environment configuration**
   - Copy `.env.example` to `.env.local`
   - Update the environment variables with your Firebase config

4. **Start the development server**
   ```bash
   npm run dev
   # or
   yarn dev
   ```

5. **For Android development**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

## Key Technologies

- **React** - UI library
- **Vite** - Build tool and development server
- **TypeScript** - Type-safe JavaScript
- **Firebase** - Backend services
- **React Router** - Application routing
- **TensorFlow.js** - Neural processing for learning algorithm
- **Jest/React Testing Library** - Testing framework

## Code Style and Conventions

Vocab follows a consistent code style to maintain readability and quality:

### TypeScript

- Use typed interfaces for all props and state
- Prefer explicit typing over inferrence for function parameters and returns
- Use enums for constants with multiple related values

### React Components

- Use functional components with hooks
- Props interfaces should be exported for documentation
- Component files should export only one component
- Use React.memo for performance optimization when appropriate

### CSS

- Use CSS modules for component styling
- Follow BEM naming convention
- Use CSS variables for theming
- Design for mobile-first, then adapt to larger screens

### State Management

- Use Context API for global state
- Use useReducer for complex state logic
- Keep state as localized as possible
- Document state shape with TypeScript interfaces

## Pull Request Process

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Implement your changes**
   - Ensure code passes linting
   - Add tests for new functionality
   - Update documentation as needed
4. **Submit pull request**
   - Reference any related issues
   - Provide a clear description of changes
   - Include screenshots for UI changes

## Testing

Vocab uses Jest and React Testing Library for testing:

```bash
# Run all tests
npm test

# Run tests with coverage
npm test -- --coverage

# Run specific test file
npm test -- src/components/YourComponent.test.tsx
```

Write tests for:
- Component rendering
- User interactions
- State changes
- Edge cases and error states

## Documentation

- Add JSDoc comments to functions and components
- Keep README updated with new features
- Update wiki for significant changes
- Document API changes in separate API docs

## Performance Considerations

- Use React.memo for expensive renders
- Lazy load routes and heavy components
- Use virtualization for long lists
- Optimize images and assets
- Monitor bundle size with `npm run analyze`

## Security Guidelines

- Never store secrets in the codebase
- Validate all user inputs
- Use Firebase security rules appropriately
- Implement proper authentication checks
- Keep dependencies updated

## Continuous Integration

The project uses GitHub Actions for CI/CD:

- Automatic linting and testing on PRs
- Build verification
- Automatic deployment to preview environments
- Release versioning

## Getting Help

- Check existing issues on GitHub
- Join our [Discord server](https://discord.gg/vocab-boost)
- Tag maintainers in complex PRs

---

We welcome your contributions and appreciate your efforts to improve Vocab! 