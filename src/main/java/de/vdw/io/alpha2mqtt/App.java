package de.vdw.io.alpha2mqtt;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.Cache;
import de.vdw.io.alpha2mqtt.models.Credentials;
import de.vdw.io.alpha2mqtt.services.MqttService;
import de.vdw.io.alpha2mqtt.services.ServiceFactory;
import de.vdw.io.alpha2mqtt.services.alpha.get.SystemService;
import de.vdw.io.alpha2mqtt.services.updater.Updater;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import eu.lestard.easydi.EasyDI;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Value
public class App {

  public static void main(String[] args) throws MqttException {
    log.info("Starting Application");

    Map<String, String> environmentVariables = System.getenv();

    if (environmentVariables.containsKey("LOG_LEVEL")) {
      switch (environmentVariables.get("LOG_LEVEL")) {
        case "TRACE":
          Configurator.setRootLevel(Level.TRACE);
          log.trace("Trace level logging activated");
          break;
        case "DEBUG":
          Configurator.setRootLevel(Level.DEBUG);
          log.trace("Debug level logging activated");
          break;
        default:
          log.warn("Unexpected value: " + environmentVariables.get("LOG_LEVEL"));
      }
    }

    Credentials c = new Credentials(environmentVariables.get(Constants.ALPHA_USERNAME),
        environmentVariables.get(Constants.ALPHA_PASSWORD));

    EasyDI ed = new EasyDI();
    ed.bindInstance(EasyDI.class, ed);

    ed.bindInstance(Credentials.class, c);
    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    log.info("Connect to MQTT-Broker");
    HomeAssistantMQTTService homeAssistantMQTTService = de.vdw.it.hamqtt.utils.ServiceFactory
        .createHomeAssistantMQTTService(environmentVariables.get(Constants.MQTT_HOST),
            environmentVariables.get(Constants.MQTT_PORT),
            environmentVariables.get(Constants.MQTT_USERNAME),
            environmentVariables.get(Constants.MQTT_PASSWORD).toCharArray(),
            environmentVariables.getOrDefault(Constants.MQTT_TOPIC, "alpha_energy"),
            environmentVariables.getOrDefault(Constants.MQTT_DISCOVERY_TOPIC, "homeassistant"),
            "Alpha ESS Proxy", environmentVariables.getOrDefault(Constants.MQTT_PROTOCOLL, "tcp"));

    ed.bindInstance(HomeAssistantMQTTService.class, homeAssistantMQTTService);

    ed.bindInstance(ScheduledExecutorService.class, Executors.newSingleThreadScheduledExecutor());

    ed.markAsSingleton(Cache.class);
    ed.markAsSingleton(MqttService.class);
    ed.markAsSingleton(SystemService.class);
    ed.markAsSingleton(ServiceFactory.class);

    App app = ed.getInstance(App.class);

    app.init();
    app.start();
  }

  Cache cache;

  SystemService systemService;

  ServiceFactory serviceFactory;

  MqttService mqttService;

  public void init() {
    this.serviceFactory.init();

    this.mqttService.init();
  }

  public void start() {
    this.serviceFactory.getUpdateServices().forEach(Updater::init);
  }
}
