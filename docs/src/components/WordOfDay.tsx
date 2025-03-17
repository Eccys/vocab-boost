import React, { useState, useEffect } from 'react';
import '../styles/WordOfDay.css';
import WordQuiz from './WordQuiz';

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

const WordOfDay: React.FC = () => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [showQuiz, setShowQuiz] = useState(false);
  
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
  
  // Open the quiz modal
  const handlePracticeQuiz = () => {
    setShowQuiz(true);
  };
  
  // Close the quiz modal
  const handleCloseQuiz = () => {
    setShowQuiz(false);
  };
  
  return (
    <section className="word-of-day-section">
      <div className="container">
        <h1 className="section-title">Word of the Day</h1>
        
        <div className={`word-of-day-card ${isLoaded ? 'loaded' : ''}`}>
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
            <button className="secondary-btn" onClick={handlePracticeQuiz}>
              Practice Quiz
            </button>
          </div>
        </div>
        
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
      </div>
      
      {/* Render the quiz modal when showQuiz is true */}
      {showQuiz && (
        <WordQuiz
          word={wordData.word}
          partOfSpeech={wordData.type}
          onClose={handleCloseQuiz}
        />
      )}
    </section>
  );
};

export default WordOfDay; 