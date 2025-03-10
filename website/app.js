// Initialize AOS (Animate On Scroll)
document.addEventListener('DOMContentLoaded', function() {
    // Initialize AOS
    AOS.init({
        duration: 800,
        easing: 'ease-in-out',
        once: false,
        mirror: true
    });
    
    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;
            
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                window.scrollTo({
                    top: targetElement.offsetTop - 80,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // Scroll-based animations
    const heroImage = document.querySelector('.phone-mockup');
    const wordCard = document.querySelector('.word-card');
    const shapes = document.querySelectorAll('.shape');
    
    // Random word data for demo
    const words = [
        { 
            word: 'Ephemeral', 
            type: 'adjective', 
            definition: 'Lasting for a very short time', 
            example: 'The beauty of cherry blossoms is ephemeral, lasting only a few days.'
        },
        { 
            word: 'Serendipity', 
            type: 'noun', 
            definition: 'The occurrence of events by chance in a happy or beneficial way', 
            example: 'Finding this app was a serendipity that improved my vocabulary.'
        },
        { 
            word: 'Eloquent', 
            type: 'adjective', 
            definition: 'Fluent or persuasive in speaking or writing', 
            example: 'Her eloquent speech captivated the entire audience.'
        },
        { 
            word: 'Panacea', 
            type: 'noun', 
            definition: 'A solution or remedy for all difficulties or diseases', 
            example: 'Exercise is not a panacea for all health problems, but it certainly helps.'
        },
        { 
            word: 'Ubiquitous', 
            type: 'adjective', 
            definition: 'Present, appearing, or found everywhere', 
            example: 'Smartphones have become ubiquitous in modern society.'
        }
    ];
    
    // Function to update the word card with a random word
    function updateWordCard() {
        if (!wordCard) return;
        
        const randomIndex = Math.floor(Math.random() * words.length);
        const wordData = words[randomIndex];
        
        const wordTitle = wordCard.querySelector('h2');
        const wordType = wordCard.querySelector('p:nth-of-type(1)');
        const definition = wordCard.querySelector('.definition');
        const example = wordCard.querySelector('.example p');
        
        // Apply fade out effect
        wordCard.style.opacity = '0';
        wordCard.style.transform = 'translateY(20px)';
        
        // Update content after fade out
        setTimeout(() => {
            wordTitle.textContent = wordData.word;
            wordType.innerHTML = `<em>${wordData.type}</em>`;
            definition.textContent = wordData.definition;
            example.textContent = `"${wordData.example}"`;
            
            // Apply fade in effect
            wordCard.style.opacity = '1';
            wordCard.style.transform = 'translateY(0)';
        }, 500);
    }
    
    // Change word every 5 seconds
    if (wordCard) {
        setInterval(updateWordCard, 5000);
    }
    
    // Parallax effect on scroll
    if (shapes.length > 0) {
        window.addEventListener('scroll', function() {
            const scrollPosition = window.scrollY;
            
            shapes.forEach((shape, index) => {
                const speed = index === 0 ? 0.05 : 0.03;
                shape.style.transform = `translateY(${scrollPosition * speed}px)`;
            });
            
            if (heroImage) {
                heroImage.style.transform = `rotateY(${-20 + scrollPosition * 0.01}deg) rotateX(${10 - scrollPosition * 0.01}deg)`;
            }
        });
    }
    
    // Testimonial slider touch support
    const testimonialSlider = document.querySelector('.testimonial-slider');
    if (testimonialSlider) {
        let isDown = false;
        let startX;
        let scrollLeft;
        
        testimonialSlider.addEventListener('mousedown', (e) => {
            isDown = true;
            testimonialSlider.classList.add('active');
            startX = e.pageX - testimonialSlider.offsetLeft;
            scrollLeft = testimonialSlider.scrollLeft;
        });
        
        testimonialSlider.addEventListener('mouseleave', () => {
            isDown = false;
            testimonialSlider.classList.remove('active');
        });
        
        testimonialSlider.addEventListener('mouseup', () => {
            isDown = false;
            testimonialSlider.classList.remove('active');
        });
        
        testimonialSlider.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - testimonialSlider.offsetLeft;
            const walk = (x - startX) * 2;
            testimonialSlider.scrollLeft = scrollLeft - walk;
        });
    }
    
    // Create floating elements on the hero section
    function createFloatingElements() {
        const hero = document.querySelector('.hero');
        if (!hero) return;
        
        const numElements = 10;
        const colors = ['#6a11cb', '#2575fc', '#ff9a3c', '#ffffff'];
        
        for (let i = 0; i < numElements; i++) {
            const size = Math.random() * 20 + 5;
            const element = document.createElement('div');
            
            element.classList.add('floating-element');
            element.style.width = `${size}px`;
            element.style.height = `${size}px`;
            element.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
            element.style.opacity = Math.random() * 0.5 + 0.1;
            element.style.borderRadius = '50%';
            element.style.position = 'absolute';
            element.style.top = `${Math.random() * 100}%`;
            element.style.left = `${Math.random() * 100}%`;
            element.style.animation = `float ${Math.random() * 10 + 5}s ease-in-out infinite`;
            element.style.animationDelay = `${Math.random() * 5}s`;
            element.style.zIndex = '1';
            element.style.filter = 'blur(1px)';
            
            hero.appendChild(element);
        }
    }
    
    createFloatingElements();
    
    // Download button action
    const downloadButtons = document.querySelectorAll('.primary-btn, .download-btn');
    downloadButtons.forEach(button => {
        button.addEventListener('click', function() {
            const downloadSection = document.querySelector('#download');
            if (downloadSection) {
                window.scrollTo({
                    top: downloadSection.offsetTop - 80,
                    behavior: 'smooth'
                });
            }
        });
    });
    
    // Add typing effect to the hero headline
    function typeEffect() {
        const heroHeadline = document.querySelector('.hero h1');
        if (!heroHeadline) return;
        
        const originalText = heroHeadline.innerHTML;
        
        // Skip if already processed
        if (heroHeadline.classList.contains('typed')) return;
        
        heroHeadline.classList.add('typed');
        
        // Create the typing effect container
        const typingContainer = document.createElement('div');
        typingContainer.classList.add('typing-effect');
        
        // Replace the hero headline with the typing container
        heroHeadline.innerHTML = '';
        heroHeadline.appendChild(typingContainer);
        
        // Set up typing animation
        let i = 0;
        const speed = 50; // typing speed
        
        function typeWriter() {
            if (i < originalText.length) {
                typingContainer.innerHTML += originalText.charAt(i);
                i++;
                setTimeout(typeWriter, speed);
            }
        }
        
        // Start typing animation after a short delay
        setTimeout(typeWriter, 500);
    }
    
    // Run typing effect
    typeEffect();
    
    // Add scroll indicator
    const hero = document.querySelector('.hero');
    if (hero) {
        const scrollIndicator = document.createElement('div');
        scrollIndicator.classList.add('scroll-indicator');
        scrollIndicator.innerHTML = `
            <div class="mouse">
                <div class="wheel"></div>
            </div>
            <div class="scroll-text">Scroll Down</div>
        `;
        
        // Add the scroll indicator to the hero section
        hero.appendChild(scrollIndicator);
        
        // Style the scroll indicator
        const style = document.createElement('style');
        style.textContent = `
            .scroll-indicator {
                position: absolute;
                bottom: 50px;
                left: 50%;
                transform: translateX(-50%);
                display: flex;
                flex-direction: column;
                align-items: center;
                animation: fadeIn 1s ease-out 2s forwards;
                opacity: 0;
                z-index: 10;
            }
            
            .mouse {
                width: 30px;
                height: 50px;
                border: 2px solid white;
                border-radius: 15px;
                position: relative;
            }
            
            .wheel {
                width: 4px;
                height: 10px;
                background-color: white;
                position: absolute;
                top: 10px;
                left: 50%;
                transform: translateX(-50%);
                border-radius: 2px;
                animation: scroll 2s ease infinite;
            }
            
            .scroll-text {
                color: white;
                font-size: 0.8rem;
                margin-top: 10px;
                font-weight: 500;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            
            @keyframes scroll {
                0% {
                    opacity: 1;
                    top: 10px;
                }
                100% {
                    opacity: 0;
                    top: 30px;
                }
            }
            
            @keyframes fadeIn {
                to {
                    opacity: 1;
                }
            }
        `;
        
        document.head.appendChild(style);
    }
}); 