package de.vdw.io.alpha2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.utils.ServiceFactory;
import de.vdw.io.alpha2mqtt.services.alpha.RunningDataUpdateService;
import de.vdw.io.alpha2mqtt.services.alpha.SummeryDataUpdateService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import eu.lestard.easydi.EasyDI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.connection_config.DBConfiguration;

@Slf4j
@RequiredArgsConstructor
public class App {

  private final BatteryDeviceService batteryDeviceService;

  private final SolarModuleDeviceService solarModuleDeviceService;

  private final InverterDeviceService inverterDeviceService;

  private final HomeAssistantMQTTService mqttService;

  private final ScheduledExecutorService scheduledExecutorService;

  private final EasyDI easyDI;

  public static void main(String[] args) {
    log.info("Load DB Settings");
    DBConfiguration.loadConfiguration("/database.properties");

    log.info("Connect to MQTT-Broker");
    Map<String, String> envs = System.getenv();

    HomeAssistantMQTTService mqttService = ServiceFactory.getMqttService(envs.get("MQTT_HOST"),
        envs.get("MQTT_PORT"),
        envs.get("MQTT_USERNAME"),
        envs.get("MQTT_PASSWORD").toCharArray(),
        "alpha_energy",
        envs.getOrDefault("MQTT_DISCOVERY_TOPIC", "homeassistant"),
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

    mqttService.addDevice(batteryDeviceService.getBattery());
    mqttService.addDevice(solarModuleDeviceService.getSolarModules());
    mqttService.addDevice(inverterDeviceService.getInverter());

    scheduledExecutorService.scheduleAtFixedRate(() -> {
      log.info("Publish configs");
      mqttService.publishConfigs();
    }, 0, 1, TimeUnit.HOURS);
  }

  public void start() {

    RunningDataUpdateService runningDataUpdateService =
        easyDI.getInstance(RunningDataUpdateService.class);

    runningDataUpdateService.init();

    SummeryDataUpdateService summeryDataUpdateService =
        easyDI.getInstance(SummeryDataUpdateService.class);

    summeryDataUpdateService.init();
  }
}
