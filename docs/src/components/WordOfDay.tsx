import React, { useState, useEffect } from 'react';
import '../styles/WordOfDay.css';

// Sample word data
const wordData = {
  word: 'Ephemeral',
  type: 'adjective',
  pronunciation: 'ih-FEM-er-uhl',
  definition: 'Lasting for a very short time',
  example: 'The beauty of cherry blossoms is ephemeral, lasting only a few days.',
  synonyms: ['fleeting', 'transitory', 'momentary', 'brief', 'short-lived'],
  antonyms: ['permanent', 'enduring', 'eternal', 'everlasting', 'perpetual']
};

// Additional adjectives for quiz distractors (would be fetched from API in production)
const randomAdjectivePool = [
  'abundant', 'adorable', 'adventurous', 'aggressive', 'ancient', 
  'anxious', 'beautiful', 'boring', 'brave', 'bright', 'calm', 
  'cautious', 'cheerful', 'clever', 'colorful', 'comfortable', 
  'cooperative', 'courageous', 'crazy', 'dangerous', 'delightful', 
  'determined', 'different', 'dramatic', 'eager', 'elegant', 'embarrassed',
  'famous', 'fantastic', 'fascinating', 'fierce', 'frail', 'friendly',
  'graceful', 'grateful', 'handsome', 'healthy', 'helpful', 'hilarious',
  'important', 'innocent', 'intelligent', 'jealous', 'lazy', 'magnificent',
  'mysterious', 'nervous', 'outstanding', 'powerful', 'precious', 'puzzled',
  'remarkable', 'shiny', 'splendid', 'spotless', 'successful', 'surprising',
  'talented', 'thoughtful', 'victorious', 'wealthy', 'wonderful', 'worried'
];

// Interface for quiz questions
interface QuizQuestion {
  question: string;
  options: string[];
  correctAnswerIndex: number;
}

// Quiz states
type QuizState = 'not-started' | 'in-progress' | 'completed';

