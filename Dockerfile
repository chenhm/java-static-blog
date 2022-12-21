FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y graphviz locales fonts-arphic-uming && locale-gen zh_CN.UTF-8
COPY build/libs/java-static-blog-full.jar /root/java-static-blog-full.jar
