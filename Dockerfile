FROM eclipse-temurin:17-jdk-alpine AS builder

RUN apk add binutils && jlink \
    --module-path "$JAVA_HOME/jmods" \
    --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.jfr,jdk.unsupported \
    --verbose \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /opt/jre-minimal

FROM alpine
RUN apk add --no-cache graphviz font-noto-cjk && rm -rf /usr/share/fonts/noto/NotoSerifCJK* 

COPY --from=builder /opt/jre-minimal /opt/jre-minimal

ENV JAVA_HOME=/opt/jre-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

COPY build/libs/java-static-blog-full.jar /root/java-static-blog-full.jar
