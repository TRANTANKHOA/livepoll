#!/bin/bash
# ==============================================================================
# LivePulse - Production Release Keystore Generator
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

KEYSTORE_FILE="release.keystore"
KEY_ALIAS="upload"

# keytool from PATH, else Homebrew's keg-only JDK (macOS /usr/bin/keytool is a stub)
KEYTOOL="$(command -v keytool || true)"
if [ -z "$KEYTOOL" ] || ! "$KEYTOOL" -help >/dev/null 2>&1; then
    KEYTOOL="/opt/homebrew/opt/openjdk@17/bin/keytool"
fi

# Password from env, else prompt. Never a hardcoded default.
STORE_PASSWORD="${RELEASE_STORE_PASSWORD:-}"
if [ -z "$STORE_PASSWORD" ]; then
    read -sp "Keystore password: " STORE_PASSWORD
    echo
fi

echo -e "${CYAN}${BOLD}🔐 LivePulse - Generating Production Release Keystore${NC}\n"

if [ -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}⚠️ '$KEYSTORE_FILE' already exists in the current directory.${NC}"
    read -p "Do you want to overwrite it? (y/N): " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        echo "Aborting keystore generation."
        exit 0
    fi
    rm "$KEYSTORE_FILE"
fi

echo -e "Generating 2048-bit RSA Keystore valid for 25 years (10,000 days)..."
"$KEYTOOL" -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$STORE_PASSWORD" \
    -dname "CN=LivePulse, OU=Mobile, O=Plutusoft, L=Ho Chi Minh, C=VN"

echo -e "\n${GREEN}${BOLD}✓ Keystore generated successfully: ${KEYSTORE_FILE}${NC}\n"

echo -e "${CYAN}${BOLD}📋 Certificate Fingerprints (Required for Firebase & Google Sign-In):${NC}"
"$KEYTOOL" -list -v -keystore "$KEYSTORE_FILE" -alias "$KEY_ALIAS" -storepass "$STORE_PASSWORD" | grep -E "SHA1|SHA256"

echo -e "\n${YELLOW}${BOLD}👉 NEXT STEP:${NC}"
echo -e "1. Copy the SHA-1 and SHA-256 fingerprints above."
echo -e "2. Go to Firebase Console -> Project Settings -> Your Android App -> Add Fingerprint."
echo -e "3. Download the updated 'google-services.json' and place it in the 'app/' directory."
