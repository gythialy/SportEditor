// Top-level build file. Plugin versions live in gradle/libs.versions.toml.

// AGP 9.4.1 pulls vulnerable transitive versions onto the build classpath.
// These jars are only used by the build tooling and are not packaged into the APK,
// but pinning them here clears the Dependabot alerts without changing runtime code.
buildscript {
    configurations.all {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.85",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.bouncycastle:bcutil-jdk18on:1.85",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.jdom:jdom2:2.0.6.1",
            "org.apache.commons:commons-lang3:3.18.0",
            "org.apache.httpcomponents:httpclient:4.5.14"
        )
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

// Code formatting. Run `./gradlew spotlessApply` before committing;
// CI runs `./gradlew spotlessCheck` (also wired into `check` by Spotless).
spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable")
        )
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
    format("misc") {
        target("*.md", "docs/**/*.md", "scripts/**/*.md", ".github/**/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
