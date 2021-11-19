package de.vdwals.io.alpha2mqtt.services;

import static de.vdw.it.hamqtt.devices.Units.KILO_WATT_PER_HOUR;
import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import static de.vdw.it.hamqtt.devices.Units.WATT;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.measurement;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total_increasing;
import static de.vdwals.io.alpha2mqtt.utils.IdUtils.getUniqueId;

import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import de.vdwals.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdwals.io.alpha2mqtt.utils.IdUtils;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
@Singleton
public class BatteryDeviceService {

  @Getter
  private Sensor batteryLoad, batteryEnergy, pvPower, gridPower, powerConsumption, selfConsumption,
      selfSufficiency, carbonNum, pvToday, pvTotal, treeNum;

  private Device battery;

  public List<Device> getBatteryDevices() {
    log.info("Load and init batteries");
    List<DeviceInformation> deviceInformationList = Base.withDb(() -> AlphaEssBattery.findAll()
        .stream()
        .map(battery -> (AlphaEssBattery) battery)
        .map(battery -> {
          return DeviceInformation.builder()
              .manufacturer("Alpha ESS")
              .model("Smile5")
              .name(battery.getSn())
              .identifiers(List.of(battery.getSn()))
              .build();
        })
        .collect(Collectors.toList()));

    return getDevices(deviceInformationList);
  }

  private List<Device> getDevices(List<DeviceInformation> deviceInformationList) {
    log.info("Create sensors");
    return deviceInformationList.stream().map(deviceInformation -> {
      String deviceId = IdUtils.getDeviceId(deviceInformation);

      battery = new Device(deviceId, deviceInformation);

      batteryLoad = getMeasurementSensor(deviceInformation,
          deviceId,
          DeviceClass.battery,
          "soc",
          "Batterie Ladung").build();
      battery.addEntity(batteryLoad);

      batteryEnergy = getEnergySensor(deviceInformation, deviceId, "pBat", "Batterie Leistung");
      battery.addEntity(batteryEnergy);

      pvPower = getEnergySensor(deviceInformation, deviceId, "ppvTotal", "PV-Leistung");
      battery.addEntity(pvPower);

      gridPower = getEnergySensor(deviceInformation, deviceId, "gridPower", "Netz-Leistung");
      battery.addEntity(gridPower);

      powerConsumption =
          getEnergySensor(deviceInformation, deviceId, "powerConsumption", "Verbraucher Leistung");
      battery.addEntity(powerConsumption);

      carbonNum = getSensor(deviceInformation,
          deviceId,
          DeviceClass.carbon_dioxide,
          "carbonNum",
          "CO2 Einsparung").unitOfMeasurement("kg").stateClass(total_increasing).build();
      battery.addEntity(carbonNum);

      pvToday = getPowerSensor(deviceInformation,
          deviceId,
          "pvToday",
          "PV Energy Heute").stateClass(total)
          .lastResetValueTemplate("{{ value_json.start_of_day }}")
          .build();
      battery.addEntity(pvToday);

      pvTotal = getPowerSensor(deviceInformation, deviceId, "pvTotal", "PV Energy").stateClass(
          total_increasing).build();
      battery.addEntity(pvTotal);

      selfConsumption = getPercentSensor(deviceInformation,
          deviceId,
          "selfConsumption",
          "PV Energie Eigenverbrauch");
      battery.addEntity(selfConsumption);

      selfSufficiency =
          getPercentSensor(deviceInformation, deviceId, "selfSufficiency", "Autarkie");
      battery.addEntity(selfSufficiency);

      treeNum = getSensor(deviceInformation,
          deviceId,
          DeviceClass.None,
          "treeNum",
          "Gepflanzte Bäume").stateClass(total_increasing).build();
      battery.addEntity(treeNum);

      return battery;
    }).collect(Collectors.toList());
  }

  private SensorBuilder getPowerSensor(DeviceInformation deviceInformation,
                                       String deviceId,
                                       String objectId,
                                       String name) {
    return getSensor(deviceInformation,
        deviceId,
        DeviceClass.power,
        objectId,
        name).unitOfMeasurement(KILO_WATT_PER_HOUR.getUnit());
  }

  private Sensor getPercentSensor(DeviceInformation deviceInformation,
                                  String deviceId,
                                  String objectId,
                                  String name) {
    return getMeasurementSensor(deviceInformation,
        deviceId,
        DeviceClass.None,
        objectId,
        name).unitOfMeasurement(PERCENT.getUnit()).build();
  }

  private Sensor getEnergySensor(DeviceInformation deviceInformation,
                                 String deviceId,
                                 String objectId,
                                 String name) {
    return getMeasurementSensor(deviceInformation,
        deviceId,
        DeviceClass.energy,
        objectId,
        name).unitOfMeasurement(WATT.getUnit()).build();
  }

  private SensorBuilder getMeasurementSensor(DeviceInformation deviceInformation,
                                             String deviceId,
                                             DeviceClass deviceClass,
                                             String id,
                                             String name) {
    return getSensor(deviceInformation, deviceId, deviceClass, id, name).stateClass(measurement);
  }

  private SensorBuilder getSensor(DeviceInformation deviceInformation,
                                  String deviceId,
                                  DeviceClass deviceClass,
                                  String id,
                                  String name) {
    return Sensor.builder()
        .deviceClass(deviceClass)
        .device(deviceInformation)
        .objectId(id)
        .uniqueId(getUniqueId(deviceId, id))
        .name(name);
  }

}
