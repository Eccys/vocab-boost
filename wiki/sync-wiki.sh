#!/bin/bash

# Wiki Sync Script
# This script syncs the wiki directory contents to the GitHub wiki repository
# Usage: ./sync-wiki.sh [github_wiki_url]

# Set default GitHub wiki URL if not provided
GITHUB_WIKI_URL=${1:-"https://github.com/Eccys/vocab-boost.wiki.git"}
WIKI_REPO_DIR="vocab-boost.wiki"
WIKI_SOURCE_DIR="wiki"
EXCLUDED_FILES="sync-wiki.sh"

# Text styling
BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BOLD}Vocab Wiki Sync Tool${NC}"
echo "============================================"
echo "This script will sync your local wiki content to the GitHub wiki repository."
echo -e "GitHub Wiki URL: ${YELLOW}${GITHUB_WIKI_URL}${NC}"
echo

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: git is not installed. Please install git and try again.${NC}"
    exit 1
fi

# Confirm with the user
read -p "Continue with the sync? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled by user."
    exit 0
fi

echo
echo -e "${BOLD}Step 1:${NC} Cloning the GitHub wiki repository..."
if [ -d "$WIKI_REPO_DIR" ]; then
    echo "Wiki repository directory already exists. Removing to ensure clean sync..."
    rm -rf "$WIKI_REPO_DIR"
fi

git clone "$GITHUB_WIKI_URL" "$WIKI_REPO_DIR"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to clone the GitHub wiki repository.${NC}"
    echo "Please check the URL and your network connection."
    exit 1
fi

echo
echo -e "${BOLD}Step 2:${NC} Copying wiki content..."
# Remove existing files in the wiki repo (except .git directory)
find "$WIKI_REPO_DIR" -mindepth 1 -not -path "$WIKI_REPO_DIR/.git*" -delete

# Create images directory in the wiki repo if it doesn't exist
mkdir -p "$WIKI_REPO_DIR/images"

# Copy all files from wiki source directory to the wiki repo
for file in $(find "$WIKI_SOURCE_DIR" -type f -not -path "$WIKI_SOURCE_DIR/images*" | grep -v "$EXCLUDED_FILES"); do
    echo "Copying $(basename "$file")"
    cp "$file" "$WIKI_REPO_DIR/"
done

# Copy images directory contents if it exists
if [ -d "$WIKI_SOURCE_DIR/images" ]; then
    echo "Copying images directory..."
    cp -R "$WIKI_SOURCE_DIR/images/"* "$WIKI_REPO_DIR/images/"
fi

echo
echo -e "${BOLD}Step 3:${NC} Committing and pushing changes..."
cd "$WIKI_REPO_DIR"

# Add all files to git
git add .

# Check if there are changes to commit
if git diff --cached --quiet; then
    echo -e "${YELLOW}No changes detected. Nothing to commit.${NC}"
    cd ..
    rm -rf "$WIKI_REPO_DIR"
    exit 0
fi

# Commit changes
git commit -m "Update wiki content via sync script"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to commit changes.${NC}"
    cd ..
    rm -rf "$WIKI_REPO_DIR"
    exit 1
fi

# Push changes
git push
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to push changes to the GitHub wiki repository.${NC}"
    echo "Please check your credentials and try again."
    cd ..
    rm -rf "$WIKI_REPO_DIR"
    exit 1
fi

cd ..

echo
echo -e "${GREEN}Wiki content successfully synced to GitHub!${NC}"
echo "The following files were updated:"
ls -la "$WIKI_REPO_DIR" | grep -v "^d" | grep -v ".git"

# Clean up
echo
read -p "Would you like to remove the temporary wiki repository? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf "$WIKI_REPO_DIR"
    echo "Temporary wiki repository removed."
else
    echo -e "Temporary wiki repository not removed. You can find it at: ${YELLOW}${WIKI_REPO_DIR}${NC}"
fi

echo
echo -e "${BOLD}Wiki sync complete!${NC}"
echo "============================================"
echo -e "You can view your wiki at: ${YELLOW}https://github.com/Eccys/vocab-boost/wiki${NC}"
echo 