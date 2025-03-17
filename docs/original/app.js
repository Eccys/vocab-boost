// Initialize AOS (Animate On Scroll)
document.addEventListener('DOMContentLoaded', function() {
    // Initialize AOS
    AOS.init({
        duration: 800,
        easing: 'ease-in-out',
        once: false,
        mirror: true
    });
    
    // App Mockup Carousel functionality
    const navDots = document.querySelectorAll('.nav-dot');
    const screens = document.querySelectorAll('.screen');
    const prevArrow = document.querySelector('.prev-arrow');
    const nextArrow = document.querySelector('.next-arrow');
    let currentScreenIndex = 0;
    let userInteracted = false;
    
    // Function to update active screen
    function goToScreen(index) {
        // Ensure index is within bounds
        if (index < 0) index = screens.length - 1;
        if (index >= screens.length) index = 0;
        
        currentScreenIndex = index;
        
        // Remove active class from all screens and dots
        screens.forEach(screen => screen.classList.remove('active'));
        navDots.forEach(dot => dot.classList.remove('active'));
        
        // Add active class to current screen and dot
        screens[currentScreenIndex].classList.add('active');
        navDots[currentScreenIndex].classList.add('active');
        
        // Update arrows state (optional: disable at ends if not circular)
        /*
        prevArrow.disabled = currentScreenIndex === 0;
        nextArrow.disabled = currentScreenIndex === screens.length - 1;
        */
    }
    
    // Add click event to each navigation dot
    navDots.forEach((dot, index) => {
        dot.addEventListener('click', () => {
            goToScreen(index);
            // Reset the timer when manually navigating
            clearInterval(screenRotationInterval);
            // Mark that user has interacted
            userInteracted = true;
        });
    });
    
    // Add click event to arrow buttons
    if (prevArrow) {
        prevArrow.addEventListener('click', () => {
            goToScreen(currentScreenIndex - 1);
            // Reset the timer when manually navigating
            clearInterval(screenRotationInterval);
            // Mark that user has interacted
            userInteracted = true;
        });
    }
    
    if (nextArrow) {
        nextArrow.addEventListener('click', () => {
            goToScreen(currentScreenIndex + 1);
            // Reset the timer when manually navigating
            clearInterval(screenRotationInterval);
            // Mark that user has interacted
            userInteracted = true;
        });
    }
    
    // Auto-rotate screens every 5 seconds
    function rotateScreens() {
        goToScreen(currentScreenIndex + 1);
    }
    
    // Set interval for rotating screens, but clear it if user interacts with the carousel
    let screenRotationInterval = setInterval(rotateScreens, 5000);
    
    const appCarousel = document.querySelector('.app-screens-carousel');
    if (appCarousel) {
        appCarousel.addEventListener('mouseenter', () => {
            // Clear the rotation interval when mouse enters
            clearInterval(screenRotationInterval);
        });
        
        appCarousel.addEventListener('mouseleave', () => {
            // Only restart rotation if user hasn't interacted with carousel
            if (!userInteracted) {
                screenRotationInterval = setInterval(rotateScreens, 5000);
            }
        });
        
        // Enable keyboard navigation for the carousel when it's hovered
        appCarousel.addEventListener('mouseover', () => {
            document.addEventListener('keydown', handleKeyNavigation);
        });
        
        appCarousel.addEventListener('mouseout', () => {
            document.removeEventListener('keydown', handleKeyNavigation);
        });
        
        // Add touch swipe support for mobile devices
        let touchStartX = 0;
        let touchEndX = 0;
        
        // Set up touch event handlers if appCarousel exists
        appCarousel.addEventListener('touchstart', (e) => {
            touchStartX = e.changedTouches[0].screenX;
        }, { passive: true });
        
        appCarousel.addEventListener('touchend', (e) => {
            touchEndX = e.changedTouches[0].screenX;
            handleSwipe();
        }, { passive: true });
    }
    
    // Handle the swipe gesture
    function handleSwipe() {
        // Minimum distance required for a swipe - adjust as needed
        const minSwipeDistance = 50;
        const swipeDistance = touchEndX - touchStartX;
        
        if (Math.abs(swipeDistance) >= minSwipeDistance) {
            if (swipeDistance > 0) {
                // Swiped right - go to previous screen
                goToScreen(currentScreenIndex - 1);
            } else {
                // Swiped left - go to next screen
                goToScreen(currentScreenIndex + 1);
            }
            
            // Mark that user has interacted
            userInteracted = true;
            clearInterval(screenRotationInterval);
        }
    }
    
    // Handle keyboard navigation (left/right arrow keys)
    function handleKeyNavigation(e) {
        if (e.key === 'ArrowLeft') {
            goToScreen(currentScreenIndex - 1);
            userInteracted = true;
            clearInterval(screenRotationInterval);
        } else if (e.key === 'ArrowRight') {
            goToScreen(currentScreenIndex + 1);
            userInteracted = true;
            clearInterval(screenRotationInterval);
        }
    }
    
    // Back to Top button functionality
    const backToTop = document.querySelector('.back-to-top');
    
    window.addEventListener('scroll', function() {
        if (window.scrollY > 300) {
            backToTop.classList.add('visible');
        } else {
            backToTop.classList.remove('visible');
        }
    });
    
    backToTop.addEventListener('click', function(e) {
        e.preventDefault();
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
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
            word: 'Chiaroscuro', 
            type: 'noun',
            pronunciation: 'kee-ah-roh-SKYOOR-oh',
            definition: 'A visual effect created by strong contrasts between light and dark', 
            example: 'The artist used chiaroscuro to add drama to his painting.'
        },
        { 
            word: 'Ephemeral', 
            type: 'adjective',
            pronunciation: 'ih-FEM-er-uhl',
            definition: 'Lasting for a very short time', 
            example: 'The beauty of cherry blossoms is ephemeral, lasting only a few days.'
        },
        { 
            word: 'Serendipity', 
            type: 'noun',
            pronunciation: 'ser-uhn-DIP-ih-tee',
            definition: 'The occurrence of events by chance in a happy or beneficial way', 
            example: 'Finding this app was a serendipity that improved my vocabulary.'
        },
        { 
            word: 'Eloquent', 
            type: 'adjective',
            pronunciation: 'EL-uh-kwuhnt',
            definition: 'Fluent or persuasive in speaking or writing', 
            example: 'Her eloquent speech captivated the entire audience.'
        },
        { 
            word: 'Panacea', 
            type: 'noun',
            pronunciation: 'pan-uh-SEE-uh',
            definition: 'A solution or remedy for all difficulties or diseases', 
            example: 'Exercise is not a panacea for all health problems, but it certainly helps.'
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
        
        // Enhanced fade out effect
        wordCard.style.transition = 'all 0.6s cubic-bezier(0.68, -0.55, 0.27, 1.55)';
        wordCard.style.opacity = '0';
        wordCard.style.transform = 'translateY(30px) scale(0.95)';
        
        // Update content after fade out
        setTimeout(() => {
            wordTitle.textContent = wordData.word;
            wordType.innerHTML = `<em>${wordData.type}</em> | <span class="pronunciation">${wordData.pronunciation}</span>`;
            definition.textContent = wordData.definition;
            example.textContent = `"${wordData.example}"`;
            
            // Enhanced fade in effect with slight delay for better visual appeal
            setTimeout(() => {
                // Add a subtle shadow and glow effect during the fade-in
                wordCard.style.boxShadow = '0 15px 35px rgba(0, 0, 0, 0.4), 0 0 15px rgba(106, 17, 203, 0.2)';
                wordCard.style.opacity = '1';
                wordCard.style.transform = 'translateY(0) scale(1)';
                
                // Reset the shadow after the animation completes
                setTimeout(() => {
                    wordCard.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.3)';
                    wordCard.style.transition = 'all 0.3s ease'; // Reset to quicker transitions for hover effects
                }, 600);
            }, 100);
        }, 600);
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
        
        // We'll use a simple approach that separates the HTML tags from text content
        
        // First, identify the span tag and its content
        const spanMatch = originalText.match(/<span class="gradient-text">(.*?)<\/span>/);
        
        if (spanMatch) {
            const beforeSpan = originalText.split(spanMatch[0])[0];
            const spanContent = spanMatch[1]; // "One Word at a Time"
            
            // Typing speed
            const speed = 50;
            let currentText = '';
            let currentIndex = 0;
            let isTypingSpan = false;
            let spanElement = null;
            
            function typeNextChar() {
                // If we're still typing the text before the span
                if (!isTypingSpan && currentIndex < beforeSpan.length) {
                    typingContainer.textContent += beforeSpan.charAt(currentIndex);
                    currentIndex++;
                    setTimeout(typeNextChar, speed);
                }
                // Start typing the span content - first create the span
                else if (!isTypingSpan) {
                    isTypingSpan = true;
                    currentIndex = 0;
                    
                    // Add a line break before the span
                    const lineBreak = document.createElement('br');
                    typingContainer.appendChild(lineBreak);
                    
                    // Create the span element
                    spanElement = document.createElement('span');
                    spanElement.className = 'gradient-text';
                    typingContainer.appendChild(spanElement);
                    
                    // Add a slight delay before starting to type in the span
                    setTimeout(typeNextChar, speed);
                }
                // Type the span content character by character
                else if (currentIndex < spanContent.length) {
                    spanElement.textContent += spanContent.charAt(currentIndex);
                    currentIndex++;
                    setTimeout(typeNextChar, speed);
                }
            }
            
            // Start the typing animation
            typeNextChar();
        } else {
            // If there's no span, just type the text normally
            let i = 0;
            const speed = 50;
            
            function typeWriter() {
                if (i < originalText.length) {
                    typingContainer.innerHTML += originalText.charAt(i);
                    i++;
                    setTimeout(typeWriter, speed);
                }
            }
            
            setTimeout(typeWriter, 500);
        }
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