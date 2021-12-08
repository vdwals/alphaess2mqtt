package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.AbstractAvailabilityEntity;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.BinarySensor;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import de.vdw.it.hamqtt.devices.entities.Switch;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.entities.BinarySensor.Payload.OFF;
import static de.vdw.it.hamqtt.devices.entities.BinarySensor.Payload.ON;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class WallBoxDeviceService extends DeviceService {

  AbstractEntity chargeEnergy, chargePower, chargeState, plugState, pluggedCarState;

  @Getter Switch charger;

  public WallBoxDeviceService() {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", "ALP2021040257071");

    chargeEnergy =
        getEnergySensor("chargeEnergy", "Wallbox Energie geladen")
            .stateClass(Sensor.StateClass.total_increasing)
            .build();

    getDevice().addEntity(chargeEnergy);

    chargePower = getPowerSensor("chargePower", "Wallbox Ladeleistung");

    chargeState =
        BinarySensor.builder()
            .device(getDevice())
            .name("Wallbox lädt")
            .objectId("charging")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "charging"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.battery_charging)
            .build();

    plugState =
        BinarySensor.builder()
            .device(getDevice())
            .name("Wallbox Stecker")
            .objectId("plug")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "plug"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.plug)
            .build();

    pluggedCarState =
        BinarySensor.builder()
            .device(getDevice())
            .name("E-Auto")
            .objectId("ev_car")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "ev_car"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.connectivity)
            .build();

    charger =
        Switch.builder()
            .device(getDevice())
            .name("charger")
            .objectId("alphaCharger")
            .uniqueId(getUniqueId(getDevice().getNodeId(), "alphaCharger"))
            .build();

    charger.setValue(OFF);
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

    // 1: Nicht angeschlossen
    // 2:
    // 3: Angeschlossen, nicht laden
    // 4:
    // 5: Warten auf Antwort des E-Autos (EV)
    // 6: Lädt

    switch (dataDto.getEv1_mode()) {
      case 1:
        charger.setValue(OFF);
        chargeState.setValue(OFF);
        plugState.setValue(OFF);
        pluggedCarState.setValue(OFF);
        break;
      case 4:
      case 2:
        break;
      case 3:
        chargeState.setValue(OFF);
        plugState.setValue(ON);
        pluggedCarState.setValue(OFF);
        break;
      case 5:
        chargeState.setValue(OFF);
        plugState.setValue(ON);
        pluggedCarState.setValue(ON);
        break;
      case 6:
        chargeState.setValue(ON);
        plugState.setValue(ON);
        pluggedCarState.setValue(ON);
        break;
    }
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
