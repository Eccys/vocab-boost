import React from 'react';
import '../styles/HowItWorks.css';

const HowItWorks: React.FC = () => {
  const steps = [
    {
      number: 1,
      title: 'Learn a New Word Daily',
      description: 'Start each day with a new vocabulary word, complete with definition, pronunciation, and usage examples.'
    },
    {
      number: 2,
      title: 'Test Your Knowledge',
      description: 'Take quizzes to reinforce your learning and commit words to long-term memory.'
    },
    {
      number: 3,
      title: 'Review at Optimal Times',
      description: "Our machine learning algorithm schedules reviews when you're most likely to forget, optimizing your retention."
    },
    {
      number: 4,
      title: 'Track Your Progress',
      description: 'Monitor your learning journey with detailed statistics and visualizations of your growing vocabulary.'
    }
  ];

  return (
    <section id="how-it-works" className="how-it-works">
      <div className="container">
        <h2 className="section-title" data-aos="fade-up">How It Works</h2>
        <div className="steps">
          {steps.map((step, index) => (
            <div 
              key={index} 
              className="step" 
              data-aos="fade-right" 
              data-aos-delay={index * 100}
            >
              <div className="step-number">{step.number}</div>
              <div className="step-content">
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default HowItWorks; 