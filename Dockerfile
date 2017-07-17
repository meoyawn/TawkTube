FROM anapsix/alpine-java
COPY ./workdir/build/libs/youtube-1.0-SNAPSHOT.jar run.jar
CMD ["java", "-jar", "run.jar"]
