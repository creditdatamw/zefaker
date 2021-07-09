FROM openjdk:16-alpine3.13

MAINTAINER Zikani Nyirenda Mwase <zikani@creditdatamw.com>
COPY build/libs/zefaker-all.jar /zefaker.jar

ENTRYPOINT ["java", "-jar", "/zefaker.jar"]
