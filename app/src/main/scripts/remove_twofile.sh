#!/bin/bash

# Check if two arguments are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 file1.json file2.json"
    exit 1
fi

FILE1="$1"
FILE2="$2"
OUTPUT_FILE="output.json"

# Extract words from file2 and store them as a JSON array
jq -r 'map(.word) | unique' "$FILE2" > words_to_remove.json

# Filter file1 to exclude words that appear in file2
jq --argjson words_to_remove "$(cat words_to_remove.json)" '
    [ .[] | select(.word | IN($words_to_remove[] ) | not) ]
' "$FILE1" > "$OUTPUT_FILE"

# Clean up temporary file
rm words_to_remove.json

echo "Filtered JSON saved to $OUTPUT_FILE."
