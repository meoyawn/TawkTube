FROM anapsix/alpine-java
RUN ["ls", "-1"]
RUN ["ls", "~/repo"]
COPY ~/repo/build/libs/youtube-1.0-SNAPSHOT.jar run.jar
CMD ["java", "-jar", "run.jar"]
