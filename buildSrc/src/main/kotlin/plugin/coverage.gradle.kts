package plugin

plugins { id("jacoco") }

// 제외할 패턴을 상수로 정의
val excludePatterns =
    listOf(
        "**/config/**",
        "**/domain/**",
        "**/dto/**",
        "**/exception/**",
        "**/ai/**", // AI는 E2E 테스트로 대체
        "**/tourapi/**", // Tour API 데이터 동기화 도메인 제외
        "**/*Application*",
        "**/*Config*",
        "**/*Dto*",
        "**/*Entity*",
        "**/*Exception*",
        "**/*ErrorCode*",
        "**/*Handler*",
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
        "*.*Handler*",

        // OAuth2 관련 일부 테스트는 E2E 테스트로 검증, 추후 개발이 어느정도 진행되면 삭제
        "*.CustomOAuth2UserService",
        "*.CustomOAuth2AuthorizationRequestRepository",
        "*.OAuth2RedirectUrlResolver",
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
