pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProotCowork"

include(":app")

include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("third_party/termux-app/terminal-emulator")

include(":terminal-view")
project(":terminal-view").projectDir = file("third_party/termux-app/terminal-view")

include(":termux-shared")
project(":termux-shared").projectDir = file("third_party/termux-app/termux-shared")

include(":shell-loader:stub")
project(":shell-loader:stub").projectDir = file("third_party/termux-x11/shell-loader/stub")

include(":termux-x11-app")
project(":termux-x11-app").projectDir = file("third_party/termux-x11/app")
