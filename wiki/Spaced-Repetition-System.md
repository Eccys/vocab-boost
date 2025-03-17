# Spaced Repetition System

The core of Vocab's learning methodology is its advanced **Spaced Repetition System (SRS)**, a scientifically-proven technique designed to optimize memory retention and language acquisition.

## Basic Concept

Spaced repetition works on the principle of the **forgetting curve**, first described by German psychologist Hermann Ebbinghaus in the late 19th century. This principle demonstrates that:

- Memory retention declines over time
- The rate of decline can be slowed through strategic reviews
- Each successful review strengthens the memory and extends the retention period

## Vocab's Implementation

Vocab implements a sophisticated SRS algorithm that adapts to your individual learning patterns:

### Initial Exposure

When you first encounter a word in Vocab:

1. The word is presented with comprehensive context:
   - Definition
   - Etymology
   - Example sentences
   - Pronunciation guide
   - Part of speech
   - Synonyms and antonyms

2. You're prompted to complete an interactive exercise to reinforce initial understanding
   - Multiple-choice questions
   - Fill-in-the-blank sentences
   - Context matching

### Review Scheduling

The SRS algorithm then schedules future reviews based on your performance:

| Review Performance | Next Review Interval | Difficulty Adjustment |
|--------------------|----------------------|-----------------------|
| Perfect recall     | Interval × 2.5       | -1 Difficulty         |
| Good recall        | Interval × 2.0       | No change             |
| Hesitant recall    | Interval × 1.5       | +1 Difficulty         |
| Incorrect          | 1 day                | +2 Difficulty         |

The exact interval multipliers are dynamically adjusted based on:
- Your historical performance with similar words
- Time of day when you typically show optimal recall
- Word complexity and similarity to other words in your learning queue

### Neural Processing Engine

Vocab's neural processing engine goes beyond traditional SRS by incorporating semantic analysis:

```
Word Difficulty Factor = BaseComplexity * (1 + SemanticOverlapFactor)

Where:
- BaseComplexity is determined by word frequency, length, and morphological structure
- SemanticOverlapFactor accounts for similarity to other words you're learning to avoid confusion
```

The neural engine also implements **forgetting curve prediction** to anticipate when you're about to forget a word, scheduling reviews right before that point for maximum efficiency.

## Advanced Settings

Power users can customize the SRS behavior in Settings:

### Review Frequency Control

- **Aggressive**: Shorter intervals, more frequent reviews
- **Standard**: Balanced approach (default)
- **Relaxed**: Longer intervals, fewer reviews

### Learning Focus

- **Acquisition**: Prioritizes learning new words
- **Retention**: Prioritizes solidifying existing knowledge 
- **Balance**: Equal emphasis on new and review cards (default)

### Algorithm Sensitivity

- **Response Time Weighting**: How much your answer speed affects difficulty assessment
- **Error Penalty Factor**: How much incorrect answers impact scheduling
- **Leeches Threshold**: When to flag difficult words for special attention

## The Science Behind SRS

Vocab's SRS implementation is based on extensive research in cognitive science:

- **Retrieval practice effect**: The act of recalling information strengthens neural pathways
- **Spacing effect**: Distributed practice leads to better long-term retention than massed practice
- **Contextual variation**: Learning words in different contexts enhances retention and transfer
- **Interleaving effect**: Mixing different word types produces stronger memory traces

## Data-Driven Optimization

The system continually improves through:

- Analysis of aggregated, anonymized learning data from thousands of users
- A/B testing of algorithm variations
- Regular refinement of difficulty assessments and interval calculations

Our commitment to evidence-based learning ensures that each minute you spend with Vocab delivers maximum vocabulary growth.

---

For technical details on the algorithm implementation, developers can refer to the [Algorithm Implementation](https://github.com/Eccys/vocab-boost/blob/main/app/src/main/java/com/ecys/vocabboost/algorithm/SRSEngine.java) file in our GitHub repository. 