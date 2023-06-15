FROM openjdk:17-slim

ENV VERTICLE_HOME /usr/verticles

# jar
ENV VERTICLE_JAR rest-machinegun-1.0.0-SNAPSHOT-fat.jar

# app config file
ENV VERTX_CONFIG_PATH config.json

EXPOSE 9999

# Copy jar to container
COPY build/libs/$VERTICLE_JAR $VERTICLE_HOME/

VOLUME ["/usr/verticles/logs"]

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]

CMD ["exec java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dlogback.configurationFile=logback.xml -jar $VERTICLE_JAR"]
