FROM eclipse-temurin:21.0.1_12-jre-alpine
WORKDIR /data
RUN apk add git
RUN apk add openssh
COPY build/install/gaybot /app
CMD ["/app/bin/gaybot"]
