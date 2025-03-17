import React from 'react';
import Header from '../components/Header';
import WordOfDay from '../components/WordOfDay';
import Footer from '../components/Footer';
import BackToTop from '../components/BackToTop';

const WordOfDayPage: React.FC = () => {
  return (
    <>
      <Header />
      <main>
        <WordOfDay />
      </main>
      <Footer />
      <BackToTop />
    </>
  );
};

export default WordOfDayPage; 