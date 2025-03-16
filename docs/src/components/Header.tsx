import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import '../styles/Header.css';

const Header: React.FC = () => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);

  // Handle scroll event
  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 50) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll);
    
    // Cleanup event listener
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);
  
  // Handle click outside to close mobile menu
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        isMobileMenuOpen && 
        menuRef.current && 
        !menuRef.current.contains(event.target as Node) &&
        toggleButtonRef.current &&
        !toggleButtonRef.current.contains(event.target as Node)
      ) {
        setIsMobileMenuOpen(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isMobileMenuOpen]);

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };
  
  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };
  
  // Smooth scroll to download section
  const scrollToDownload = (e: React.MouseEvent) => {
    e.preventDefault();
    const downloadSection = document.getElementById('download');
    if (downloadSection) {
      window.scrollTo({
        top: downloadSection.offsetTop - 80, // Account for header height
        behavior: 'smooth'
      });
    }
    closeMobileMenu();
  };

  return (
    <header className={`header ${isScrolled ? 'scrolled' : ''}`}>
      <nav className="container nav-container">
        <Link to="/" className="logo">
          <img src="/images/v-bold.svg" alt="Vocab Logo" />
          <span>Vocab</span>
        </Link>
        
        <div className={`nav-links ${isMobileMenuOpen ? 'mobile-open' : ''}`} ref={menuRef}>
          <a href="/#features" onClick={closeMobileMenu}>Features</a>
          <a href="/#how-it-works" onClick={closeMobileMenu}>How it Works</a>
          <Link to="/word-of-day" onClick={closeMobileMenu}>Today's Word</Link>
          <a href="/#download" onClick={closeMobileMenu}>Download</a>
          <a href="https://github.com/eccys/vocab-boost" className="github-link" target="_blank" rel="noopener noreferrer" onClick={closeMobileMenu}>GitHub</a>
        </div>
        
        <a href="#download" className="header-cta-btn" onClick={scrollToDownload}>
          Get the App
        </a>
        
        <button 
          className="mobile-menu-toggle" 
          onClick={toggleMobileMenu}
          ref={toggleButtonRef}
        >
          <span className={`hamburger ${isMobileMenuOpen ? 'open' : ''}`}></span>
        </button>
      </nav>
    </header>
  );
};

export default Header; 