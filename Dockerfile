# 1. 자바 21 이미지 사용
FROM eclipse-temurin:21-jdk-alpine

# 2. Maven 빌드 후 jar -> (target 폴더)로 복사
COPY target/*.jar app.jar

# 3. 실행 명령어
ENTRYPOINT ["java", "-jar", "/app.jar"]