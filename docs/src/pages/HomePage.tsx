import React from 'react';
import Header from '../components/Header';
import Hero from '../components/Hero';
import Features from '../components/Features';
import HowItWorks from '../components/HowItWorks';
import Testimonials from '../components/Testimonials';
import Download from '../components/Download';
import Footer from '../components/Footer';
import BackToTop from '../components/BackToTop';

const HomePage: React.FC = () => {
  return (
    <>
      <Header />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Testimonials />
        <Download />
      </main>
      <Footer />
      <BackToTop />
    </>
  );
};

export default HomePage; 