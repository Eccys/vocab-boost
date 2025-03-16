import { useState, useEffect } from 'react';

interface UseActiveSectionProps {
  sectionIds: string[];
  rootMargin?: string;
  threshold?: number;
}

/**
 * A custom hook that uses the Intersection Observer API to track which section is currently visible
 * in the viewport, based on provided section IDs.
 */
export function useActiveSection({
  sectionIds,
  rootMargin = '-20% 0px -30% 0px', // Default margins favoring the center of the viewport
  threshold = 0.1, // Default threshold for considering an element as "visible"
}: UseActiveSectionProps): string | null {
  const [activeSection, setActiveSection] = useState<string | null>(null);

  useEffect(() => {
    // No need to continue if there are no sections to observe
    if (!sectionIds.length) return;

    // Create an intersection observer to watch for sections entering the viewport
    const observer = new IntersectionObserver(
      (entries) => {
        // Get entries that are currently intersecting
        const visibleEntries = entries.filter((entry) => entry.isIntersecting);

        // If we have visible entries, update the active section to the first one
        if (visibleEntries.length > 0) {
          // We typically want the topmost visible section to be the active one
          const topSection = visibleEntries.reduce((top, entry) => {
            const entryTop = entry.boundingClientRect.top;
            const topEntryTop = top.boundingClientRect.top;
            return entryTop < topEntryTop ? entry : top;
          }, visibleEntries[0]);

          setActiveSection(topSection.target.id);
        }
      },
      { rootMargin, threshold }
    );

    // Observe each section
    sectionIds.forEach((id) => {
      const element = document.getElementById(id);
      if (element) {
        observer.observe(element);
      }
    });

    // Clean up the observer when the component unmounts or sectionIds change
    return () => {
      observer.disconnect();
    };
  }, [sectionIds, rootMargin, threshold]);

  return activeSection;
}

/**
 * Utility function to smoothly scroll to a specific section.
 */
export function scrollToSection(sectionId: string): void {
  const section = document.getElementById(sectionId);
  if (section) {
    // Scroll to section with smooth behavior
    section.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
} 