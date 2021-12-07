package de.vdw.io.alpha2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.services.RunningDataUpdateService;
import de.vdw.io.alpha2mqtt.services.SummeryDataUpdateService;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.alpha.ItemListService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.utils.ServiceFactory;
import eu.lestard.easydi.EasyDI;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.connection_config.DBConfiguration;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@RequiredArgsConstructor
@Value
public class App {

  BatteryDeviceService batteryDeviceService;

  SolarModuleDeviceService solarModuleDeviceService;

  InverterDeviceService inverterDeviceService;

  WallBoxDeviceService wallboxDeviceService;

  ChargingService chargingService;

  HomeAssistantMQTTService mqttService;

  ScheduledExecutorService scheduledExecutorService;

  EasyDI easyDI;

  public static void main(String[] args) {
    log.info("Load DB Settings");
    DBConfiguration.loadConfiguration("/database.properties");

    log.info("Connect to MQTT-Broker");
    Map<String, String> environmentVariables = System.getenv();

    HomeAssistantMQTTService mqttService =
        ServiceFactory.getMqttService(
            environmentVariables.get("MQTT_HOST"),
            environmentVariables.get("MQTT_PORT"),
            environmentVariables.get("MQTT_USERNAME"),
            environmentVariables.get("MQTT_PASSWORD").toCharArray(),
            "alpha_energy",
            environmentVariables.getOrDefault("MQTT_DISCOVERY_TOPIC", "homeassistant"),
            "Alpha ESS Proxy");

    EasyDI ed = new EasyDI();
    ed.bindInstance(ObjectMapper.class, new ObjectMapper());
    ed.bindInstance(HomeAssistantMQTTService.class, mqttService);
    ed.bindInstance(ScheduledExecutorService.class, Executors.newSingleThreadScheduledExecutor());
    ed.bindInstance(EasyDI.class, ed);

    App app = ed.getInstance(App.class);

    app.init();
    app.start();
  }

  public void init() {
    mqttService.connect();

    mqttService.addDevice(batteryDeviceService.getDevice());
    mqttService.addDevice(solarModuleDeviceService.getDevice());
    mqttService.addDevice(inverterDeviceService.getDevice());
    mqttService.addDevice(wallboxDeviceService.getDevice());

    mqttService.addCommandListener(chargingService);

    mqttService.publishConfigs();
  }

  public void start() {
    easyDI.getInstance(ItemListService.class).getData();

    RunningDataUpdateService runningDataUpdateService =
        easyDI.getInstance(RunningDataUpdateService.class);

    runningDataUpdateService.init();

    SummeryDataUpdateService summeryDataUpdateService =
        easyDI.getInstance(SummeryDataUpdateService.class);

    summeryDataUpdateService.init();
  }
}
