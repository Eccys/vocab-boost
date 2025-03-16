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