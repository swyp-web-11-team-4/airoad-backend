package plugin

plugins { id("jacoco") }

// 제외할 패턴을 상수로 정의
val excludePatterns =
    listOf(
        "**/config/**",
        "**/domain/**",
        "**/dto/**",
        "**/exception/**",
        "**/agent/**",
        "**/chat/presentation/web/**", // TODO: 명세용 controller만 만든 상황이라 controller 구현 후 제거하겠습니다.
        "**/trip/presentation/web/**", // TODO: API 구현 후 삭제 필요
        "**/*Application*",
        "**/*Config*",
        "**/*Dto*",
        "**/*Entity*",
        "**/*Exception*",
        "**/*ErrorCode*",
        "**/*Handler*",
        "**/*Dummy*"
    )

// 제외할 클래스 패턴 (커버리지 검증용)
val excludeClassPatterns =
    listOf(
        "*.*Application*",
        "*.*Config*",
        "*.*Dto*",
        "*.*Entity*",
        "*.*Exception*",
        "*.*ErrorCode*",
        "*.*Handler*"
    )

fun JacocoReportBase.configureClassDirectories() {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(excludePatterns) } })
    )
}

jacoco { reportsDirectory = layout.buildDirectory.dir("reports/jacoco") }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    reports {
        xml.required = true
        html.required = true
        csv.required = true
    }

    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")) { include("**/*.exec") })

    configureClassDirectories()
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))

    violationRules {
        // 프로젝트 전체 커버리지 검증
        rule {
            element = "BUNDLE"

            limit {
                minimum = 0.60.toBigDecimal()
                counter = "LINE"
            }
            limit {
                minimum = 0.60.toBigDecimal()
                counter = "BRANCH"
            }
        }

        // 클래스별 커버리지 검증
        rule {
            element = "CLASS"
            excludes = excludeClassPatterns

            limit {
                minimum = 0.60.toBigDecimal()
                counter = "LINE"
            }
        }
    }

    configureClassDirectories()
}