const WordOfDay: React.FC = () => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [quizState, setQuizState] = useState<QuizState>('not-started');
  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [score, setScore] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<number | null>(null);
  const [isAnswerCorrect, setIsAnswerCorrect] = useState<boolean | null>(null);
  const [isAnswerSubmitted, setIsAnswerSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  
  // API keys would be stored securely in environment variables in production
  const thesaurusApiKey = 'your-api-key'; // Replace with your actual key
  const dictionaryApiKey = 'your-api-key'; // Replace with your actual key
  
  useEffect(() => {
    // Set to loaded immediately on mount
    setIsLoaded(true);
  }, []);

  // Generate Merriam-Webster dictionary URL for the current word
  const getMerriamWebsterUrl = (word: string) => {
    // Convert to lowercase and handle any spaces
    const formattedWord = word.toLowerCase().replace(/\s+/g, '-');
    return `https://www.merriam-webster.com/dictionary/${formattedWord}`;
  };
  
  // In a production app, this would fetch from the API
  // Here we're simulating the API response
  const fetchSynonymsFromAPI = async (word: string) => {
    setIsLoading(true);
    
    try {
      // In production code, this would be a real API call:
      // const response = await fetch(
      //   `https://www.dictionaryapi.com/api/v3/references/thesaurus/json/${word}?key=${thesaurusApiKey}`
      // );
      // const data = await response.json();
      // return data[0]?.meta?.syns[0] || [];
      
      // For demo purposes, we'll use the sample data
      return new Promise<string[]>((resolve) => {
        setTimeout(() => {
          resolve(wordData.synonyms);
        }, 500);
      });
    } catch (error) {
      console.error('Error fetching synonyms:', error);
      return [];
    } finally {
      setIsLoading(false);
    }
  };
  
  // In a production app, this would fetch from the API
  // Here we're simulating the API response
  const fetchRandomWordsOfSameType = async (partOfSpeech: string, count: number) => {
    setIsLoading(true);
    
    try {
      // In production code, this would be a real API call
      // For demo purposes, we'll use the sample data
      return new Promise<string[]>((resolve) => {
        setTimeout(() => {
          // Filter out any words that are in the synonyms list
          const filteredPool = randomAdjectivePool.filter(
            word => !wordData.synonyms.includes(word)
          );
          
          // Get random words from the pool
          const shuffled = [...filteredPool].sort(() => 0.5 - Math.random());
          resolve(shuffled.slice(0, count));
        }, 500);
      });
    } catch (error) {
      console.error('Error fetching random words:', error);
      return [];
    } finally {
      setIsLoading(false);
    }
  };
  
  const generateQuizQuestions = async () => {
    setIsLoading(true);
    
    try {
      // Get synonyms from API (simulated)
      const synonyms = await fetchSynonymsFromAPI(wordData.word);
      
      // Generate 5 questions
      const newQuestions: QuizQuestion[] = [];
      
      for (let i = 0; i < 5; i++) {
        // For each question, we'll use a different synonym if available
        const synonymIndex = i % synonyms.length;
        const correctAnswer = synonyms[synonymIndex];
        
        // Get 3 random words of the same type for distractors
        const distractors = await fetchRandomWordsOfSameType(wordData.type, 3);
        
        // Combine correct answer with distractors
        const options = [correctAnswer, ...distractors];
        
        // Shuffle options
        const shuffledOptions = [...options].sort(() => 0.5 - Math.random());
        
        // Find index of correct answer in shuffled options
        const correctAnswerIndex = shuffledOptions.indexOf(correctAnswer);
        
        newQuestions.push({
          question: `Which word is a synonym for "${wordData.word}"?`,
          options: shuffledOptions,
          correctAnswerIndex: correctAnswerIndex
        });
      }
      
      setQuestions(newQuestions);
      setCurrentQuestionIndex(0);
      setScore(0);
      setSelectedAnswer(null);
      setIsAnswerCorrect(null);
      setIsAnswerSubmitted(false);
      setQuizState('in-progress');
    } catch (error) {
      console.error('Error generating quiz:', error);
    } finally {
      setIsLoading(false);
    }
  };
  
  const handleStartQuiz = () => {
    generateQuizQuestions();
  };
  
  const handleSelectAnswer = (index: number) => {
    if (!isAnswerSubmitted) {
      setSelectedAnswer(index);
    }
  };
  
  const handleSubmitAnswer = () => {
    if (selectedAnswer === null) return;
    
    const isCorrect = selectedAnswer === questions[currentQuestionIndex].correctAnswerIndex;
    setIsAnswerCorrect(isCorrect);
    
    if (isCorrect) {
      setScore(prevScore => prevScore + 1);
    }
    
    setIsAnswerSubmitted(true);
  };
  
  const handleNextQuestion = () => {
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(prevIndex => prevIndex + 1);
      setSelectedAnswer(null);
      setIsAnswerCorrect(null);
      setIsAnswerSubmitted(false);
    } else {
      setQuizState('completed');
    }
  };
  
  const handleRestartQuiz = () => {
    generateQuizQuestions();
  };
  
  const renderQuizContent = () => {
    if (isLoading) {
      return (
        <div className="quiz-loading">
          <p>Loading quiz questions...</p>
          <div className="loading-spinner"></div>
        </div>
      );
    }
    
    if (quizState === 'not-started') {
      return (
        <div className="quiz-start">
          <h3>Practice Quiz</h3>
          <p>Test your knowledge of synonyms for "{wordData.word}".</p>
          <button className="primary-btn" onClick={handleStartQuiz}>Start Quiz</button>
        </div>
      );
    }
    
    if (quizState === 'in-progress' && questions.length > 0) {
      const currentQuestion = questions[currentQuestionIndex];
      
      return (
        <div className="quiz-question">
          <div className="quiz-progress">
            <span>Question {currentQuestionIndex + 1} of {questions.length}</span>
            <span>Score: {score}</span>
          </div>
          
          <h3>{currentQuestion.question}</h3>
          
          <div className="quiz-options">
            {currentQuestion.options.map((option, index) => (
              <div 
                key={index}
                className={`quiz-option ${selectedAnswer === index ? 'selected' : ''} ${
                  isAnswerSubmitted 
                    ? index === currentQuestion.correctAnswerIndex 
                      ? 'correct' 
                      : selectedAnswer === index 
                        ? 'incorrect' 
                        : ''
                    : ''
                }`}
                onClick={() => handleSelectAnswer(index)}
              >
                <span className="option-letter">{String.fromCharCode(65 + index)}</span>
                <span className="option-text">{option}</span>
              </div>
            ))}
          </div>
          
          {isAnswerSubmitted ? (
            <div className="quiz-feedback">
              <p className={isAnswerCorrect ? 'correct-answer' : 'incorrect-answer'}>
                {isAnswerCorrect 
                  ? 'Correct!' 
                  : `Incorrect. The correct answer is ${currentQuestion.options[currentQuestion.correctAnswerIndex]}.`}
              </p>
              <button className="primary-btn" onClick={handleNextQuestion}>
                {currentQuestionIndex < questions.length - 1 ? 'Next Question' : 'See Results'}
              </button>
            </div>
          ) : (
            <button 
              className="primary-btn" 
              onClick={handleSubmitAnswer}
              disabled={selectedAnswer === null}
            >
              Submit Answer
            </button>
          )}
        </div>
      );
    }
    
    if (quizState === 'completed') {
      return (
        <div className="quiz-results">
          <h3>Quiz Completed!</h3>
          <p className="final-score">Your score: {score} out of {questions.length}</p>
          <p className="score-percentage">({Math.round((score / questions.length) * 100)}%)</p>
          
          <div className="score-message">
            {score === questions.length ? (
              <p>Perfect score! You have an excellent grasp of synonyms!</p>
            ) : score >= questions.length * 0.8 ? (
              <p>Great job! You have a strong understanding of synonyms.</p>
            ) : score >= questions.length * 0.6 ? (
              <p>Good work! Keep practicing to improve your vocabulary.</p>
            ) : (
              <p>Keep learning! Practice makes perfect.</p>
            )}
          </div>
          
          <div className="quiz-actions">
            <button className="primary-btn" onClick={handleRestartQuiz}>Try Again</button>
            <button className="secondary-btn" onClick={() => setQuizState('not-started')}>Close Quiz</button>
          </div>
        </div>
      );
    }
    
    return null;
  };
  
  return (
    <section className="word-of-day-section">
      <div className="container">
        <h1 className="section-title">Word of the Day</h1>
        
        <div className={`word-of-day-card ${isLoaded ? 'loaded' : ''}`}>
          {quizState === 'not-started' ? (
            <>
              <div className="word-header">
                <h2>{wordData.word}</h2>
                <p className="word-type"><em>{wordData.type}</em> | <span className="pronunciation">{wordData.pronunciation}</span></p>
              </div>
              
              <div className="word-content">
                <div className="definition-section">
                  <h3>Definition</h3>
                  <p>{wordData.definition}</p>
                </div>
                
                <div className="example-section">
                  <h3>Example</h3>
                  <p>"{wordData.example}"</p>
                </div>
                
                <div className="related-words">
                  <div className="synonyms">
                    <h3>Synonyms</h3>
                    <ul>
                      {wordData.synonyms.map((synonym, index) => (
                        <li key={index}>{synonym}</li>
                      ))}
                    </ul>
                  </div>
                  
                  <div className="antonyms">
                    <h3>Antonyms</h3>
                    <ul>
                      {wordData.antonyms.map((antonym, index) => (
                        <li key={index}>{antonym}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
              
              <div className="word-actions">
                <a 
                  href={getMerriamWebsterUrl(wordData.word)} 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className="primary-btn"
                >
                  View Context
                </a>
                <button className="secondary-btn" onClick={handleStartQuiz}>Practice Quiz</button>
              </div>
            </>
          ) : (
            <div className="quiz-container">
              {renderQuizContent()}
            </div>
          )}
        </div>
        
        {quizState === 'not-started' && (
          <div className="previous-words">
            <h3>Previous Words</h3>
            <div className="word-list">
              <div className="word-item">Serendipity</div>
              <div className="word-item">Mellifluous</div>
              <div className="word-item">Ubiquitous</div>
              <div className="word-item">Panacea</div>
              <div className="word-item">Eloquent</div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
};

export default WordOfDay; 