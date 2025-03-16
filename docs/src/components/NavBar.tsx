import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Home, Book, Download, Star, Menu, X } from 'lucide-react';
import '../styles/NavBar.css';
import { useActiveSection, scrollToSection } from '../hooks/useActiveSection';

// Define the navigation items
const navItems = [
  { name: 'Home', url: '/', icon: <Home size={20} />, sectionId: 'hero' },
  { name: 'Features', url: '/#features', icon: <Star size={20} />, sectionId: 'features' },
  { name: 'Learn', url: '/#learn', icon: <Book size={20} />, sectionId: 'learn' },
  { name: 'Downloads', url: '/#downloads', icon: <Download size={20} />, sectionId: 'downloads' },
];

const NavBar: React.FC = () => {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const navMenuRef = useRef<HTMLDivElement>(null);
  const indicatorRef = useRef<HTMLDivElement>(null);
  const mobileMenuRef = useRef<HTMLDivElement>(null);

  // Use our custom hook to track the active section
  const activeSection = useActiveSection({
    sectionIds: navItems.map(item => item.sectionId),
    rootMargin: '-20% 0px -30% 0px',
  });

  // Function to update indicator position based on active nav item
  const updateIndicator = (itemIndex: number) => {
    if (!navMenuRef.current || !indicatorRef.current) return;
    
    const navItems = navMenuRef.current.querySelectorAll('.nav-item');
    if (itemIndex >= 0 && itemIndex < navItems.length) {
      const activeItem = navItems[itemIndex] as HTMLElement;
      const activeLink = activeItem.querySelector('.nav-link') as HTMLElement;
      
      if (activeLink) {
        // Get positions relative to nav menu
        const activeLinkRect = activeLink.getBoundingClientRect();
        const navMenuRect = navMenuRef.current.getBoundingClientRect();
        
        // Calculate positions and apply to indicator
        const left = activeLinkRect.left - navMenuRect.left;
        const width = activeLinkRect.width;
        
        indicatorRef.current.style.left = `${left}px`;
        indicatorRef.current.style.width = `${width}px`;
      }
    }
  };

  // Toggle mobile menu
  const toggleMobileMenu = () => {
    setMobileMenuOpen(prev => !prev);
  };

  // Close mobile menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (mobileMenuRef.current && 
          !mobileMenuRef.current.contains(event.target as Node) && 
          !document.querySelector('.mobile-menu-toggle')?.contains(event.target as Node)) {
        setMobileMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 768 && mobileMenuOpen) {
        setMobileMenuOpen(false);
      }

      // Update indicator when resizing
      const activeItemIndex = navItems.findIndex(item => item.sectionId === activeSection);
      if (activeItemIndex !== -1) {
        updateIndicator(activeItemIndex);
      }
    };

    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [mobileMenuOpen, activeSection]);

  // Handle scroll event for header background change
  useEffect(() => {
    const handleScroll = () => {
      // Add scrolled class when scrolling down
      if (window.scrollY > 20) {
        setScrolled(true);
      } else {
        setScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  // Update indicator position when active section changes
  useEffect(() => {
    const activeItemIndex = navItems.findIndex(item => item.sectionId === activeSection);
    
    if (activeItemIndex !== -1) {
      updateIndicator(activeItemIndex);
    }
  }, [activeSection]);

  // Handle navigation link click
  const handleNavLinkClick = (e: React.MouseEvent, item: typeof navItems[0]) => {
    // If we're already on the homepage, scroll to the section
    if (location.pathname === '/' && item.sectionId) {
      e.preventDefault();
      scrollToSection(item.sectionId);
      setMobileMenuOpen(false);
    }
    // If we're on a different page, don't prevent default to allow navigation
  };

  return (
    <header className={`header ${scrolled ? 'scrolled' : ''}`}>
      <div className="header-content">
        <Link to="/" className="logo">
          <img src="/logo.png" alt="Vocab Logo" />
          <span>Vocab</span>
        </Link>

        <nav className="navbar">
          <div className="nav-menu" ref={navMenuRef}>
            <ul className="nav-items">
              {navItems.map((item, index) => (
                <li key={item.name} className="nav-item">
                  <Link
                    to={item.url}
                    className={`nav-link ${activeSection === item.sectionId ? 'active' : ''}`}
                    onClick={(e) => handleNavLinkClick(e, item)}
                  >
                    <span className="nav-icon">{item.icon}</span>
                    <span>{item.name}</span>
                  </Link>
                </li>
              ))}
            </ul>

            {/* Animated navigation indicator */}
            <motion.div 
              className="nav-indicator" 
              ref={indicatorRef}
              initial={{ width: 0 }}
              animate={{ width: 'auto' }}
              transition={{ duration: 0.3 }}
            >
              <div className="nav-glow"></div>
            </motion.div>
          </div>

          <Link to="/word-of-day" className="header-cta-btn">
            Today's Word
          </Link>
          
          <button 
            className="mobile-menu-toggle"
            onClick={toggleMobileMenu}
            aria-label="Toggle mobile menu"
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </nav>
      </div>

      {/* Mobile Menu */}
      <div 
        className={`mobile-menu ${mobileMenuOpen ? 'open' : ''}`}
        ref={mobileMenuRef}
      >
        <ul className="mobile-nav-items">
          {navItems.map((item) => (
            <li key={item.name} className="mobile-nav-item">
              <Link
                to={item.url}
                className={`mobile-nav-link ${activeSection === item.sectionId ? 'active' : ''}`}
                onClick={(e) => handleNavLinkClick(e, item)}
              >
                <span className="nav-icon">{item.icon}</span>
                <span>{item.name}</span>
              </Link>
            </li>
          ))}
        </ul>
        <div className="mobile-cta">
          <Link to="/word-of-day" className="header-cta-btn">
            Today's Word
          </Link>
        </div>
      </div>
    </header>
  );
};

export default NavBar; 