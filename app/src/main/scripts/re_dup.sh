#!/bin/bash

# Check if a file argument is provided
if [ $# -ne 1 ]; then
    echo "Usage: $0 <input_json_file>"
    echo "Example: $0 vocabulary.json"
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="${INPUT_FILE%.json}_unique.json"

# Check if the input file exists
if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: File '$INPUT_FILE' not found."
    exit 1
fi

# Check if jq is installed
if ! command -v jq > /dev/null 2>&1; then
    echo "Error: 'jq' is required but not installed. Please install it (e.g., 'sudo apt install jq' or 'brew install jq')."
    exit 1
fi

# Process the JSON file:
# 1. Parse the JSON array
# 2. Use 'unique_by' to keep only the first occurrence of each word based on the 'word' field
# 3. Output the result to a new file
jq 'unique_by(.word)' "$INPUT_FILE" > "$OUTPUT_FILE"

# Check if the operation was successful
if [ $? -eq 0 ]; then
    echo "Success: Duplicate words removed. Output saved to '$OUTPUT_FILE'."
    # Count entries in original and new files for verification
    ORIGINAL_COUNT=$(jq length "$INPUT_FILE")
    UNIQUE_COUNT=$(jq length "$OUTPUT_FILE")
    echo "Original entries: $ORIGINAL_COUNT"
    echo "Unique entries: $UNIQUE_COUNT"
else
    echo "Error: Failed to process the file. Check the JSON format."
    exit 1
fi

exit 0
