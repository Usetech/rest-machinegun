#!bin/bash

java -Dvertx-config-path=config.json -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -Dlogback.configurationFile=logback.xml io.vertx.core.Launcher run com.usetech.rest_machinegun.MainVerticle
