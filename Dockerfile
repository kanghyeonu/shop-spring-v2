FROM azul/zulu-openjdk:17-jre

WORKDIR /app

COPY build/libs/shop-spring-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]