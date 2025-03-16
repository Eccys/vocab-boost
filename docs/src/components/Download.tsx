import React, { useState, useEffect } from 'react';
import '../styles/Download.css';
import HistoryScreen from './HistoryScreen';
import QuizScreen from './QuizScreen';
import StatsScreen from './StatsScreen';
import SettingsScreen from './SettingsScreen';

const Download: React.FC = () => {
  // Initialize with 0 to show Settings screen first
  const [currentScreenIndex, setCurrentScreenIndex] = useState(0);
  const [userInteracted, setUserInteracted] = useState(false);
  
  const screens = [
    { id: 'settings', name: 'Settings' },
    { id: 'history', name: 'History' },
    { id: 'stats', name: 'Statistics' },
    { id: 'quiz', name: 'Quiz' }
  ];
  
  // Function to go to a specific screen
  const goToScreen = (index: number) => {
    // Ensure index is within bounds
    if (index < 0) index = screens.length - 1;
    if (index >= screens.length) index = 0;
    
    setCurrentScreenIndex(index);
  };
  
  // Auto-rotate screens
  useEffect(() => {
    if (userInteracted) return;
    
    const interval = setInterval(() => {
      goToScreen(currentScreenIndex + 1);
    }, 5000);
    
    return () => clearInterval(interval);
  }, [currentScreenIndex, userInteracted]);
  
  // Handle user interaction
  const handleNavigation = (index: number) => {
    goToScreen(index);
    setUserInteracted(true);
  };
  
  // Handle arrow navigation
  const handleArrowClick = (direction: 'prev' | 'next') => {
    if (direction === 'prev') {
      goToScreen(currentScreenIndex - 1);
    } else {
      goToScreen(currentScreenIndex + 1);
    }
    setUserInteracted(true);
  };

  return (
    <section id="download" className="download">
      <div className="container">
        <div className="download-content" data-aos="fade-right">
          <h2 className="section-title">Ready to Boost Your Vocabulary?</h2>
          <p>Download Vocab now and start your journey to a richer vocabulary today. It's 100% free and open source!</p>
          <div className="download-buttons">
            <a href="#" className="download-btn google-play">
              <img src="/images/google-play.svg" alt="Google Play" />
              <span>Get it on Google Play</span>
            </a>
            <a href="https://github.com/eccys/vocab-boost" className="download-btn github" target="_blank" rel="noopener noreferrer">
              <img src="/images/github.svg" alt="GitHub" />
              <span>View on GitHub</span>
            </a>
          </div>
        </div>
        
        <div className="download-image app-mockup-display" data-aos="fade-up">
          <div className="app-screens-carousel">
            {screens.map((screen, index) => (
              <div 
                key={screen.id} 
                className={`screen ${screen.id}-screen ${index === currentScreenIndex ? 'active' : ''}`}
              >
                <div className="screen-header">
                  <div className="back-button">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                    <span>{screen.name}</span>
                  </div>
                  <div className="actions">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <circle cx="12" cy="12" r="1"></circle>
                      <circle cx="12" cy="5" r="1"></circle>
                      <circle cx="12" cy="19" r="1"></circle>
                    </svg>
                  </div>
                </div>
                <div className="screen-content">
                  {screen.id === 'settings' ? (
                    <SettingsScreen />
                  ) : screen.id === 'history' ? (
                    <HistoryScreen />
                  ) : screen.id === 'quiz' ? (
                    <QuizScreen />
                  ) : screen.id === 'stats' ? (
                    <StatsScreen />
                  ) : (
                    <div className="screen-placeholder">
                      {screen.name} Screen
                    </div>
                  )}
                </div>
              </div>
            ))}
            
            <div className="carousel-nav">
              {screens.map((screen, index) => (
                <span 
                  key={index} 
                  className={`nav-dot ${index === currentScreenIndex ? 'active' : ''}`}
                  onClick={() => handleNavigation(index)}
                ></span>
              ))}
            </div>
          </div>
          
          <div className="carousel-arrows">
            <button 
              className="carousel-arrow prev-arrow" 
              onClick={() => handleArrowClick('prev')}
              aria-label="Previous screen"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 18 9 12 15 6"></polyline>
              </svg>
            </button>
            <button 
              className="carousel-arrow next-arrow" 
              onClick={() => handleArrowClick('next')}
              aria-label="Next screen"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Download; 