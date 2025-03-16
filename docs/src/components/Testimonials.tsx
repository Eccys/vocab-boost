import React, { useRef, useEffect } from 'react';
import '../styles/Testimonials.css';

const Testimonials: React.FC = () => {
  const testimonials = [
    {
      quote: "Vocab has transformed how I learn new words. The daily word feature and quizzes make it fun and engaging!",
      author: "Sarah M., College Student",
      rating: 5
    },
    {
      quote: "As someone preparing for standardized tests, this app has been invaluable. My vocabulary has improved dramatically.",
      author: "James T., Graduate Student",
      rating: 5
    },
    {
      quote: "I love that it's open source and respects my privacy while helping me learn. The machine learning system really works!",
      author: "Elena K., Software Developer",
      rating: 5
    },
    {
      quote: "The interface is beautiful and intuitive. I've tried many vocabulary apps, but this one keeps me coming back.",
      author: "Michael R., English Teacher",
      rating: 5
    },
    {
      quote: "I've been using Vocab for three months and have already added over 200 words to my vocabulary. Highly recommended!",
      author: "David L., Business Professional",
      rating: 5
    }
  ];

  const sliderRef = useRef<HTMLDivElement>(null);
  
  useEffect(() => {
    const slider = sliderRef.current;
    if (!slider) return;
    
    let isDown = false;
    let startX: number;
    let scrollLeft: number;
    
    const handleMouseDown = (e: MouseEvent) => {
      isDown = true;
      slider.classList.add('active');
      startX = e.pageX - slider.offsetLeft;
      scrollLeft = slider.scrollLeft;
    };
    
    const handleMouseLeave = () => {
      isDown = false;
      slider.classList.remove('active');
    };
    
    const handleMouseUp = () => {
      isDown = false;
      slider.classList.remove('active');
    };
    
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDown) return;
      e.preventDefault();
      const x = e.pageX - slider.offsetLeft;
      const walk = (x - startX) * 2;
      slider.scrollLeft = scrollLeft - walk;
    };
    
    slider.addEventListener('mousedown', handleMouseDown);
    slider.addEventListener('mouseleave', handleMouseLeave);
    slider.addEventListener('mouseup', handleMouseUp);
    slider.addEventListener('mousemove', handleMouseMove);
    
    return () => {
      slider.removeEventListener('mousedown', handleMouseDown);
      slider.removeEventListener('mouseleave', handleMouseLeave);
      slider.removeEventListener('mouseup', handleMouseUp);
      slider.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return (
    <section className="testimonials">
      <div className="container">
        <h2 className="section-title" data-aos="fade-up">What Our Users Say</h2>
        <div className="testimonial-slider" ref={sliderRef} data-aos="fade-up" data-aos-delay="100">
          {testimonials.map((testimonial, index) => (
            <div key={index} className="testimonial">
              <div className="quote">{testimonial.quote}</div>
              <div className="author">{testimonial.author}</div>
              <div className="rating">
                {Array(testimonial.rating).fill('★').join('')}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Testimonials; 