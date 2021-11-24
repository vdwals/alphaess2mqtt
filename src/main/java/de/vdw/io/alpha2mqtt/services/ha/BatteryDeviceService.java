package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
@Singleton
@Value
public class BatteryDeviceService extends DeviceService {

  AbstractEntity batteryLoad, batteryEnergy, batteryInput, batteryOutput, batteryLoadEnergy;

  double capacity;

  public BatteryDeviceService() {
    super("Alpha ESS", "Smile5", "PV-Batterie", getBattery().getSn());

    capacity = getBattery().getUsableCapacity();

    batteryLoad = getMeasurementSensor(DeviceClass.battery, "soc", "Batterie Ladung %").build();
    getDevice().addEntity(batteryLoad);

    batteryEnergy = getPowerSensor("pBat", "Batterie Leistung");
    batteryInput = getPowerSensor("pBatIn", "Batterie Lade-Leistung");
    batteryOutput = getPowerSensor("pBatOut", "Batterie Entlade-Leistung");

    batteryLoadEnergy =
        getEnergySensor("pBatLoad", "Batterie Ladung (kWh)")
            .stateClass(Sensor.StateClass.measurement)
            .build();
  }

  private static AlphaEssBattery getBattery() {
    return Base.withDb(
        () -> {
          log.info("Load and init batteries");

          return AlphaEssBattery.findAll().stream()
              .map(batteryModel -> (AlphaEssBattery) batteryModel)
              .findFirst()
              .orElseThrow();
        });
  }

  @Override
  public void mapValues(RunningDataDto data) {
    batteryLoad.setValue(data.getSoc());

    double pBat = data.getPbat();
    batteryEnergy.setValue(pBat);
    batteryInput.setValue(pBat > 0 ? 0 : Math.abs(pBat));
    batteryOutput.setValue(pBat > 0 ? pBat : 0);

    batteryEnergy.setValue(getScaledValue(data.getSoc() * capacity));
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
