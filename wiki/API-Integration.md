# API Integration Guide

Vocab integrates with various external APIs to provide rich vocabulary content and learning experiences. This guide explains the current API integrations and how developers can extend the app with additional data sources.

## Current API Integrations

### Dictionary APIs

1. **WordsAPI**
   - Primary source for word definitions, synonyms, and examples
   - Endpoint: `https://wordsapiv1.p.rapidapi.com/words/{word}`
   - Used for: Core word data including parts of speech, definitions, synonyms

2. **Oxford Dictionary API**
   - Used for British English definitions and phonetics
   - Endpoint: `https://od-api.oxforddictionaries.com/api/v2/entries/{language}/{word}`
   - Used for: Pronunciation guides, British English definitions

3. **Merriam-Webster API**
   - Used for additional examples and etymology information
   - Endpoint: `https://www.dictionaryapi.com/api/v3/references/collegiate/json/{word}`
   - Used for: Word origins, additional usage examples

### Learning Content APIs

1. **Pixabay API**
   - Used for image associations with vocabulary words
   - Endpoint: `https://pixabay.com/api/?q={word}`
   - Used for: Visual vocabulary learning aids

2. **Forvo Pronunciation API**
   - Used for native speaker audio pronunciations
   - Endpoint: `https://apifree.forvo.com/action/word-pronunciations/format/json/word/{word}`
   - Used for: Audio pronunciation from native speakers

3. **Context Sentences API**
   - Custom API providing real-world usage examples
   - Endpoint: `https://api.vocab-boost.com/contexts/{word}`
   - Used for: Contextual example sentences from literature, news, etc.

## API Authentication

Vocab uses different authentication methods depending on the API:

1. **API Keys**
   - Most common method, stored securely in environment variables
   - Never exposed to client-side code directly
   - Example: `VITE_WORDS_API_KEY=your-api-key-here`

2. **OAuth**
   - Used for user-specific APIs (e.g., saving to personal dictionaries)
   - Implemented via OAuth 2.0 flow
   - Refresh tokens handled automatically

3. **Custom Authentication**
   - For internal APIs, custom JWT-based authentication
   - Tokens refreshed via background service worker

## Data Flow Architecture

```
┌───────────┐     ┌──────────────┐     ┌────────────┐
│           │     │              │     │            │
│ API Layer ├────►│ Data Service ├────►│ React App  │
│           │     │              │     │            │
└───────────┘     └──────────────┘     └────────────┘
       │                 ▲                   │
       │                 │                   │
       ▼                 │                   ▼
┌───────────┐     ┌──────────────┐    ┌─────────────┐
│           │     │              │    │             │
│   Cache   │     │ Local Storage│◄───┤ User Actions│
│           │     │              │    │             │
└───────────┘     └──────────────┘    └─────────────┘
```

1. API requests are initiated by data services
2. Results are cached for performance and offline use
3. Processed data is provided to React components
4. User interactions may trigger new API requests

## Error Handling Strategy

Vocab implements a robust error handling approach for API integrations:

1. **Graceful Degradation**
   - Falls back to alternative APIs if primary source fails
   - Uses cached data when APIs are unavailable

2. **Rate Limiting**
   - Implements exponential backoff for retries
   - Queues requests to avoid hitting rate limits

3. **Error Messaging**
   - User-friendly error messages
   - Detailed logging for debugging
   - Telemetry for tracking API reliability

## Adding Custom API Integrations

Developers can extend Vocab with additional API integrations:

### Step 1: Create API Service

Create a new service file in `src/services/api`:

