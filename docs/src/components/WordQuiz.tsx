import React, { useState, useEffect } from 'react';
import '../styles/WordQuiz.css';
import DictionaryService from '../services/DictionaryService';

interface WordQuizProps {
  word: string;
  partOfSpeech: string;
  onClose: () => void;
}

const WordQuiz: React.FC<WordQuizProps> = ({ word, partOfSpeech, onClose }) => {
  const [loading, setLoading] = useState<boolean>(true);
  const [options, setOptions] = useState<string[]>([]);
  const [correctAnswer, setCorrectAnswer] = useState<string>('');
  const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
  const [showResult, setShowResult] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchQuizData = async () => {
      try {
        setLoading(true);
        // Initialize API keys
        DictionaryService.initApiKeys();
        
        // Generate quiz options
        const quiz = await DictionaryService.generateQuiz(word, partOfSpeech);
        
        setOptions(quiz.options);
        setCorrectAnswer(quiz.correctAnswer);
        setLoading(false);
      } catch (err) {
        console.error('Error generating quiz:', err);
        setError('Could not load quiz questions. Please try again later.');
        setLoading(false);
      }
    };

    fetchQuizData();
  }, [word, partOfSpeech]);

  const handleOptionClick = (option: string) => {
    if (!showResult) {
      setSelectedAnswer(option);
      setShowResult(true);
    }
  };

  const getOptionClass = (option: string) => {
    if (!showResult) return '';
    
    if (option === correctAnswer) {
      return 'correct';
    }
    
    if (option === selectedAnswer && option !== correctAnswer) {
      return 'incorrect';
    }
    
    return '';
  };

  const handleTryAgain = () => {
    setSelectedAnswer(null);
    setShowResult(false);
  };

  return (
    <div className="quiz-overlay">
      <div className="quiz-modal">
        <div className="quiz-header">
          <h2>Word Quiz</h2>
          <button className="close-button" onClick={onClose}>×</button>
        </div>
        
        {loading ? (
          <div className="quiz-loading">
            <div className="spinner"></div>
            <p>Loading quiz...</p>
          </div>
        ) : error ? (
          <div className="quiz-error">
            <p>{error}</p>
            <button className="quiz-button primary" onClick={onClose}>Close</button>
          </div>
        ) : (
          <>
            <div className="quiz-question">
              <p>Which of the following is a synonym for <strong>{word}</strong>?</p>
            </div>
            
            <div className="quiz-options">
              {options.map((option, index) => (
                <button
                  key={index}
                  className={`quiz-option ${getOptionClass(option)}`}
                  onClick={() => handleOptionClick(option)}
                  disabled={showResult}
                >
                  {option}
                </button>
              ))}
            </div>
            
            {showResult && (
              <div className="quiz-result">
                {selectedAnswer === correctAnswer ? (
                  <p className="success">Correct! Nice job.</p>
                ) : (
                  <p className="failure">
                    Sorry, the correct answer is <strong>{correctAnswer}</strong>.
                  </p>
                )}
                
                <div className="quiz-actions">
                  <button className="quiz-button secondary" onClick={handleTryAgain}>
                    Try Again
                  </button>
                  <button className="quiz-button primary" onClick={onClose}>
                    Close
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default WordQuiz; 