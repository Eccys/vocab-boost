import React from 'react';
import { Link } from 'react-router-dom';
import '../styles/Footer.css';

const Footer: React.FC = () => {
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
              <a href="#features">Features</a>
              <a href="#how-it-works">How It Works</a>
              <a href="#download">Download</a>
            </div>
            <div className="footer-column">
              <h4>Resources</h4>
              <a href="#">Documentation</a>
              <a href="#">Contributing</a>
              <a href="#">Releases</a>
            </div>
            <div className="footer-column">
              <h4>Connect</h4>
              <a href="https://github.com/eccys/vocab-boost" target="_blank" rel="noopener noreferrer">GitHub</a>
              <a href="#">Twitter</a>
              <a href="mailto:help.vocabboost@gmail.com">Contact Us</a>
            </div>
          </div>
        </div>
        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} Vocab App. All rights reserved.</p>
          <p>Made with ❤️ by <a href="https://ecys.xyz" target="_blank" rel="noopener noreferrer">Ecys</a></p>
          <p><small>This is a demo landing page for illustrative purposes only.</small></p>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 