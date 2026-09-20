#!/bin/bash
# ==============================================================================
# PulsePoll - Build Production Release Bundle (AAB) & Release APK
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}📦 Building PulsePoll Production Release Artifacts...${NC}\n"

if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

# Check for google-services.json
if [ ! -f "app/google-services.json" ]; then
    echo -e "${YELLOW}⚠️ Notice: 'app/google-services.json' not found.${NC}"
    echo -e "If using Cloud Firestore in production, make sure to add your Firebase config."
fi

# 1. Build Android App Bundle (.aab) for Google Play Console
echo -e "${CYAN}Building Android App Bundle (:app:bundleRelease)...${NC}"
$GRADLE_CMD bundleRelease "$@"

# 2. Build Release APK (.apk) for direct distribution or QA
echo -e "${CYAN}Building Standalone Release APK (:app:assembleRelease)...${NC}"
$GRADLE_CMD assembleRelease "$@"

echo -e "\n${GREEN}${BOLD}======================================================${NC}"
echo -e "${GREEN}${BOLD}✓ Production Build Succeeded!${NC}"
echo -e "${GREEN}${BOLD}======================================================${NC}\n"

echo -e "📌 ${BOLD}Google Play Console Bundle (AAB):${NC}"
echo -e "   ${CYAN}app/build/outputs/bundle/release/app-release.aab${NC}"
echo -e "   (Upload this to Google Play Console Internal Testing or Production track)\n"

echo -e "📌 ${BOLD}Standalone Production APK:${NC}"
echo -e "   ${CYAN}app/build/outputs/apk/release/app-release-unsigned.apk${NC} (or signed)\n"
