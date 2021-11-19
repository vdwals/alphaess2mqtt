package de.vdwals.io.alpha2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.it.hamqtt.Service;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.utils.ServiceFactory;
import de.vdwals.io.alpha2mqtt.services.BatteryDeviceService;
import de.vdwals.io.alpha2mqtt.services.RunningDataUpdateService;
import de.vdwals.io.alpha2mqtt.services.SummeryDataUpdateService;
import eu.lestard.easydi.EasyDI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.connection_config.DBConfiguration;

@Slf4j
@RequiredArgsConstructor
public class App {

  private final BatteryDeviceService batteryDeviceService;

  private final Service mqttService;

  private final EasyDI easyDI;

  private List<Device> devices;

  public static void main(String[] args) {
    log.info("Load DB Settings");
    DBConfiguration.loadConfiguration("/database.properties");

    log.info("Connect to MQTT-Broker");
    Map<String, String> envs = System.getenv();

    Service mqttService = ServiceFactory.getMqttService(envs.get("MQTT_HOST"),
        envs.get("MQTT_PORT"),
        envs.get("MQTT_USERNAME"),
        envs.get("MQTT_PASSWORD").toCharArray(),
        "alpha_energy",
        envs.getOrDefault("MQTT_DISCOVERY_TOPIC", "homeassistant"),
        "Alpha ESS Proxy");

    EasyDI ed = new EasyDI();
    ed.bindInstance(ObjectMapper.class, new ObjectMapper());
    ed.bindInstance(Service.class, mqttService);
    ed.bindInstance(ScheduledExecutorService.class, Executors.newSingleThreadScheduledExecutor());
    ed.bindInstance(EasyDI.class, ed);

    App app = ed.getInstance(App.class);

    app.init();
    app.start();
  }

  public void init() {
    mqttService.connect();

    devices = batteryDeviceService.getBatteryDevices();

    log.info("Publish configs");
    mqttService.addDevices(devices);
    mqttService.publishConfigs();
  }

  public void start() {

    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    devices.forEach(device -> {
      RunningDataUpdateService runningDataUpdateService =
          easyDI.getInstance(RunningDataUpdateService.class);

      runningDataUpdateService.init(device);

      SummeryDataUpdateService summeryDataUpdateService =
          easyDI.getInstance(SummeryDataUpdateService.class);

      summeryDataUpdateService.init(device);
    });
  }
}
