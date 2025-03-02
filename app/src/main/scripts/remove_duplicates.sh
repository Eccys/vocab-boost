#!/bin/bash

# Input JSON file
INPUT_FILE="$1"
OUTPUT_FILE="output.json"

# Process JSON: Keep only the last occurrence of each "word"
jq 'reverse | unique_by(.word) | reverse' "$INPUT_FILE" > "$OUTPUT_FILE"

echo "Duplicates removed. Cleaned JSON saved to $OUTPUT_FILE."

