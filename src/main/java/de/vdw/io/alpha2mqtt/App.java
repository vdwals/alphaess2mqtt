package de.vdw.io.alpha2mqtt;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    Map<String, String> environmentVariables = System.getenv();

    Credentials c = new Credentials(environmentVariables.get("ALPHA.USERNAME"),
        environmentVariables.get("ALPHA.PASSWORD"));

    EasyDI ed = new EasyDI();
    ed.bindInstance(EasyDI.class, ed);

    ed.bindInstance(Credentials.class, c);
    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    log.info("Connect to MQTT-Broker");
    HomeAssistantMQTTService homeAssistantMQTTService = de.vdw.it.hamqtt.utils.ServiceFactory
        .createHomeAssistantMQTTService(environmentVariables.get("MQTT.HOST"),
            environmentVariables.get("MQTT.PORT"), environmentVariables.get("MQTT.USERNAME"),
            environmentVariables.get("MQTT.PASSWORD").toCharArray(), "alpha_energy",
            environmentVariables.getOrDefault("MQTT.DISCOVERY_TOPIC", "homeassistant"),
            "Alpha ESS Proxy", environmentVariables.getOrDefault("MQTT.PROTOCOLL", "tcp"));

    ed.bindInstance(HomeAssistantMQTTService.class, homeAssistantMQTTService);

    ed.bindInstance(ScheduledExecutorService.class, Executors.newSingleThreadScheduledExecutor());

    ed.markAsSingleton(Cache.class);

    App app = ed.getInstance(App.class);

    app.init();
    app.start();
  }

  Cache cache;

  SystemService systemService;

  ServiceFactory serviceFactory;

  MqttService mqttService;

  public void init() {
    serviceFactory.init();

    mqttService.init();
  }

  public void start() {
    serviceFactory.getUpdateServices().forEach(Updater::init);
  }
}
