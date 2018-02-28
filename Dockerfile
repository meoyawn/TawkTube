FROM kperson/alpine-java-8-ffmpeg
COPY build/libs/youtube-1.0.jar run.jar
CMD ["java", "-jar", "run.jar"]
EXPOSE 8080
