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

const WordOfDay: React.FC = () => {
  const [isLoaded, setIsLoaded] = useState(false);
  
  useEffect(() => {
    // Set to loaded immediately on mount
    setIsLoaded(true);
  }, []);
  
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
            <button className="primary-btn">Add to Favorites</button>
            <button className="secondary-btn">Practice Quiz</button>
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
    </section>
  );
};

export default WordOfDay; 