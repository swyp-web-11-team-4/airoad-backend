package plugin

import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins { id("com.diffplug.spotless") }

spotless {
    // Java 파일 포맷팅
    java {
        target("src/**/*.java")
        targetExclude("build/**")

        // Google Java Formatter 사용
        googleJavaFormat()

        // Import 순서 정리
        importOrder("java", "javax", "jakarta", "org", "com", "", "\\#")

        // 불필요한 import 제거
        removeUnusedImports()
    }

    // Kotlin 파일 포맷팅
    kotlin {
        target("src/**/*.kt")
        targetExclude("build/**")

        // ktfmt 포맷터 사용
        ktfmt().kotlinlangStyle()

        // 들여쓰기 및 공백 정리
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }

    // Gradle 파일 포맷팅
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/**/*.gradle.kts")

        ktfmt().kotlinlangStyle()
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }

    // JSON 파일 포맷팅
    json {
        target("src/**/*.json")
        targetExclude("build/**")

        gson().indentWithSpaces(4)
    }

    // YAML 파일 포맷팅
    format("yaml") {
        target("src/**/*.yml", "src/**/*.yaml")
        targetExclude("build/**")

        prettier()
    }

    // Properties 파일 포맷팅
    format("properties") {
        target("src/**/*.properties")
        targetExclude("build/**")

        trimTrailingWhitespace()
        endWithNewline()
    }

    // XML 파일 포맷팅
    format("xml") {
        target("src/**/*.xml")
        targetExclude("build/**")

        eclipseWtp(EclipseWtpFormatterStep.XML)
    }
}
