// Dictionary and Thesaurus API service
// This service handles all interactions with the Merriam-Webster APIs

// Environment variables - these should be loaded from .env file
// Using placeholder values here, real keys will be loaded from .env
let DICTIONARY_API_KEY = 'your-dictionary-api-key';
let THESAURUS_API_KEY = 'your-thesaurus-api-key';

// Cache to track used words and prevent repetition
const usedWordsCache: Set<string> = new Set();
const MAX_CACHE_SIZE = 100;

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

// Add a word to the used words cache
const addToUsedWordsCache = (word: string) => {
  usedWordsCache.add(word.toLowerCase());
  // Keep cache size under control
  if (usedWordsCache.size > MAX_CACHE_SIZE) {
    const iterator = usedWordsCache.values();
    for (let i = 0; i < 20; i++) { // Remove 20 oldest items
      const next = iterator.next();
      if (next.done) break;
      usedWordsCache.delete(next.value);
    }
  }
};

// Check if a word has been recently used
const hasBeenUsedRecently = (word: string): boolean => {
  return usedWordsCache.has(word.toLowerCase());
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
    
    // Expanded starter words for each part of speech to get related words
    const starterWords = {
      noun: [
        'time', 'person', 'way', 'day', 'thing', 'world', 'life', 'hand', 'part', 'child',
        'place', 'work', 'book', 'case', 'water', 'money', 'group', 'fact', 'music', 'tree'
      ],
      verb: [
        'be', 'have', 'do', 'say', 'get', 'make', 'go', 'know', 'take', 'see',
        'come', 'think', 'look', 'want', 'give', 'use', 'find', 'tell', 'ask', 'work'
      ],
      adjective: [
        'good', 'new', 'first', 'last', 'long', 'great', 'little', 'own', 'other', 'old',
        'right', 'big', 'high', 'different', 'small', 'large', 'next', 'early', 'young', 'important'
      ],
      adverb: [
        'up', 'so', 'out', 'just', 'now', 'how', 'then', 'more', 'also', 'here',
        'well', 'only', 'very', 'even', 'back', 'there', 'down', 'still', 'quickly', 'slowly'
      ]
    };
    
    // Random timestamps to prevent caching
    const timestamp = Date.now() + Math.floor(Math.random() * 1000);
    
    // Choose 3 different seed words to increase variety
    const seedWords = starterWords[pos as keyof typeof starterWords] || starterWords.noun;
    const shuffledSeeds = shuffleArray([...seedWords]);
    const seedsToUse = shuffledSeeds.slice(0, 3);
    
    // Collect words from multiple seed words
    let allRelatedWords: string[] = [];
    
    for (const seedWord of seedsToUse) {
      // Use a cache-busting parameter to prevent API caching
      const response = await fetch(
        `https://www.dictionaryapi.com/api/v3/references/collegiate/json/${seedWord}?key=${DICTIONARY_API_KEY}&t=${timestamp}`
      );
      const data = await response.json();
      
      if (!data || data.length === 0 || typeof data[0] === 'string') {
        continue;
      }
      
      // Extract words of the same part of speech from the response
      const relatedWords: string[] = [];
      
      // Collect words from all entries
      data.forEach((entry: any) => {
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
      
      // Filter and add to our collection
      const cleanedWords = [...new Set(relatedWords)]
        .filter(word => word.length > 2)
        .filter(word => /^[a-zA-Z]+$/.test(word))
        .filter(word => !hasBeenUsedRecently(word)); // Skip recently used words
      
      allRelatedWords = [...allRelatedWords, ...cleanedWords];
      
      // If we have enough words already, stop making more API calls
      if (allRelatedWords.length >= count * 2) {
        break;
      }
    }
    
    // Shuffle and select the required number of words
    const selectedWords = shuffleArray(allRelatedWords).slice(0, count);
    
    // Add the selected words to the used words cache
    selectedWords.forEach(addToUsedWordsCache);
    
    // If we couldn't get enough words, use fallback
    if (selectedWords.length < count) {
      const fallbackWords = getFallbackWords(partOfSpeech, count - selectedWords.length);
      return [...selectedWords, ...fallbackWords];
    }
    
    return selectedWords;
  } catch (error) {
    console.error('Error fetching random words:', error);
    return getFallbackWords(partOfSpeech, count);
  }
};

// Get fallback words when API fails
const getFallbackWords = (partOfSpeech: string, count: number): string[] => {
  // Large collection of varied words for each part of speech
  const fallbackWords: Record<string, string[]> = {
    noun: [
      'apple', 'banana', 'car', 'dog', 'elephant', 'fish', 'guitar', 'house', 'island', 'jacket',
      'key', 'lion', 'mountain', 'notebook', 'ocean', 'piano', 'queen', 'river', 'sunset', 'tiger',
      'umbrella', 'violin', 'window', 'xylophone', 'yacht', 'zebra', 'airport', 'beach', 'coffee', 'diamond'
    ],
    verb: [
      'accept', 'begin', 'carry', 'dance', 'enjoy', 'finish', 'grow', 'hope', 'improve', 'jump',
      'knock', 'learn', 'move', 'notice', 'open', 'paint', 'question', 'remember', 'sing', 'travel',
      'understand', 'visit', 'wait', 'yell', 'zoom', 'appreciate', 'breathe', 'celebrate', 'dream', 'explore'
    ],
    adjective: [
      'amazing', 'bold', 'calm', 'delicate', 'elegant', 'fancy', 'gentle', 'happy', 'intelligent', 'jolly',
      'kind', 'logical', 'mysterious', 'narrow', 'obvious', 'peaceful', 'quiet', 'remarkable', 'simple', 'tall',
      'unique', 'valuable', 'warm', 'young', 'zealous', 'adventurous', 'brilliant', 'charming', 'delightful', 'energetic'
    ],
    adverb: [
      'actually', 'boldly', 'calmly', 'deeply', 'easily', 'faithfully', 'gently', 'happily', 'immediately', 'joyfully',
      'kindly', 'loosely', 'mysteriously', 'nearly', 'openly', 'perfectly', 'quietly', 'rapidly', 'silently', 'truly',
      'usually', 'very', 'warmly', 'yearly', 'zealously', 'accidentally', 'briefly', 'carefully', 'deliberately', 'elegantly'
    ]
  };
  
  const normalizedPos = partOfSpeech.toLowerCase();
  const pos = normalizedPos === 'adj' || normalizedPos === 'adjective' ? 'adjective' : 
              normalizedPos === 'adv' || normalizedPos === 'adverb' ? 'adverb' : 
              normalizedPos === 'v' || normalizedPos === 'verb' ? 'verb' : 'noun';
  
  // Get words that haven't been recently used
  const wordList = fallbackWords[pos] || fallbackWords.noun;
  const unusedWords = wordList.filter(word => !hasBeenUsedRecently(word));
  
  // Use unused words if possible, otherwise fall back to the full list
  const wordsToChooseFrom = unusedWords.length >= count ? unusedWords : wordList;
  
  const selectedWords = shuffleArray(wordsToChooseFrom).slice(0, count);
  
  // Add selected words to cache
  selectedWords.forEach(addToUsedWordsCache);
  
  return selectedWords;
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
  try {
    // Get synonyms for the word
    const synonyms = await getSynonyms(word);
    
    // Select one synonym as the correct answer (if available)
    let correctAnswer = '';
    if (synonyms.length > 0) {
      // Try to find a synonym that hasn't been used recently
      const unusedSynonyms = synonyms.filter(syn => !hasBeenUsedRecently(syn));
      
      if (unusedSynonyms.length > 0) {
        // Choose from unused synonyms
        correctAnswer = unusedSynonyms[Math.floor(Math.random() * unusedSynonyms.length)];
      } else {
        // If all synonyms have been used, just pick any
        correctAnswer = synonyms[Math.floor(Math.random() * synonyms.length)];
      }
    } else {
      // Fallback if no synonyms found
      correctAnswer = word;
    }
    
    // Add the correct answer to cache
    addToUsedWordsCache(correctAnswer);

    // Get contrasting words with the cache-busting timestamp
    const timestamp = Date.now() + Math.floor(Math.random() * 1000);
    let distractors: string[] = [];
    
    // Try to get antonyms with random timestamp to avoid caching
    const contrastingWords = await getContrastingWords(word, partOfSpeech, timestamp);
    
    if (contrastingWords.length > 0) {
      // Use 1-2 contrasting words to ensure variety
      const numContrasting = Math.min(2, contrastingWords.length);
      distractors = contrastingWords.slice(0, numContrasting);
    }
    
    // Fill remaining slots with random words
    const numNeeded = 3 - distractors.length;
    if (numNeeded > 0) {
      const randomWords = await getRandomWordsOfSamePartOfSpeech(partOfSpeech, numNeeded);
      distractors = [...distractors, ...randomWords];
    }
    
    // Filter out any accidental synonyms
    distractors = distractors.filter(distractor => 
      !synonyms.includes(distractor) && 
      distractor.toLowerCase() !== correctAnswer.toLowerCase()
    );
    
    // If we're still short on distractors, get more random words
    if (distractors.length < 3) {
      const additionalWords = await getRandomWordsOfSamePartOfSpeech(partOfSpeech, 3 - distractors.length);
      distractors = [...distractors, ...additionalWords];
    }
    
    // Ensure exactly 3 distractors
    distractors = distractors.slice(0, 3);
    
    // Add distractors to cache
    distractors.forEach(addToUsedWordsCache);
    
    // Combine and shuffle options
    const options = [correctAnswer, ...distractors];
    const shuffledOptions = shuffleArray(options);
    
    console.log("Quiz options for", word, ":", shuffledOptions);
    
    return {
      options: shuffledOptions,
      correctAnswer
    };
  } catch (error) {
    console.error('Error generating quiz:', error);
    
    // Fallback
    const fallbackWords = getFallbackWords(partOfSpeech, 3);
    const options = [word, ...fallbackWords];
    
    return {
      options: shuffleArray(options),
      correctAnswer: word
    };
  }
};

// Helper function to find contrasting words
const getContrastingWords = async (
  word: string, 
  partOfSpeech: string,
  timestamp: number = Date.now()
): Promise<string[]> => {
  try {
    // First try to get antonyms via the thesaurus API with cache busting
    const response = await fetch(
      `https://www.dictionaryapi.com/api/v3/references/thesaurus/json/${word}?key=${THESAURUS_API_KEY}&t=${timestamp}`
    );
    const data = await response.json();
    
    if (!data || data.length === 0 || typeof data[0] === 'string') {
      return [];
    }
    
    // Extract antonyms from the API response
    const antonyms: string[] = [];
    data.forEach((entry: any) => {
      if (entry.meta && entry.meta.ants && entry.meta.ants.length > 0) {
        entry.meta.ants.forEach((antGroup: string[]) => {
          antonyms.push(...antGroup);
        });
      }
    });
    
    // Filter out antonyms that have been used recently
    const filteredAntonyms = [...new Set(antonyms)]
      .filter(ant => !hasBeenUsedRecently(ant));
    
    // If we found antonyms, return those
    if (filteredAntonyms.length > 0) {
      // Add these to the used words cache
      filteredAntonyms.forEach(addToUsedWordsCache);
      return filteredAntonyms;
    }
    
    // If no antonyms found, try contrast seeds
    const contrastSeeds = {
      noun: ['opposite', 'contrary', 'antithesis', 'difference'],
      verb: ['stop', 'prevent', 'hinder', 'cease'],
      adjective: ['contrary', 'opposite', 'different', 'disparate'],
      adverb: ['contrarily', 'oppositely', 'differently', 'disparately']
    };
    
    const normalizedPos = partOfSpeech.toLowerCase();
    const pos = normalizedPos === 'adj' || normalizedPos === 'adjective' ? 'adjective' : 
                normalizedPos === 'adv' || normalizedPos === 'adverb' ? 'adverb' : 
                normalizedPos === 'v' || normalizedPos === 'verb' ? 'verb' : 'noun';
    
    const seedList = contrastSeeds[pos as keyof typeof contrastSeeds] || contrastSeeds.noun;
    const seedWord = seedList[Math.floor(Math.random() * seedList.length)];
    
    // Get related words to the contrast seed
    const relatedWords = await getRelatedWords(seedWord, pos);
    
    return relatedWords.filter(word => !hasBeenUsedRecently(word));
  } catch (error) {
    console.error('Error fetching contrasting words:', error);
    return [];
  }
};

export default {
  initApiKeys,
  getSynonyms,
  getRandomWordsOfSamePartOfSpeech,
  generateQuiz
}; 