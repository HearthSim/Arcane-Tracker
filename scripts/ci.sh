#!/usr/bin/env bash

set -x

./gradlew :app:assembleDebug :app:assembleRelease jvmTest linkDebugFrameworkMacosX64 linkReleaseFrameworkMacosX64 -Dorg.gradle.jvmargs=-Xmx2g

curl -s "https://get.sdkman.io" | bash > /dev/null
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install kotlin
sdk install kscript

./scripts/uploadRelease.kts