FROM maven:3.8.5-openjdk-17-slim AS build
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn clean package

FROM openjdk:17-jdk-alpine

RUN apk update && apk add  vim iputils net-tools busybox-extras curl wget

COPY --from=build /app/target/solaris-network-function-manager-1.0.0.jar /usr/src/nfm.jar
WORKDIR /usr/src/



