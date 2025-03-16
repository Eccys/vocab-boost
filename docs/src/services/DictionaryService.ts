// Dictionary and Thesaurus API service
// This service handles all interactions with the Merriam-Webster APIs

// Environment variables - these should be loaded from .env file
// Using dummy strings here, will be replaced during runtime
let DICTIONARY_API_KEY = 'your-dictionary-api-key';
let THESAURUS_API_KEY = 'your-thesaurus-api-key';

// Initialize API keys from environment variables if available
// This will happen during runtime, so the actual keys won't be in the source code
export const initApiKeys = () => {
  // Access environment variables safely
  if (import.meta.env.VITE_DICTIONARY_API_KEY) {
    DICTIONARY_API_KEY = import.meta.env.VITE_DICTIONARY_API_KEY;
  }
  if (import.meta.env.VITE_THESAURUS_API_KEY) {
    THESAURUS_API_KEY = import.meta.env.VITE_THESAURUS_API_KEY;
  }
};

// Thesaurus API
export const getSynonyms = async (word: string): Promise<string[]> => {
  try {
    const response = await fetch(
      `https://www.dictionaryapi.com/api/v3/references/thesaurus/json/${word}?key=${THESAURUS_API_KEY}`
    );
    const data = await response.json();
    
    // Check if we got valid results
    if (!data || data.length === 0 || typeof data[0] === 'string') {
      console.warn('No synonym results found for:', word);
      return [];
    }
    
    // Extract synonyms from the API response
    // The API returns an array of entries, and each entry has a 'meta' field containing 'syns'
    const synonyms: string[] = [];
    data.forEach((entry: any) => {
      if (entry.meta && entry.meta.syns && entry.meta.syns.length > 0) {
        entry.meta.syns.forEach((synGroup: string[]) => {
          synonyms.push(...synGroup);
        });
      }
    });
    
    return [...new Set(synonyms)]; // Return unique synonyms
  } catch (error) {
    console.error('Error fetching synonyms:', error);
    return [];
  }
};

// Dictionary API - Get random words of the same part of speech
export const getRandomWordsOfSamePartOfSpeech = async (
  partOfSpeech: string,
  count: number = 3
): Promise<string[]> => {
  // For this demo, we'll use a small set of words for each part of speech
  // In a production app, you would use the Dictionary API to get more words
  
  const commonWords: Record<string, string[]> = {
    noun: ['apple', 'car', 'house', 'book', 'city', 'dog', 'time', 'year', 'person', 'way'],
    verb: ['run', 'eat', 'sleep', 'write', 'read', 'talk', 'walk', 'play', 'work', 'think'],
    adjective: ['happy', 'sad', 'big', 'small', 'good', 'bad', 'hot', 'cold', 'new', 'old'],
    adverb: ['quickly', 'slowly', 'carefully', 'easily', 'loudly', 'quietly', 'well', 'badly', 'never', 'always']
  };
  
  // Normalize the part of speech to match our keys
  const normalizedPos = partOfSpeech.toLowerCase();
  const pos = normalizedPos === 'adj' || normalizedPos === 'adjective' ? 'adjective' : 
              normalizedPos === 'adv' || normalizedPos === 'adverb' ? 'adverb' : 
              normalizedPos === 'v' || normalizedPos === 'verb' ? 'verb' : 'noun';
  
  // Get the list of words for this part of speech
  const wordList = commonWords[pos] || commonWords.noun;
  
  // Shuffle and return the requested number of words
  return shuffleArray(wordList).slice(0, count);
};

// Helper function to shuffle array
const shuffleArray = <T>(array: T[]): T[] => {
  const shuffled = [...array];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
};

// Function to get a sample quiz for a word
export const generateQuiz = async (
  word: string, 
  partOfSpeech: string
): Promise<{ options: string[], correctAnswer: string }> => {
  // Get synonyms for the word
  const synonyms = await getSynonyms(word);
  
  // Select one synonym as the correct answer (if available)
  let correctAnswer = '';
  if (synonyms.length > 0) {
    correctAnswer = synonyms[Math.floor(Math.random() * synonyms.length)];
  } else {
    // Fallback if no synonyms found
    correctAnswer = word;
  }
  
  // Get random words of the same part of speech
  const randomWords = await getRandomWordsOfSamePartOfSpeech(partOfSpeech, 3);
  
  // Combine the correct answer with random words
  const options = [correctAnswer, ...randomWords];
  
  // Shuffle the options
  const shuffledOptions = shuffleArray(options);
  
  return {
    options: shuffledOptions,
    correctAnswer
  };
};

export default {
  initApiKeys,
  getSynonyms,
  getRandomWordsOfSamePartOfSpeech,
  generateQuiz
}; 