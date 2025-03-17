import React, { useState } from 'react';
import '../styles/QuizScreen.css';

interface QuizOption {
  text: string;
  isCorrect: boolean;
}

interface QuizData {
  word: string;
  options: QuizOption[];
}

const QuizScreen: React.FC = () => {
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  
  const quizData: QuizData = {
    word: "recalcitrant",
    options: [
      { text: "spartan", isCorrect: false },
      { text: "combative", isCorrect: false },
      { text: "defiant", isCorrect: true },
      { text: "immense", isCorrect: false }
    ]
  };

  const handleOptionSelect = (index: number) => {
    setSelectedOption(index);
  };

  return (
    <div className="quiz-screen">
      <div className="quiz-content">
        <div className="word-display">
          <h2>{quizData.word}</h2>
        </div>
        
        <div className="quiz-options">
          {quizData.options.map((option, index) => (
            <button 
              key={index}
              className={`quiz-option ${selectedOption === index ? 
                (option.isCorrect ? 'correct' : 'incorrect') : ''}`}
              onClick={() => handleOptionSelect(index)}
              disabled={selectedOption !== null}
            >
              {option.text}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default QuizScreen; 