import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import '../styles/Header.css';
import { Home, Download, Menu, X, BookOpenText } from 'lucide-react';

// Define the navigation items structure
interface NavItem {
  name: string;
  url: string;
  icon: React.ReactNode;
  onClick?: (e: React.MouseEvent) => void;
}

const Header: React.FC = () => {
  const location = useLocation();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);
  const [activeTab, setActiveTab] = useState('Home');
  const menuRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);

  // Define navigation items - removed Github and Today's Word
  const navItems: NavItem[] = [
    {
      name: 'Home',
      url: '/#hero',
      icon: <Home size={18} strokeWidth={2.5} />,
    },
    {
      name: 'Features',
      url: '/#features',
      icon: <BookOpenText size={18} strokeWidth={2.5} />,
      onClick: (e) => scrollToSection(e, 'features')
    },
    {
      name: 'Download',
      url: '/#download',
      icon: <Download size={18} strokeWidth={2.5} />,
      onClick: (e) => scrollToSection(e, 'download')
    }
  ];

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Set active tab based on URL
  useEffect(() => {
    // Check if we're on a specific page
    if (location.pathname === '/word-of-day') {
      setActiveTab('Home'); // Default to Home when on word-of-day page
    } else if (location.hash) {
      // Handle hash links on homepage
      const hash = location.hash.substring(1);
      const matchingItem = navItems.find(item => item.url.includes(hash));
      if (matchingItem) {
        setActiveTab(matchingItem.name);
      }
    } else if (location.pathname === '/' || location.pathname === '') {
      setActiveTab('Home');
    }
  }, [location, navItems]);

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
  
  // Smooth scroll to section
  const scrollToSection = (e: React.MouseEvent, sectionId: string) => {
    e.preventDefault();
    const section = document.getElementById(sectionId);
    if (section) {
      window.scrollTo({
        top: section.offsetTop - 80, // Account for header height
        behavior: 'smooth'
      });
    }
    closeMobileMenu();
  };

  return (
    <header className={`header ${isScrolled ? 'scrolled' : ''}`}>
      <div className="container nav-container">
        <Link to="/" className="logo">
          <img src="/images/v-bold.svg" alt="Vocab Logo" />
          <span>Vocab</span>
        </Link>
        
        <div className="modern-navbar-container">
          <div 
            className={`modern-navbar ${isMobileMenuOpen ? 'mobile-open' : ''}`}
            ref={menuRef}
          >
            {navItems.map((item) => {
              const isActive = activeTab === item.name;
              const isExternal = item.url.startsWith('http');
              
              // Custom click handler that combines item onClick and active state change
              const handleItemClick = (e: React.MouseEvent) => {
                setActiveTab(item.name);
                if (item.onClick) {
                  item.onClick(e);
                } else {
                  closeMobileMenu();
                }
              };
              
              // Render different elements for internal vs external links
              const LinkElement = isExternal ? 
                (props: any) => (
                  <a 
                    href={item.url} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    onClick={handleItemClick} 
                    {...props} 
                  />
                ) : 
                (props: any) => {
                  // For hash links on the home page
                  if (item.url.includes('#') && !item.url.startsWith('/')) {
                    return (
                      <a 
                        href={item.url} 
                        onClick={handleItemClick} 
                        {...props} 
                      />
                    );
                  }
                  // For regular pages
                  return (
                    <Link 
                      to={item.url} 
                      onClick={handleItemClick} 
                      {...props} 
                    />
                  );
                };

              return (
                <LinkElement
                  key={item.name}
                  className={`nav-item ${isActive ? 'active' : ''}`}
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span className="nav-text">{item.name}</span>
                  
                  {isActive && (
                    <motion.div
                      layoutId="navbar-active-indicator"
                      className="nav-indicator"
                      initial={false}
                      transition={{
                        type: "spring",
                        stiffness: 300,
                        damping: 30,
                      }}
                    >
                      <div className="nav-glow"></div>
                    </motion.div>
                  )}
                </LinkElement>
              );
            })}
          </div>
        </div>
        
        <div className="header-right">
          <Link to="/word-of-day" className="header-cta-btn">
            Today's Word
          </Link>
          
          <button 
            className="mobile-menu-toggle" 
            onClick={toggleMobileMenu}
            ref={toggleButtonRef}
          >
            {isMobileMenuOpen ? 
              <X size={24} strokeWidth={2} /> : 
              <Menu size={24} strokeWidth={2} />
            }
          </button>
        </div>
      </div>
    </header>
  );
};

export default Header; 