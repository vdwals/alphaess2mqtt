package de.vdw.io.alpha2mqtt;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.Cache;
import de.vdw.io.alpha2mqtt.models.Credentials;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
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

    EnvironmentService environmentService = new EnvironmentService();

    EasyDI ed = new EasyDI();
    ed.bindInstance(EasyDI.class, ed);

    ed.bindInstance(EnvironmentService.class, environmentService);
    ed.bindInstance(Credentials.class, environmentService.getCredentials());
    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    log.info("Connect to MQTT-Broker");
    HomeAssistantMQTTService homeAssistantMQTTService =
        de.vdw.it.hamqtt.utils.ServiceFactory.createHomeAssistantMQTTService(
            environmentService.mqttHost(), environmentService.mqttPort(),
            environmentService.mqttUsername(), environmentService.mqttPassword(),
            environmentService.mqttTopic(), environmentService.mqttDiscoveryTopic(),
            "Alpha ESS Proxy", environmentService.mqttProtocoll());

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
