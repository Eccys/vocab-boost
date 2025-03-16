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
  sectionId?: string;
}

const Header: React.FC = () => {
  const location = useLocation();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);
  const [activeTab, setActiveTab] = useState<string | null>('Home');
  const menuRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);

  // Define navigation items with sectionId for scroll detection
  const navItems: NavItem[] = [
    {
      name: 'Home',
      url: '/#hero',
      icon: <Home size={18} strokeWidth={2.5} />,
      sectionId: 'hero'
    },
    {
      name: 'Features',
      url: '/#features',
      icon: <BookOpenText size={18} strokeWidth={2.5} />,
      onClick: (e) => scrollToSection(e, 'features'),
      sectionId: 'features'
    },
    {
      name: 'Download',
      url: '/#download',
      icon: <Download size={18} strokeWidth={2.5} />,
      onClick: (e) => scrollToSection(e, 'download'),
      sectionId: 'download'
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
    // Check if we're on word-of-day page - don't highlight anything
    if (location.pathname === '/word-of-day') {
      setActiveTab(null);
      return;
    }
    
    // Only run this for the home page
    if (location.pathname === '/' || location.pathname === '') {
      // If there's a hash in the URL, use that
      if (location.hash) {
        const hash = location.hash.substring(1);
        const matchingItem = navItems.find(item => item.sectionId === hash);
        if (matchingItem) {
          setActiveTab(matchingItem.name);
          return;
        }
      }
      
      // Default to Home if no hash or no matching hash
      setActiveTab('Home');
    }
  }, [location.pathname, location.hash, navItems]);

  // Handle scroll event for header background and section detection
  useEffect(() => {
    const handleScroll = () => {
      // Update background opacity based on scroll position
      if (window.scrollY > 50) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
      
      // Don't update active tab on word-of-day page
      if (location.pathname === '/word-of-day') {
        return;
      }
      
      // Only update active tab on homepage
      if (location.pathname === '/' || location.pathname === '') {
        // Detect which section is currently in view
        const scrollPosition = window.scrollY + window.innerHeight / 3;
        
        // Get all sections that correspond to nav items
        const sections = navItems
          .filter(item => item.sectionId)
          .map(item => {
            const element = document.getElementById(item.sectionId!);
            return {
              name: item.name,
              top: element?.offsetTop || 0,
              bottom: (element?.offsetTop || 0) + (element?.offsetHeight || 0)
            };
          })
          .sort((a, b) => a.top - b.top);
        
        // Find the active section based on scroll position
        let activeSection: string | null = null;
        for (const section of sections) {
          if (scrollPosition >= section.top && scrollPosition <= section.bottom) {
            activeSection = section.name;
            break;
          }
        }
        
        // If we scrolled past the last section, activate the last one
        if (!activeSection && scrollPosition > sections[sections.length - 1]?.bottom) {
          activeSection = sections[sections.length - 1]?.name || null;
        }
        
        // If we're at the top of the page or found no section, set to Home
        if (!activeSection && scrollPosition < sections[0]?.top) {
          activeSection = 'Home';
        }
        
        // Update active tab if it changed
        if (activeSection !== activeTab) {
          setActiveTab(activeSection);
        }
      }
    };

    window.addEventListener('scroll', handleScroll);
    
    // Initial call to set correct active tab on page load
    handleScroll();
    
    // Cleanup event listener
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [location.pathname, activeTab, navItems]);
  
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
      
      // Set the active tab immediately for better UX
      const matchingItem = navItems.find(item => item.sectionId === sectionId);
      if (matchingItem) {
        setActiveTab(matchingItem.name);
      }
    }
    closeMobileMenu();
    
    // Update URL hash without full page reload
    if (history.pushState) {
      history.pushState(null, '', `#${sectionId}`);
    } else {
      window.location.hash = sectionId;
    }
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