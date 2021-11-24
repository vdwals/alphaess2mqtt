package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class BatteryDeviceService extends DeviceService {

  private final Sensor batteryLoad, batteryEnergy, batteryInput, batteryOutput;

  public BatteryDeviceService() {
    super(
        "Alpha ESS",
        "Smile5",
        "PV-Batterie",
        Base.withDb(
            () -> {
              log.info("Load and init batteries");

              return AlphaEssBattery.findAll().stream()
                  .map(battery -> (AlphaEssBattery) battery)
                  .map(AlphaEssBattery::getSn)
                  .findFirst()
                  .orElseThrow();
            }));

    batteryLoad = getMeasurementSensor(DeviceClass.battery, "soc", "Batterie Ladung").build();
    getDevice().addEntity(batteryLoad);

    batteryEnergy = getPowerSensor("pBat", "Batterie Leistung");
    batteryInput = getPowerSensor("pBatIn", "Batterie Lade-Leistung");
    batteryOutput = getPowerSensor("pBatOut", "Batterie Entlade-Leistung");
  }

  public void mapValues(RunningDataDto data) {
    batteryLoad.setValue(data.getSoc());

    double pBat = data.getPbat();
    batteryEnergy.setValue(pBat);
    batteryInput.setValue(pBat > 0 ? 0 : Math.abs(pBat));
    batteryOutput.setValue(pBat > 0 ? pBat : 0);
  }
}
