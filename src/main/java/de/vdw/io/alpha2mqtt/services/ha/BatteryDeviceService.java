package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

import javax.inject.Singleton;

import static de.vdw.it.hamqtt.devices.Units.WATT_PER_HOUR;

@Slf4j
@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
public class BatteryDeviceService extends DeviceService {

  AbstractEntity batteryLoad, batteryEnergy, batteryInput, batteryOutput, batteryLoadEnergy;

  double capacity;

  public BatteryDeviceService() {
    super("Alpha ESS", "Smile5", "PV-Batterie", getBattery().getSn());

    capacity = getBattery().getUsableCapacity();

    batteryLoad =
        getMeasurementSensor(Sensor.DeviceClass.battery, "soc", "Batterie Ladung %").build();
    getDevice().addEntity(batteryLoad);

    batteryEnergy = getPowerSensor("pBat", "Batterie Leistung");
    batteryInput = getPowerSensor("pBatIn", "Batterie Lade-Leistung");
    batteryOutput = getPowerSensor("pBatOut", "Batterie Entlade-Leistung");

    batteryLoadEnergy =
        getSensor(Sensor.DeviceClass.energy, "pBatLoad", "Batterie Ladung (Wh)")
            .unitOfMeasurement(WATT_PER_HOUR.getUnit())
            .stateClass(Sensor.StateClass.measurement)
            .build();
    getDevice().addEntity(batteryLoadEnergy);
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

    batteryLoadEnergy.setValue(getScaledValue(data.getSoc() * capacity * 1000 / 100));
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
