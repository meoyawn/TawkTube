FROM anapsix/alpine-java
RUN ["./gradlew", "jar"]
CMD ["java", "-jar", "build/libs/youtube-1.0-SNAPSHOT.jar"]
