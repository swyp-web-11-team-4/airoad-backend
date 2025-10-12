package plugin

plugins { id("org.sonarqube") }

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "swyp-web-11-team-4")
        property("sonar.projectKey", "swyp-web-11-team-4_airoad-backend")
        property("sonar.token", System.getenv("SONAR_TOKEN"))
        property("sonar.projectVersion", version)

        // 소스 및 바이너리 경로 설정
        property("sonar.sources", "src/main")
        property("sonar.tests", "src/test")
        property("sonar.java.binaries", "build/classes/java/main")
        property("sonar.java.test.binaries", "build/classes/java/test")

        // JaCoCo 커버리지 리포트 연동
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "build/reports/jacoco/test/jacocoTestReport.xml"
        )

        // 커버리지 측정에서 제외할 파일 패턴
        property(
            "sonar.coverage.exclusions",
            "**/*Application.java," + "**/common/**," + "**/*Constants.java"
        )

        // 분석 제외 패턴
        property(
            "sonar.exclusions",
            "**/build/**," +
                "**/generated/**," +
                "**/*.gradle.kts," +
                "**/gradlew," +
                "**/gradlew.bat"
        )

        // 코드 품질 게이트 설정
        property("sonar.qualitygate.wait", "false")

        // 언어 설정
        property("sonar.language", "java")
        property("sonar.java.source", "17")

        // 인코딩 설정
        property("sonar.sourceEncoding", "UTF-8")

        // 중복 코드 검출 설정
        property("sonar.cpd.exclusions", "**/*Test.java,**/*Tests.java")
    }
}
