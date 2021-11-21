package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import java.util.List;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
@Singleton
public class BatteryDeviceService extends DeviceService {

  private final Sensor batteryLoad, batteryEnergy, batteryInput, batteryOutput;

  @Getter
  private final Device battery;

  public BatteryDeviceService() {
    log.info("Load and init batteries");
    DeviceInformation deviceInformation = Base.withDb(() -> AlphaEssBattery.findAll()
        .stream()
        .map(battery -> (AlphaEssBattery) battery)
        .map(battery -> DeviceInformation.builder()
            .manufacturer("Alpha ESS")
            .model("Smile5")
            .name("PV-Batterie")
            .identifiers(List.of(battery.getSn()))
            .build())
        .findFirst()).get();

    log.info("Create sensors");
    String deviceId = IdUtils.getDeviceId(deviceInformation);

    battery = new Device(deviceId, deviceInformation);

    batteryLoad = getMeasurementSensor(deviceInformation,
        deviceId,
        DeviceClass.battery,
        "soc",
        "Batterie Ladung").build();
    battery.addEntity(batteryLoad);

    batteryEnergy = getPowerSensor(deviceInformation, deviceId, "pBat", "Batterie Leistung");
    battery.addEntity(batteryEnergy);
    batteryInput = getPowerSensor(deviceInformation, deviceId, "pBatIn", "Batterie Lade-Leistung");
    battery.addEntity(batteryInput);
    batteryOutput =
        getPowerSensor(deviceInformation, deviceId, "pBatOut", "Batterie Entlade-Leistung");
    battery.addEntity(batteryOutput);
  }

  public void mapValues(RunningDataDto data) {
    battery.updateValue(batteryLoad.getObjectId(), data.getSoc());

    double pBat = data.getPbat();
    battery.updateValue(batteryEnergy.getObjectId(), pBat);
    battery.updateValue(batteryInput.getObjectId(), pBat > 0 ? 0 : Math.abs(pBat));
    battery.updateValue(batteryOutput.getObjectId(), pBat > 0 ? pBat : 0);
  }
}
