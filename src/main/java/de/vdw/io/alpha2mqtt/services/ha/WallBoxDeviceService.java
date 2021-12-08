package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.switches.Switch;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class WallBoxDeviceService extends DeviceService {

  AbstractEntity chargeEnergy, chargePower, switchCharge;

  public WallBoxDeviceService() {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", "ALP2021040257071");

    chargeEnergy =
        getEnergySensor("chargeEnergy", "Wallbox Energie geladen")
            .stateClass(Sensor.StateClass.total_increasing)
            .build();

    getDevice().addEntity(chargeEnergy);

    chargePower = getPowerSensor("chargePower", "Wallbox Ladeleistung");

    switchCharge =
        Switch.builder()
            .name("Starte Ladevorgang")
            .objectId("startCharging")
            .uniqueId(getUniqueId(getDevice().getNodeId(), "startCharging"))
            .build();
  }

  @Override
  public void mapValues(RunningDataDto dataDto) {
    double wallBoxPower = dataDto.getEv1_power();

    double totalAvailablePower =
        dataDto.getPmeter_l1()
            + dataDto.getPmeter_l2()
            + dataDto.getPmeter_l1()
            + dataDto.getPmeter_dc()
            + dataDto.getPpv1()
            + dataDto.getPpv2()
            + dataDto.getPbat();

    if (wallBoxPower > 0.0 && totalAvailablePower < wallBoxPower) {
      log.warn(
          "Wallbox power {} Wh exceeds total available power of {} Wh. Fixing by recalculating wallbox power",
          wallBoxPower,
          totalAvailablePower);

      wallBoxPower -= 2 * Math.abs(totalAvailablePower - wallBoxPower);
    }

    chargePower.setValue(wallBoxPower);
    chargeEnergy.setValue(dataDto.getEv1_chgenergy_real());
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
