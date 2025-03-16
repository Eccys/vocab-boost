import React from 'react';
import { Link } from 'react-router-dom';
import '../styles/Footer.css';

const Footer: React.FC = () => {
  // Smooth scroll to section
  const scrollToSection = (e: React.MouseEvent, sectionId: string) => {
    e.preventDefault();
    const section = document.getElementById(sectionId);
    
    // Update URL hash without full page reload
    if (history.pushState) {
      history.pushState(null, '', `#${sectionId}`);
    } else {
      window.location.hash = sectionId;
    }
    
    if (section) {
      // Calculate accurate position
      const rect = section.getBoundingClientRect();
      const scrollTop = window.scrollY;
      const offsetTop = rect.top + scrollTop;
      
      // Perform smooth scroll
      window.scrollTo({
        top: offsetTop - 80, // Account for header height
        behavior: 'smooth'
      });
    }
    
    // Manually trigger a scroll event to update the header navigation
    // First immediately to update the indicator
    window.dispatchEvent(new Event('scroll'));
    
    // Then again after scrolling completes to finalize
    setTimeout(() => {
      window.dispatchEvent(new Event('scroll'));
    }, 500);
  };

  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-content">
          <div className="footer-logo">
            <img src="/images/v-bold.svg" alt="Vocab Logo" />
            <span>Vocab</span>
          </div>
          <div className="footer-links">
            <div className="footer-column">
              <h4>App</h4>
              <a href="#features" className="footer-nav-link" onClick={(e) => scrollToSection(e, 'features')}>Features</a>
              <a href="#how-it-works" className="footer-nav-link" onClick={(e) => scrollToSection(e, 'how-it-works')}>How It Works</a>
              <a href="#download" className="footer-nav-link" onClick={(e) => scrollToSection(e, 'download')}>Download</a>
            </div>
            <div className="footer-column">
              <h4>Resources</h4>
              <a href="#" className="footer-nav-link">Documentation</a>
              <a href="#" className="footer-nav-link">Contributing</a>
              <a href="#" className="footer-nav-link">Releases</a>
            </div>
            <div className="footer-column">
              <h4>Connect</h4>
              <a href="https://github.com/eccys/vocab-boost" target="_blank" rel="noopener noreferrer" className="footer-nav-link">GitHub</a>
              <a href="#" className="footer-nav-link">Twitter</a>
              <a href="mailto:help.vocabboost@gmail.com" className="footer-nav-link">Contact Us</a>
            </div>
          </div>
        </div>
        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} Vocab App. All rights reserved.</p>
          <p>Made with ❤️ by <a href="https://ecys.xyz" target="_blank" rel="noopener noreferrer">Ecys</a></p>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 