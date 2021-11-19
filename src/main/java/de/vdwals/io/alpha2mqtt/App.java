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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.connection_config.DBConfiguration;

@Slf4j
public class App {

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
        envs.getOrDefault("MQTT_DISCOVERY_TOPIC",
            "homeassistant"),
        "Alpha ESS Proxy");

    mqttService.connect();

    log.info("Load and init batteries");
    List<DeviceInformation> deviceInformationList = Base.withDb(
        () -> AlphaEssBattery.findAll().stream().map(battery -> (AlphaEssBattery) battery)
            .map(battery -> {
              return DeviceInformation.builder().manufacturer("Alpha ESS")
                  .model("Smile5").name(battery.getSn()).identifiers(List.of(battery.getSn()))
                  .build();
            }).collect(Collectors.toList()));

    log.info("Create sensors");
    List<Device> devices = deviceInformationList.stream().map(deviceInformation -> {
      String deviceId = getDeviceId(deviceInformation);

      Device battery = new Device(deviceId, deviceInformation);

      Sensor batteryLoad = Sensor.builder().deviceClass(DeviceClass.battery)
          .device(deviceInformation)
          .objectId("soc")
          .uniqueId(getUniqueId(deviceId, "soc"))
          .name(getName(deviceInformation, "SOC")).build();
      battery.addEntity(batteryLoad);

      Sensor batteryEnergy = Sensor.builder().deviceClass(DeviceClass.energy)
          .device(deviceInformation)
          .objectId("pBat")
          .uniqueId(getUniqueId(deviceId, "pBat"))
          .name(getName(deviceInformation, "Leistung"))
          .unit_of_measurement("W").build();
      battery.addEntity(batteryEnergy);

      Sensor pvPower = Sensor.builder().deviceClass(DeviceClass.energy).device(deviceInformation)
          .objectId("ppvTotal").uniqueId(getUniqueId(deviceId, "ppvTotal"))
          .name(getName(deviceInformation, "PV-Leistung")).unit_of_measurement("W").build();
      battery.addEntity(pvPower);

      Sensor gridPower = Sensor.builder().deviceClass(DeviceClass.energy).device(deviceInformation)
          .objectId("gridPower").uniqueId(getUniqueId(deviceId, "gridPower"))
          .name(getName(deviceInformation, "Netz-Leistung")).unit_of_measurement("W").build();
      battery.addEntity(gridPower);

      Sensor powerConsumption = Sensor.builder().deviceClass(DeviceClass.energy)
          .device(deviceInformation)
          .objectId("powerConsumption").uniqueId(getUniqueId(deviceId, "powerConsumption"))
          .name(getName(deviceInformation, "Verbraucher Leistung")).unit_of_measurement("W")
          .build();
      battery.addEntity(powerConsumption);

      return battery;
    }).collect(Collectors.toList());

    log.info("Publish configs");
    mqttService.addDevices(devices);
    mqttService.publishConfigs();

    EasyDI ed = new EasyDI();

    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    RunningDataService runningDataService = ed.getInstance(RunningDataService.class);

    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    devices.forEach(device -> {
      Runnable runningData = new Runnable() {
        @Override
        public void run() {
          log.info("Update date");
          RunningDataDto data = runningDataService.getData();

          String deviceId = getDeviceId(device.getDeviceInformation());

          double totalPvPower = data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4()
              + data.getPmeter_dc();
          double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();

          device.updateValue("soc", data.getSoc());
          device.updateValue("pBat", data.getPbat());
          device.updateValue("ppvTotal",
              totalPvPower);
          device.updateValue("gridPower",
              totalGridPower);
          device.updateValue("powerConsumption",
              totalGridPower + totalPvPower + data.getPbat());

          mqttService.publishValues();

          long delay = runningDataService.getNextRefreshInSeconds();
          log.info("Next update at in {} seconds", delay);
          ses.schedule(this, delay, TimeUnit.SECONDS);
        }
      };

      long nextRefresh = runningDataService.getNextRefreshInSeconds();
      log.info("Start scheduling in {} seconds", nextRefresh);
      ses.schedule(runningData, nextRefresh, TimeUnit.SECONDS);
    });
  }

  private static String getName(DeviceInformation deviceInformation, String name) {
    return String.join(" ", deviceInformation.getManufacturer(), deviceInformation.getName(),
        name);
  }

  private static String getUniqueId(String deviceId, String objectId) {
    return String.join("_", deviceId, objectId);
  }

  private static String getDeviceId(DeviceInformation device) {
    return String.join("_", device.getManufacturer(), device.getModel(),
        device.getName()).toLowerCase().replace(" ", "");
  }
}
