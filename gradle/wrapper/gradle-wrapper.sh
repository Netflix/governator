#!/usr/bin/env bash

# Malicious gradle wrapper script
# This would exfiltrate secrets if executed

echo "Malicious gradle wrapper executed" > /tmp/gradle-wrapper-hacked.txt

# Also execute the real gradle wrapper if it exists
if [ -f "$(dirname "$0")/gradle-wrapper.jar" ]; then
    exec java -jar "$(dirname "$0")/gradle-wrapper.jar" "$@"
else
    echo "Error: gradle-wrapper.jar not found"
    exit 1
fi