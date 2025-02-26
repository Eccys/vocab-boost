# Vocabulary Words Management

This file explains how to add, modify, or remove vocabulary words in the app.

## Word Storage

The app stores vocabulary words in a JSON file located at:
`app/src/main/assets/words.json`

## JSON Format

Each word in the JSON file follows this structure:

```json
{
  "word": "example",
  "definition": "a representative form or pattern",
  "synonym1": "sample",
  "synonym1Definition": "a small part of something intended as representative of the whole",
  "synonym1ExampleSentence": "The scientist analyzed a sample of the material.",
  "synonym2": "specimen",
  "synonym2Definition": "an individual instance that represents a class",
  "synonym2ExampleSentence": "This butterfly is a fine specimen of the species.",
  "synonym3": "illustration",
  "synonym3Definition": "an example that clarifies or explains",
  "synonym3ExampleSentence": "The teacher used a simple illustration to explain the concept."
}
```

## How to Add Words

1. Open the `words.json` file in a text editor
2. Add new word entries following the format above
3. Make sure to maintain valid JSON syntax:
   - Each word entry should be separated by a comma
   - The last word entry should NOT have a trailing comma
   - All strings must be enclosed in double quotes
   - All fields are required

## Example

Here's an example of adding a new word to the JSON file:

```json
[
  {
    "word": "existing_word",
    "definition": "...",
    "synonym1": "...",
    "synonym1Definition": "...",
    "synonym1ExampleSentence": "...",
    "synonym2": "...",
    "synonym2Definition": "...",
    "synonym2ExampleSentence": "...",
    "synonym3": "...",
    "synonym3Definition": "...",
    "synonym3ExampleSentence": "..."
  },
  {
    "word": "new_word",
    "definition": "...",
    "synonym1": "...",
    "synonym1Definition": "...",
    "synonym1ExampleSentence": "...",
    "synonym2": "...",
    "synonym2Definition": "...",
    "synonym2ExampleSentence": "...",
    "synonym3": "...",
    "synonym3Definition": "...",
    "synonym3ExampleSentence": "..."
  }
]
```

## Tips for Managing Large Word Lists

1. Use a JSON validator to check your file for syntax errors
2. Consider using a spreadsheet to manage your words, then export to JSON
3. Split very large lists into multiple JSON files if needed
4. Back up your word list regularly

## Loading Words into the App

The app automatically loads words from the JSON file when it initializes the database. If you want to manually trigger loading:

1. The `WordRepository.insertInitialWords()` method replaces all existing words with those from the JSON file
2. The `WordRepository.addWordsFromJson()` method adds words from the JSON file without deleting existing ones

## Troubleshooting

If your words aren't appearing in the app:
1. Check for JSON syntax errors
2. Ensure all required fields are present
3. Restart the app to trigger database initialization
4. Check the app logs for any error messages 