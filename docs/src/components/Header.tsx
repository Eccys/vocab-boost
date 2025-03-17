import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import '../styles/Header.css';
import { Home, Download, Menu, X, BookOpenText } from 'lucide-react';

// Define the navigation items structure
interface NavItem {
  name: string;
  url: string;
  icon: React.ReactNode;
  sectionId: string;
}

// Define section info interface
interface SectionInfo {
  name: string | null;
  sectionId: string;
  top: number;
  bottom: number;
  height: number;
}

const Header: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<string>('Home');
  const [isAnimating, setIsAnimating] = useState(false);
  const [isNavigating, setIsNavigating] = useState(false); // Track manual navigation
  const lastActiveTabRef = useRef<string>('Home'); // Track last active tab
  const menuRef = useRef<HTMLDivElement>(null);
  const toggleButtonRef = useRef<HTMLButtonElement>(null);
  const navigationTimerRef = useRef<NodeJS.Timeout | null>(null);
  
  // Define navigation items
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
      sectionId: 'features'
    },
    {
      name: 'Download',
      url: '/#download',
      icon: <Download size={18} strokeWidth={2.5} />,
      sectionId: 'download'
    }
  ];

  // Clean up navigation timer on unmount
  useEffect(() => {
    return () => {
      if (navigationTimerRef.current) {
        clearTimeout(navigationTimerRef.current);
      }
    };
  }, []);

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      // Recalculate section positions after resize
      if (location.pathname === '/' || location.pathname === '') {
        updateActiveSection();
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [location.pathname]);

  // Set active tab based on URL and scroll position
  useEffect(() => {
    // Skip during manual navigation
    if (isNavigating) return;
    
    // Check if we're on word-of-day page - don't highlight anything
    if (location.pathname === '/word-of-day') {
      setActiveTab('');
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
          lastActiveTabRef.current = matchingItem.name; // Update last active when explicitly navigating
          return;
        }
        // If hash doesn't match a nav item (e.g., how-it-works), maintain last active
      }
      
      // Check scroll position to set active tab
      updateActiveSection();
    }
  }, [location.pathname, location.hash, isNavigating]);

  // Updated function to determine active section based on scroll position
  const updateActiveSection = () => {
    // Skip updates during animation or manual navigation
    if (isAnimating || isNavigating) return;
    
    // Only run on home page
    if (location.pathname !== '/' && location.pathname !== '') return;
    
    const scrollPosition = window.scrollY;
    
    // Get all sections and their positions (including how-it-works)
    const allSectionIds = [...navItems.map(item => item.sectionId), 'how-it-works'];
    const sectionsInfo: SectionInfo[] = allSectionIds
      .map(sectionId => {
        const element = document.getElementById(sectionId);
        if (!element) return null;
        
        const rect = element.getBoundingClientRect();
        // Find corresponding nav item name (or null for how-it-works)
        const navItem = navItems.find(item => item.sectionId === sectionId);
        
        return {
          name: navItem ? navItem.name : null,
          sectionId,
          top: rect.top + window.scrollY,
          bottom: rect.bottom + window.scrollY,
          height: rect.height
        };
      })
      .filter((section): section is SectionInfo => section !== null);
    
    if (sectionsInfo.length === 0) return;
    
    // If at the very top of the page, select Home
    if (scrollPosition < sectionsInfo[0].top - 150) {
      setActiveTab('Home');
      lastActiveTabRef.current = 'Home';
      return;
    }
    
    // Find which section the user is currently viewing
    let currentSectionId: string | null = null;
    let maxVisibility = 0;
    
    for (const section of sectionsInfo) {
      const sectionTop = section.top;
      const sectionBottom = section.bottom;
      
      // Check if section is in view
      const viewportTop = scrollPosition;
      const viewportBottom = viewportTop + window.innerHeight;
      
      // Calculate how much of the section is visible
      const visibleTop = Math.max(sectionTop, viewportTop);
      const visibleBottom = Math.min(sectionBottom, viewportBottom);
      
      // If section is in view
      if (visibleBottom > visibleTop) {
        // Special case: if we're near the top of a section, prioritize it
        if (scrollPosition >= sectionTop - 150 && scrollPosition < sectionTop + 150) {
          currentSectionId = section.sectionId;
          break;
        }
        
        const visibleHeight = visibleBottom - visibleTop;
        if (visibleHeight > maxVisibility) {
          maxVisibility = visibleHeight;
          currentSectionId = section.sectionId;
        }
      }
    }
    
    // If we're viewing a section with a nav item, update active tab
    if (currentSectionId) {
      const section = sectionsInfo.find(s => s.sectionId === currentSectionId);
      
      if (section) {
        if (section.name) {
          // This is a section with a navbar item - update active tab and last active
          setActiveTab(section.name);
          lastActiveTabRef.current = section.name;
        } else {
          // This is a section without a navbar item (like how-it-works)
          // Keep the last active tab highlighted
          setActiveTab(lastActiveTabRef.current);
        }
      }
    }
  };

  // Handle scroll event
  useEffect(() => {
    const handleScroll = () => {
      // Update header background
      setIsScrolled(window.scrollY > 50);
      
      // Update active section on home page (but skip during animation or navigation)
      if (!isAnimating && !isNavigating && (location.pathname === '/' || location.pathname === '')) {
        updateActiveSection();
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    
    // Initial call to set correct values
    handleScroll();
    
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [location.pathname, isAnimating, isNavigating]);
  
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
  
  // Handle animation start and end
  const handleAnimationStart = () => {
    setIsAnimating(true);
  };
  
  const handleAnimationComplete = () => {
    setIsAnimating(false);
  };
  
  // Lock navigation for a period to prevent scroll events from interfering
  const lockNavigation = () => {
    setIsNavigating(true);
    
    // Clear any existing timer
    if (navigationTimerRef.current) {
      clearTimeout(navigationTimerRef.current);
    }
    
    // Set timer to unlock navigation after animation and scrolling complete
    navigationTimerRef.current = setTimeout(() => {
      setIsNavigating(false);
    }, 1250); // 1.25 seconds (animation duration + scroll time)
  };
  
  // Handle navigation state for cross-page navigation
  useEffect(() => {
    // This handles the case when we navigate from another page (like Word of Day)
    if (location.state && location.state.scrollTo) {
      const sectionId = location.state.scrollTo;
      
      // Need to wait for the page to fully render before scrolling
      setTimeout(() => {
        // Lock navigation to prevent scroll events from changing the active tab
        lockNavigation();
        
        // Find the matching nav item and update active tab
        const matchingItem = navItems.find(item => item.sectionId === sectionId);
        if (matchingItem) {
          setActiveTab(matchingItem.name);
          lastActiveTabRef.current = matchingItem.name;
        }
        
        // Scroll to the section
        const section = document.getElementById(sectionId);
        if (section) {
          const offsetTop = section.getBoundingClientRect().top + window.scrollY;
          window.scrollTo({
            top: offsetTop - 80,
            behavior: 'smooth'
          });
          
          // Update URL hash
          if (history.pushState) {
            history.pushState(null, '', `#${sectionId}`);
          } else {
            window.location.hash = sectionId;
          }
        }
        
        // Clear the navigation state to prevent scrolling on refresh
        navigate('/', { replace: true, state: {} });
      }, 100);
    }
  }, [location.state, navigate]);
  
  // Smooth scroll to section - updated to handle cross-page navigation
  const scrollToSection = (e: React.MouseEvent, sectionId: string) => {
    e.preventDefault();
    closeMobileMenu();
    
    // Check if we're on the homepage or not
    const isHomePage = location.pathname === '/' || location.pathname === '';
    
    if (isHomePage) {
      // If we're already on the homepage, just scroll to the section
      // Find the matching nav item and update active tab
      const matchingItem = navItems.find(item => item.sectionId === sectionId);
      if (matchingItem) {
        // Lock navigation to prevent scroll events from changing the active tab
        lockNavigation();
        
        // Update active tab (animation will happen independently)
        setActiveTab(matchingItem.name);
        lastActiveTabRef.current = matchingItem.name;
      }
      
      // Handle scrolling immediately (don't wait for animation)
      const section = document.getElementById(sectionId);
      if (section) {
        const offsetTop = section.getBoundingClientRect().top + window.scrollY;
        window.scrollTo({
          top: offsetTop - 80,
          behavior: 'smooth'
        });
      }
      
      // Update URL hash immediately
      if (history.pushState) {
        history.pushState(null, '', `#${sectionId}`);
      } else {
        window.location.hash = sectionId;
      }
    } else {
      // We're on another page, navigate to homepage with state
      navigate('/', { state: { scrollTo: sectionId } });
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
              
              return (
                <a 
                  key={item.name}
                  href={`/#${item.sectionId}`}
                  className={`nav-item ${isActive ? 'active' : ''}`}
                  onClick={(e) => scrollToSection(e, item.sectionId)}
                >
                  <span className="nav-icon">{item.icon}</span>
                  <span className="nav-text">{item.name}</span>
                  
                  {/* Create a shared layout element present in all items to prevent jumps */}
                  <div className="nav-indicator-container"></div>
                  
                  {isActive && (
                    <motion.div
                      layoutId="navbar-active-indicator"
                      className="nav-indicator"
                      layout="position"
                      initial={false}
                      onAnimationStart={handleAnimationStart}
                      onAnimationComplete={handleAnimationComplete}
                      transition={{
                        type: "tween",
                        duration: 0.25,
                        ease: "easeInOut",
                      }}
                    >
                      <div className="nav-glow"></div>
                    </motion.div>
                  )}
                </a>
              );
            })}
          </div>
        </div>
        
        <div className="header-right">
          <Link 
            to="/word-of-day" 
            className="header-cta-btn"
            onClick={() => {
              // Ensure we scroll to top when navigating to Word of Day page
              window.scrollTo(0, 0);
            }}
          >
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