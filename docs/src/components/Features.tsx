import React, { useEffect, useState, useRef } from 'react';
import '../styles/Features.css';
import { motion } from 'framer-motion';
import { Tilt } from './ui/Tilt';

const Features: React.FC = () => {
  const features = [
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M12 2c5.52 0 10 4.48 10 10s-4.48 10-10 10S2 17.52 2 12 6.48 2 12 2zm0 18c4.42 0 8-3.58 8-8s-3.58-8-8-8-8 3.58-8 8 3.58 8 8 8zm5-9h-4V7h-2v4H7v2h4v4h2v-4h4v-2z"/>
        </svg>
      ),
      title: 'Daily Word',
      description: 'Learn a new vocabulary word every day with comprehensive definitions, examples, and usage contexts.'
    },
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm0 2v12h16V6H4zm3 3h2v6H7V9zm4 0h2v6h-2V9zm4 0h2v6h-2V9z"/>
        </svg>
      ),
      title: 'Interactive Quizzes',
      description: 'Test your knowledge with customizable quizzes based on your learning history.'
    },
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10zm0-2a8 8 0 1 0 0-16 8 8 0 0 0 0 16zm1-8h4v2h-6V7h2v5z"/>
        </svg>
      ),
      title: 'Machine Learning',
      description: 'Advanced algorithm that optimizes vocabulary retention by scheduling reviews at optimal intervals.'
    },
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M5 3v16h16v2H3V3h2zm15.293 3.293l1.414 1.414L16 13.414l-3-2.999-4.293 4.292-1.414-1.414L13 7.586l3 2.999 4.293-4.292z"/>
        </svg>
      ),
      title: 'Progress Tracking',
      description: 'Detailed statistics to monitor your learning progress over time and stay motivated.'
    },
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M17 18v-2h.5a3.5 3.5 0 1 0-2.5-5.95V10a2 2 0 0 0-4 0v-.5a3.5 3.5 0 1 0-3.5 3.5H8v5H3v2h18v-2h-4zm-8-2v-3h-.5a1.5 1.5 0 1 1 1.5-1.5v1.5h2v3h3v-3h-.5a1.5 1.5 0 1 1 1.5-1.5v1.5h2v3h3v-3h-.5a3.5 3.5 0 1 0-3.5 3.5H16v2H8v-2z"/>
        </svg>
      ),
      title: 'Cross-Device Sync',
      description: 'Seamlessly sync your progress, bookmarks, and settings across multiple devices.'
    },
    {
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="48" height="48">
          <path fill="none" d="M0 0h24v24H0z"/>
          <path d="M3 3h18a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm17 8H4v8h16v-8zm0-2V5H4v4h16zm-5-3h4v2h-6V7h2v5z"/>
        </svg>
      ),
      title: 'Offline Mode',
      description: 'Full functionality available without internet connection for learning anytime, anywhere.'
    }
  ];

  // Component for feature card with tilt and spotlight effects
  const FeatureCard: React.FC<{
    feature: typeof features[0];
    index: number;
  }> = ({ feature, index }) => {
    const [isHovered, setIsHovered] = useState(false);
    const [isLoaded, setIsLoaded] = useState(false);
    const cardRef = useRef<HTMLDivElement>(null);
    
    // Wait for the AOS animation to complete before enabling tilt
    useEffect(() => {
      const timer = setTimeout(() => {
        setIsLoaded(true);
      }, 500 + (index * 100)); // Match the AOS animation timing plus a little buffer
      
      return () => clearTimeout(timer);
    }, [index]);
    
    // Handle spotlight effect
    const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
      if (!cardRef.current) return;
      
      const rect = cardRef.current.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 100;
      const y = ((e.clientY - rect.top) / rect.height) * 100;
      
      // Find the inner element and set CSS variables
      const inner = cardRef.current.querySelector('.feature-card-inner') as HTMLElement;
      if (inner) {
        inner.style.setProperty('--x', `${x}%`);
        inner.style.setProperty('--y', `${y}%`);
      }
    };
    
    return (
      <div 
        className="feature-card-container"
        data-aos="fade-up"
        data-aos-delay={index * 100}
        ref={cardRef}
        onMouseMove={handleMouseMove}
      >
        {isLoaded ? (
          <Tilt 
            className="feature-card"
            rotationFactor={10}
            springOptions={{
              stiffness: 300,
              damping: 15
            }}
          >
            <motion.div
              className="feature-card-inner"
              onMouseEnter={() => setIsHovered(true)}
              onMouseLeave={() => setIsHovered(false)}
              whileHover={{
                scale: 1.02,
                transition: { type: "spring", stiffness: 400, damping: 10 }
              }}
            >
              <div className="feature-card-spotlight"></div>
              <div className="feature-icon">
                {feature.icon}
              </div>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </motion.div>
          </Tilt>
        ) : (
          <div className="feature-card">
            <div className="feature-card-inner">
              <div className="feature-card-spotlight"></div>
              <div className="feature-icon">
                {feature.icon}
              </div>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <section id="features" className="features">
      <div className="container">
        <h2 className="section-title" data-aos="fade-up">Powerful Features</h2>
        <div className="features-grid">
          {features.map((feature, index) => (
            <FeatureCard key={index} feature={feature} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
};

export default Features; 