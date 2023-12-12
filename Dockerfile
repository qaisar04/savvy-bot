FROM openjdk:17

ADD target/spring-boot-tg-docker.jar spring-boot-tg-docker.jar

ENTRYPOINT ["java", "-jar", "spring-boot-tg-docker.jar"]

