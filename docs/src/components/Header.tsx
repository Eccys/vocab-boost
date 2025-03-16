import React, { useState, useEffect, useRef, useCallback } from 'react';
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

// Debounce function to limit how often a function is called
function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  
  return function(...args: Parameters<T>) {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

const Header: React.FC = () => {
  const location = useLocation();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);
  const [activeTab, setActiveTab] = useState<string | null>('Home');
  const menuRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);
  const lastScrollPosRef = useRef(0);
  const scrollingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sectionsRef = useRef<Array<{name: string, top: number, bottom: number}>>([]);

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
      // Recalculate section positions after resize
      updateSectionPositions();
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

  // Update section positions
  const updateSectionPositions = useCallback(() => {
    if (location.pathname !== '/' && location.pathname !== '') return;
    
    const newSections = navItems
      .filter(item => item.sectionId)
      .map(item => {
        const element = document.getElementById(item.sectionId!);
        const rect = element?.getBoundingClientRect();
        const scrollTop = window.scrollY;
        return {
          name: item.name,
          top: (rect?.top || 0) + scrollTop - 100, // Buffer zone at top (accounting for header)
          bottom: (rect?.bottom || 0) + scrollTop
        };
      })
      .sort((a, b) => a.top - b.top);
    
    sectionsRef.current = newSections;
  }, [navItems, location.pathname]);

  // Determine which section is active based on scroll position
  const determineActiveSection = useCallback((scrollPosition: number): string | null => {
    if (location.pathname === '/word-of-day') return null;
    if (location.pathname !== '/' && location.pathname !== '') return null;
    
    const sections = sectionsRef.current;
    if (sections.length === 0) return 'Home';
    
    // If at the very top of the page, select Home
    if (scrollPosition < sections[0].top - 200) {
      return 'Home';
    }
    
    // Find section that contains current scroll position with a buffer
    const buffer = 50; // Pixels of buffer to prevent flickering
    const scrollDirection = scrollPosition > lastScrollPosRef.current ? 'down' : 'up';
    lastScrollPosRef.current = scrollPosition;
    
    for (let i = 0; i < sections.length; i++) {
      const section = sections[i];
      
      if (scrollDirection === 'down') {
        // When scrolling down, be more eager to switch to the next section
        if (scrollPosition >= section.top && scrollPosition <= section.bottom + buffer) {
          return section.name;
        }
      } else {
        // When scrolling up, be more resistant to switch to the previous section
        if (scrollPosition >= section.top - buffer && scrollPosition <= section.bottom) {
          return section.name;
        }
      }
    }
    
    // If we're past all sections, highlight the last one
    if (scrollPosition > sections[sections.length - 1].bottom) {
      return sections[sections.length - 1].name;
    }
    
    // Fallback to current active tab
    return activeTab;
  }, [location.pathname, activeTab]);

  // Handle scroll event with debounce for header background and section detection
  useEffect(() => {
    // Update section positions initially
    updateSectionPositions();
    
    const handleHeaderOpacity = () => {
      if (window.scrollY > 50) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };
    
    const handleActiveSection = debounce(() => {
      if (location.pathname === '/word-of-day') return;
      
      const scrollPosition = window.scrollY;
      const newActiveSection = determineActiveSection(scrollPosition);
      
      if (newActiveSection !== activeTab) {
        setActiveTab(newActiveSection);
      }
    }, 50); // 50ms debounce
    
    const handleScroll = () => {
      handleHeaderOpacity();
      
      // Skip section detection if we're actively scrolling to a section
      if (scrollingTimerRef.current) return;
      
      handleActiveSection();
    };

    window.addEventListener('scroll', handleScroll);
    
    // Initial call to set correct values
    handleHeaderOpacity();
    handleActiveSection();
    
    // Cleanup
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [determineActiveSection, updateSectionPositions, location.pathname, activeTab]);
  
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
      // Set the active tab immediately for better UX
      const matchingItem = navItems.find(item => item.sectionId === sectionId);
      if (matchingItem) {
        setActiveTab(matchingItem.name);
      }
      
      // Prevent section detection during programmatic scrolling
      if (scrollingTimerRef.current) clearTimeout(scrollingTimerRef.current);
      scrollingTimerRef.current = setTimeout(() => {
        scrollingTimerRef.current = null;
      }, 1000); // Lock section detection for 1 second during programmatic scrolling
      
      window.scrollTo({
        top: section.offsetTop - 80, // Account for header height
        behavior: 'smooth'
      });
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