FROM alpine:3.14
WORKDIR /app

COPY . .

EXPOSE 8080

RUN  apk update \
  && apk upgrade \
  && apk add --update openjdk11 tzdata curl unzip bash \
  && rm -rf /var/cache/apk/*

RUN javac -cp json.jar Serveur.java
CMD ["java", "-cp", "json.jar", "Serveur.java"]

