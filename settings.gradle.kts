pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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

val termuxX11App = file("third_party/termux-x11/app")
if (termuxX11App.exists()) {
    include(":termux-x11-app")
    project(":termux-x11-app").projectDir = termuxX11App
    include(":shell-loader-stub")
    project(":shell-loader-stub").projectDir = file("third_party/termux-x11/shell-loader/stub")
}