```typescript
// src/services/api/customDictionaryApi.ts
import { ApiResponse, WordData } from '../types';
import { handleApiError } from '../utils';

export async function fetchWordFromCustomApi(word: string): Promise<WordData> {
  try {
    const response = await fetch(
      `https://your-custom-api.com/v1/words/${word}`,
      {
        headers: {
          'Authorization': `Bearer ${process.env.VITE_CUSTOM_API_KEY}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    
    const data = await response.json();
    return transformCustomApiResponse(data);
  } catch (error) {
    return handleApiError(error, 'customDictionaryApi');
  }
}

function transformCustomApiResponse(apiData: any): WordData {
  // Transform the API's response format to match your app's data model
  return {
    word: apiData.term,
    phonetic: apiData.pronunciation || '',
    partOfSpeech: apiData.wordType || '',
    definitions: apiData.meanings.map(m => ({
      definition: m.definition,
      example: m.exampleSentence || ''
    })),
    // ... other transformations
  };
}
```

### Step 2: Register in API Manager

Add your API to the API manager for centralized handling:

```typescript
// src/services/apiManager.ts
import { fetchWordFromCustomApi } from './api/customDictionaryApi';

// In the getWordData function
export async function getWordData(word: string) {
  try {
    // Try primary API
    const data = await fetchWordData(word);
    return data;
  } catch (primaryError) {
    // Log the error
    logApiError(primaryError, 'primary');
    
    try {
      // Try your custom API as fallback
      return await fetchWordFromCustomApi(word);
    } catch (fallbackError) {
      // Handle all APIs failing
      logApiError(fallbackError, 'fallback');
      throw new Error('Unable to fetch word data from any source');
    }
  }
}
```

### Step 3: Add Environment Variables

Update your `.env` file and types:

```
VITE_CUSTOM_API_KEY=your-api-key-here
VITE_CUSTOM_API_URL=https://your-custom-api.com/v1
```

### Step 4: Create Tests

Add tests for your new API integration:

```typescript
// src/services/api/__tests__/customDictionaryApi.test.ts
import { fetchWordFromCustomApi } from '../customDictionaryApi';
import fetchMock from 'jest-fetch-mock';

describe('Custom Dictionary API', () => {
  beforeEach(() => {
    fetchMock.resetMocks();
  });

  it('successfully fetches and transforms word data', async () => {
    // Mock API response
    fetchMock.mockResponseOnce(JSON.stringify({
      term: 'example',
      pronunciation: 'ɪɡˈzæmpəl',
      wordType: 'noun',
      meanings: [{
        definition: 'a thing characteristic of its kind',
        exampleSentence: 'This is an example sentence.'
      }]
    }));

    const result = await fetchWordFromCustomApi('example');
    
    expect(result.word).toBe('example');
    expect(result.phonetic).toBe('ɪɡˈzæmpəl');
    expect(result.partOfSpeech).toBe('noun');
    expect(result.definitions[0].definition).toBe('a thing characteristic of its kind');
    expect(result.definitions[0].example).toBe('This is an example sentence.');
  });

  it('handles API errors gracefully', async () => {
    fetchMock.mockRejectOnce(new Error('Network error'));
    
    await expect(fetchWordFromCustomApi('test')).rejects.toThrow();
    // Or if you're returning a default value instead of throwing:
    // const result = await fetchWordFromCustomApi('test');
    // expect(result.error).toBeTruthy();
  });
});
```

## Best Practices for API Integration

1. **Caching Strategy**
   - Implement appropriate cache TTL for different data types
   - Use service workers for offline access to API data
   - Consider IndexedDB for larger datasets

2. **Performance Optimization**
   - Batch API requests when possible
   - Implement request debouncing for search inputs
   - Lazy load data when scrolling through large lists

3. **Security Considerations**
   - Never expose API keys in client-side code
   - Implement a proxy service for sensitive APIs
   - Validate and sanitize all data from external APIs

4. **Monitoring and Logging**
   - Track API usage and performance
   - Set up alerts for API failures
   - Log API responses for debugging issues

5. **Rate Limit Management**
   - Implement token bucket algorithm for limiting requests
   - Distribute requests evenly over time
   - Provide feedback to users when rate limits are approaching

## Fallback Mechanisms

Vocab implements several fallback mechanisms to ensure uninterrupted user experience:

1. **API Priority Chain**
   - Tries multiple APIs in sequence if primary sources fail
   - Configured via `apiPriority` setting in the API manager

2. **Offline Dictionary**
   - Core vocabulary (10,000 most common words) stored locally
   - Provides basic functionality without internet connection

3. **Progressive Enhancement**
   - Basic definitions available without advanced API features
   - UI adapts to show only available data

---

For API key requests and partnership opportunities, please contact the development team at api@vocab-boost.com. 