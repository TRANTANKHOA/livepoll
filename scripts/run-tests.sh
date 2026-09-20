#!/bin/bash
# ==============================================================================
# PulsePoll - Run Test Suite (Local macOS / Unix)
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}⚡ Running PulsePoll Local Unit & JVM Test Suite...${NC}\n"

# Use ./gradlew if present, else fallback to system gradle
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

$GRADLE_CMD :app:testDebugUnitTest "$@"

echo -e "\n${GREEN}${BOLD}✓ All Unit and JVM Tests Passed Successfully!${NC}"
echo -e "HTML Test Report: ${CYAN}app/build/reports/tests/testDebugUnitTest/index.html${NC}"
echo -e "Open in browser:  ${BOLD}open app/build/reports/tests/testDebugUnitTest/index.html${NC}\n"
