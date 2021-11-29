package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

import javax.inject.Singleton;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class WallboxDeviceService extends DeviceService {

  AbstractEntity chargeEnergy, chargePower;

  public WallboxDeviceService() {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", "ALP2021040257071");

    chargeEnergy =
        getEnergySensor("chargeEnergy", "Wallbox Energie geladen")
            .stateClass(Sensor.StateClass.total_increasing)
            .build();

    getDevice().addEntity(chargeEnergy);

    chargePower = getPowerSensor("chargePower", "Wallbox Ladeleistung");
  }

  @Override
  public void mapValues(RunningDataDto dataDto) {
    double wallboxPower = dataDto.getEv1_power();

    double totalAvailablePower =
        dataDto.getPmeter_l1()
            + dataDto.getPmeter_l2()
            + dataDto.getPmeter_l1()
            + dataDto.getPmeter_dc()
            + dataDto.getPpv1()
            + dataDto.getPpv2()
            + dataDto.getPbat();

    if (wallboxPower > 0.0 && totalAvailablePower <= wallboxPower) {
      log.warn(
          "Wallbox power {} Wh exceeds total available power of {} Wh. Fixing by recalculating wallbox power",
          wallboxPower,
          totalAvailablePower);

      wallboxPower -= 2 * Math.abs(totalAvailablePower - wallboxPower);
    }

    chargePower.setValue(wallboxPower);
    chargeEnergy.setValue(dataDto.getEv1_chgenergy_real());
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
