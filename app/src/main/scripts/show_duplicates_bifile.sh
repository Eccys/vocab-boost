#!/bin/bash

# Check if two arguments are provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 file1.json file2.json"
    exit 1
fi

FILE1="$1"
FILE2="$2"

# Extract words from both JSON files and find common entries
jq -r '.[].word' "$FILE1" | sort > words1.txt
jq -r '.[].word' "$FILE2" | sort > words2.txt

# Print repeated words
comm -12 words1.txt words2.txt

# Clean up temporary files
rm words1.txt words2.txt

