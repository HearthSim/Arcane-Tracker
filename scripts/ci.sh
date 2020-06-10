#!/usr/bin/env bash

set -e

export PATH="$ANDROID_HOME"/tools/bin:$PATH
sdkmanager --install 'ndk;21.0.6113669'  >/dev/null

echo "$GOOGLE_SERVICES_JSON" > app/google-services.json

./gradlew :app:assembleDebug :app:assembleRelease jvmTest -x kotlin-hsreplay-api:jvmTest linkDebugFrameworkMacosX64 linkReleaseFrameworkMacosX64 -Dorg.gradle.jvmargs=-Xmx2g

curl -s "https://get.sdkman.io" | bash > /dev/null
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install kotlin
sdk install kscript

./scripts/uploadRelease.kts