package de.vdwals.io.alpha2mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.it.hamqtt.Service;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.utils.ServiceFactory;
import de.vdwals.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdwals.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdwals.io.alpha2mqtt.services.RunningDataService;
import eu.lestard.easydi.EasyDI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.connection_config.DBConfiguration;

public class App {

  public static void main(String[] args) {
    DBConfiguration.loadConfiguration("/database.properties");

    Map<String, String> envs = System.getenv();
    Service mqttService = ServiceFactory.getMqttService(envs.get("MQTT_HOST"),
        envs.get("MQTT_PORT"),
        envs.get("MQTT_USERNAME"),
        envs.get("MQTT_PASSWORD").toCharArray(),
        "alpha_energy",
        envs.getOrDefault("MQTT_DISCOVERY_TOPIC",
            "homeassistant"),
        "Alpha ESS Proxy");

    mqttService.connect();

    List<DeviceInformation> deviceInformations = Base.withDb(
        () -> AlphaEssBattery.findAll().stream().map(battery -> (AlphaEssBattery) battery)
            .map(battery -> {
              return DeviceInformation.builder().manufacturer("Alpha ESS")
                  .model("Smile5").name(battery.getSn()).build();
            }).collect(Collectors.toList()));

    List<Device> devices = deviceInformations.stream().map(deviceInformation -> {
      String deviceId = getDeviceId(deviceInformation);

      Device battery = new Device(deviceId, deviceInformation);

      String socId = getSocId(deviceId);
      Sensor batteryLoad = Sensor.builder().deviceClass(DeviceClass.battery)
          .device(deviceInformation)
          .objectId(socId)
          .uniqueId(socId)
          .name(String.join(" ", deviceInformation.getManufacturer(), deviceInformation.getName(),
              "SOC")).build();
      battery.addEntity(batteryLoad);

      String pBatId = getPBat(deviceId);
      Sensor batteryEnergy = Sensor.builder().deviceClass(DeviceClass.energy)
          .device(deviceInformation)
          .objectId(pBatId)
          .uniqueId(pBatId)
          .name(String.join(" ", deviceInformation.getManufacturer(), deviceInformation.getName(),
              "Leistung"))
          .unit_of_measurement("W").build();
      battery.addEntity(batteryEnergy);

      return battery;
    }).collect(Collectors.toList());

    mqttService.addDevices(devices);
    mqttService.publishConfigs();

    EasyDI ed = new EasyDI();

    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    RunningDataService runningDataService = ed.getInstance(RunningDataService.class);

    Timer dataTimer = new Timer("Data Timer");

    devices.forEach(device -> {
      TimerTask tt = new TimerTask() {
        @Override
        public void run() {
          RunningDataDto data = runningDataService.getData();
          LocalDateTime nextRefresh = runningDataService.getNextRefresh();

          String deviceId = getDeviceId(device.getDeviceInformation());

          device.updateValue(getSocId(deviceId), String.valueOf(data.getSoc()));
          device.updateValue(getPBat(deviceId), String.valueOf(data.getPbat()));

          mqttService.publishValues();
          dataTimer.schedule(this,
              convertToDateViaInstant(
                  nextRefresh));
        }
      };

      LocalDateTime nextRefresh = runningDataService.getNextRefresh();
      if (nextRefresh == null || nextRefresh
          .isBefore(LocalDateTime.now().plusSeconds(30))) {
        dataTimer.schedule(tt, TimeUnit.SECONDS.toMillis(30));
      } else {
        dataTimer.schedule(tt, convertToDateViaInstant(nextRefresh));
      }
    });
  }

  private static Date convertToDateViaInstant(LocalDateTime dateToConvert) {
    return Date
        .from(dateToConvert.atZone(ZoneId.systemDefault())
            .toInstant());
  }

  private static String getPBat(String deviceId) {
    return String.join("_", deviceId, "pbat");
  }

  private static String getSocId(String deviceId) {
    return String.join("_", deviceId, "soc");
  }

  private static String getDeviceId(DeviceInformation device) {
    return String.join("_", device.getManufacturer(), device.getModel(),
        device.getName()).toLowerCase().replace(" ", "");
  }
}
