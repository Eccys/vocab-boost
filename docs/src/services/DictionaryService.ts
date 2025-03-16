// Dictionary and Thesaurus API service
// This service handles all interactions with the Merriam-Webster APIs

// Environment variables - these should be loaded from .env file
// Using the provided keys for immediate use
let DICTIONARY_API_KEY = '3762541c-f0ec-4fe7-a364-0b76c0fc2cc3';
let THESAURUS_API_KEY = 'ecd56eea-18e1-409a-a066-19af340754f3';

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

// Dictionary API - Get random words of the same part of speech using the API
export const getRandomWordsOfSamePartOfSpeech = async (
  partOfSpeech: string,
  count: number = 3
): Promise<string[]> => {
  try {
    // Map the part of speech to Merriam-Webster's format
    const normalizedPos = partOfSpeech.toLowerCase();
    const pos = normalizedPos === 'adj' || normalizedPos === 'adjective' ? 'adjective' : 
                normalizedPos === 'adv' || normalizedPos === 'adverb' ? 'adverb' : 
                normalizedPos === 'v' || normalizedPos === 'verb' ? 'verb' : 'noun';
    
    // Common starter words for each part of speech to get related words
    const starterWords = {
      noun: ['time', 'person', 'way', 'day', 'thing', 'world', 'life', 'hand', 'part', 'child'],
      verb: ['be', 'have', 'do', 'say', 'get', 'make', 'go', 'know', 'take', 'see'],
      adjective: ['good', 'new', 'first', 'last', 'long', 'great', 'little', 'own', 'other', 'old'],
      adverb: ['up', 'so', 'out', 'just', 'now', 'how', 'then', 'more', 'also', 'here']
    };
    
    // Randomly select a starter word based on part of speech
    const seedWords = starterWords[pos as keyof typeof starterWords] || starterWords.noun;
    const randomSeedWord = seedWords[Math.floor(Math.random() * seedWords.length)];
    
    // Query the Dictionary API with the seed word
    const response = await fetch(
      `https://www.dictionaryapi.com/api/v3/references/collegiate/json/${randomSeedWord}?key=${DICTIONARY_API_KEY}`
    );
    const data = await response.json();
    
    if (!data || data.length === 0 || typeof data[0] === 'string') {
      console.warn('No dictionary results found for seed word:', randomSeedWord);
      // As a fallback, use suggestion words if they exist
      if (Array.isArray(data) && data.length > 0 && typeof data[0] === 'string') {
        return shuffleArray(data.filter(word => word.length > 2)).slice(0, count);
      }
      throw new Error('Failed to get related words');
    }
    
    // Extract words of the same part of speech from the response
    // The API returns an array of entries, and each entry has related words and cross-references
    const relatedWords: string[] = [];
    
    // Collect words from all entries
    data.forEach((entry: any) => {
      // Check if this entry matches our desired part of speech
      const entryPos = entry.fl ? entry.fl.toLowerCase() : '';
      
      if (entryPos && (entryPos.includes(pos) || pos.includes(entryPos))) {
        // Add headword if it exists
        if (entry.hwi && entry.hwi.hw) {
          relatedWords.push(entry.hwi.hw.replace(/\*/g, ''));
        }
        
        // Check for cross-references
        if (entry.dros && Array.isArray(entry.dros)) {
          entry.dros.forEach((dro: any) => {
            if (dro.drp) {
              relatedWords.push(dro.drp.replace(/\*/g, ''));
            }
          });
        }
        
        // Add stems if they exist
        if (entry.meta && entry.meta.stems && Array.isArray(entry.meta.stems)) {
          relatedWords.push(...entry.meta.stems);
        }
        
        // Add synonyms if they exist in shortdef
        if (entry.shortdef && Array.isArray(entry.shortdef)) {
          entry.shortdef.forEach((def: string) => {
            // Extract words from definition that might be synonyms (words after ":")
            const colonIndex = def.indexOf(':');
            if (colonIndex !== -1) {
              const potentialSynonyms = def.substring(colonIndex + 1).split(/,\s*/);
              relatedWords.push(...potentialSynonyms.map(s => s.trim()));
            }
          });
        }
      }
    });
    
    // Make sure all words are unique and remove any with special characters
    const cleanedWords = [...new Set(relatedWords)]
      .filter(word => word.length > 2) // Filter out very short words
      .filter(word => /^[a-zA-Z]+$/.test(word)); // Filter out words with non-alphabetic characters
    
    // If we don't have enough words, make another API call with a different seed
    if (cleanedWords.length < count && seedWords.length > 1) {
      // Try a different seed word
      const remainingSeedWords = seedWords.filter(word => word !== randomSeedWord);
      const alternativeSeed = remainingSeedWords[Math.floor(Math.random() * remainingSeedWords.length)];
      
      const additionalWords = await getRelatedWords(alternativeSeed, pos);
      cleanedWords.push(...additionalWords.filter(word => !cleanedWords.includes(word)));
    }
    
    // Shuffle and return the requested number of words
    return shuffleArray(cleanedWords).slice(0, count);
  } catch (error) {
    console.error('Error fetching random words:', error);
    
    // Fallback to basic common words for each part of speech
    const fallbackWords: Record<string, string[]> = {
      noun: ['apple', 'car', 'house', 'book', 'city', 'dog', 'time', 'year', 'person', 'way'],
      verb: ['run', 'eat', 'sleep', 'write', 'read', 'talk', 'walk', 'play', 'work', 'think'],
      adjective: ['happy', 'sad', 'big', 'small', 'good', 'bad', 'hot', 'cold', 'new', 'old'],
      adverb: ['quickly', 'slowly', 'carefully', 'easily', 'loudly', 'quietly', 'well', 'badly', 'never', 'always']
    };
    
    const normalizedPos = partOfSpeech.toLowerCase();
    const pos = normalizedPos === 'adj' || normalizedPos === 'adjective' ? 'adjective' : 
                normalizedPos === 'adv' || normalizedPos === 'adverb' ? 'adverb' : 
                normalizedPos === 'v' || normalizedPos === 'verb' ? 'verb' : 'noun';
    
    // Get the list of words for this part of speech
    const wordList = fallbackWords[pos] || fallbackWords.noun;
    
    console.warn('Using fallback word list due to API error');
    return shuffleArray(wordList).slice(0, count);
  }
};

// Helper function to get related words for a given word and part of speech
const getRelatedWords = async (word: string, partOfSpeech: string): Promise<string[]> => {
  try {
    const response = await fetch(
      `https://www.dictionaryapi.com/api/v3/references/collegiate/json/${word}?key=${DICTIONARY_API_KEY}`
    );
    const data = await response.json();
    
    if (!data || data.length === 0 || typeof data[0] === 'string') {
      return [];
    }
    
    const relatedWords: string[] = [];
    
    data.forEach((entry: any) => {
      const entryPos = entry.fl ? entry.fl.toLowerCase() : '';
      
      if (entryPos && (entryPos.includes(partOfSpeech) || partOfSpeech.includes(entryPos))) {
        if (entry.meta && entry.meta.stems) {
          relatedWords.push(...entry.meta.stems);
        }
      }
    });
    
    return [...new Set(relatedWords)]
      .filter(word => word.length > 2)
      .filter(word => /^[a-zA-Z]+$/.test(word));
  } catch (error) {
    console.error('Error fetching related words:', error);
    return [];
  }
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