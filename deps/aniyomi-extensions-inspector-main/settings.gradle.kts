dependencyResolutionManagement {
    versionCatalogs {
        create("androidcompat") {
            from(files("gradle/androidcompat.versions.toml"))
        }
    }

    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven("https://github.com/Claudemirovsky/aniyomi-extensions-tester/raw/android-jar/")
    }
}

rootProject.name = "Aniyomi Extensions Inspector"
include(":AndroidCompat")
include(":AndroidCompat:Config")
include(":inspector")
