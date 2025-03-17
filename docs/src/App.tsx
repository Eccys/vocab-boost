import React from 'react';
import { Routes, Route } from 'react-router-dom';
import AOS from 'aos';

// Pages
import HomePage from './pages/HomePage';
import WordOfDayPage from './pages/WordOfDayPage';

// Import AOS script for animations
import 'aos/dist/aos.css';

// Initialize AOS animation library outside of the component
AOS.init({
  duration: 800,
  easing: 'ease-in-out',
  once: false,
  mirror: true
});

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/word-of-day" element={<WordOfDayPage />} />
    </Routes>
  );
};

export default App; 