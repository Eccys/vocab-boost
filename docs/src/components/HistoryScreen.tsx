import React from 'react';
import '../styles/HistoryScreen.css';

interface WordEntryProps {
  word: string;
  definition: string;
  correctAnswer: string;
  userAnswer?: string;
  isCorrect: boolean;
}

const WordEntry: React.FC<WordEntryProps> = ({ 
  word, 
  definition, 
  correctAnswer, 
  userAnswer,
  isCorrect 
}) => {
  return (
    <div className={`word-entry ${isCorrect ? 'correct' : 'incorrect'}`}>
      <div className="word-header">
        <h3>{word}</h3>
        <button className="bookmark-btn" aria-label="Bookmark">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
          </svg>
        </button>
      </div>
      <p className="definition">{definition}</p>
      {!isCorrect ? (
        <>
          <p className="user-answer">Your answer: {userAnswer}</p>
          <p className="correct-answer">Correct answer: {correctAnswer}</p>
        </>
      ) : (
        <p className="correct-label">Correct: {correctAnswer}</p>
      )}
    </div>
  );
};

const HistoryScreen: React.FC = () => {
  const historyData: WordEntryProps[] = [
    {
      word: "wanton",
      definition: "deliberate and unprovoked; reckless",
      correctAnswer: "reckless",
      isCorrect: true
    },
    {
      word: "abstruse",
      definition: "difficult to understand",
      correctAnswer: "recondite",
      isCorrect: true
    },
    {
      word: "propitiate",
      definition: "to win favor or appease, often to avoid anger",
      userAnswer: "forestall",
      correctAnswer: "to win favor or appease, often to avoid anger",
      isCorrect: false
    },
    {
      word: "proclivity",
      definition: "a natural tendency or inclination",
      correctAnswer: "propensity",
      isCorrect: true
    },
    {
      word: "precarious",
      definition: "dangerously unstable or insecure, uncertainly risky",
      userAnswer: "eminent",
      correctAnswer: "dangerously unstable or insecure, uncertainly risky",
      isCorrect: false
    },
    {
      word: "progenitor",
      definition: "an ancestor or originator, a source of descent",
      correctAnswer: "ancestor",
      isCorrect: true
    }
  ];

  return (
    <div className="history-screen">
      <div className="history-content">
        {historyData.map((entry, index) => (
          <WordEntry key={index} {...entry} />
        ))}
      </div>
    </div>
  );
};

export default HistoryScreen; 