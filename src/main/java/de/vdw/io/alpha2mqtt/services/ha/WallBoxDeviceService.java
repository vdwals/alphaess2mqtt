package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.AbstractAvailabilityEntity;
import de.vdw.it.hamqtt.devices.AbstractCommandEntity;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.BinarySensor;
import de.vdw.it.hamqtt.devices.entities.Select;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import de.vdw.it.hamqtt.devices.entities.Switch;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Payload.OFF;
import static de.vdw.it.hamqtt.devices.Payload.ON;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class WallBoxDeviceService extends DeviceService {

  AbstractEntity chargeEnergy, chargePower, chargeState, plugState, pluggedCarState;
  AbstractCommandEntity charger, chargerMode;

  public WallBoxDeviceService() {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", "ALP2021040257071");

    chargePower = getPowerSensor("chargePower", "Ladeleistung");

    chargeEnergy =
        getEnergySensor("chargeEnergy", "Energie geladen")
            .stateClass(Sensor.StateClass.total_increasing)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .build();
    getDevice().addEntity(chargeEnergy);

    chargeState =
        BinarySensor.builder()
            .device(getDevice())
            .name("Ladestatus")
            .objectId("charging")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "charging"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.battery_charging)
            .build();
    getDevice().addEntity(chargeState);

    plugState =
        BinarySensor.builder()
            .device(getDevice())
            .name("Steckerzustand")
            .objectId("plug")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "plug"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.plug)
            .build();
    getDevice().addEntity(plugState);

    pluggedCarState =
        BinarySensor.builder()
            .device(getDevice())
            .name("E-Auto Zustand")
            .objectId("ev_car")
            .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "ev_car"))
            .expireAfter(TimeUnit.SECONDS.toSeconds(30))
            .forceUpdate(true)
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
            .deviceClass(BinarySensor.DeviceClass.connectivity)
            .icon("mdi:car-electric")
            .build();
    getDevice().addEntity(pluggedCarState);

    charger =
        Switch.builder()
            .device(getDevice())
            .name("Laden")
            .objectId("charger_switch")
            .uniqueId(getUniqueId(getDevice().getNodeId(), "charger_switch"))
            .icon("mdi:toggle-switch")
            .entityCategory(AbstractAvailabilityEntity.EntityCategory.config)
            .build();
    charger.setValue(OFF);
    getDevice().addEntity(charger);

    chargerMode =
        Select.builder()
            .device(getDevice())
            .name("Lademodus")
            .objectId("charger_mode")
            .uniqueId(getUniqueId(getDevice().getNodeId(), "charger_mode"))
            .option(ChargingService.ChargingMode.SLOW.name())
            .option(ChargingService.ChargingMode.NORMAL.name())
            .option(ChargingService.ChargingMode.FAST.name())
            .option(ChargingService.ChargingMode.MAX.name())
            .icon(("mdi:car-select"))
            .build();
    getDevice().addEntity(chargerMode);
  }

  @Override
  public boolean mapValues(RunningDataDto dataDto) {
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

    boolean anyChange = chargePower.setValue(wallBoxPower);
    anyChange |= chargeEnergy.setValue(dataDto.getEv1_chgenergy_real());

    // 1: Nicht angeschlossen
    // 2: Angeschlossen, nicht laden
    // 3: Laden
    // 4: Unzureichende Leisuntg
    // 5: Warten auf Antwort des E-Autos (EV)
    // 6:

    switch (dataDto.getEv1_mode()) {
      case 1:
        anyChange |= charger.setValue(OFF);
        anyChange |= chargeState.setValue(OFF);
        anyChange |= plugState.setValue(OFF);
        anyChange |= pluggedCarState.setValue(OFF);
        break;
      case 3:
        anyChange |= charger.setValue(ON);
        anyChange |= chargeState.setValue(ON);
        anyChange |= plugState.setValue(ON);
        anyChange |= pluggedCarState.setValue(ON);
        break;
      case 2:
      case 4:
      case 5:
        anyChange |= chargeState.setValue(OFF);
        anyChange |= plugState.setValue(ON);
        anyChange |= pluggedCarState.setValue(ON);
        break;
      case 6:
        anyChange |= chargeState.setValue(OFF);
        anyChange |= plugState.setValue(ON);
        anyChange |= pluggedCarState.setValue(OFF);
        break;
    }

    return anyChange;
  }

  @Override
  public boolean mapValues(SummeryDto dataDto) {
    return false;
  }

  public boolean mapValues(SystemDto dataDto) {
    ChargingService.ChargingMode chargingMode =
        ChargingService.ChargingMode.chargingModeByValue(dataDto.getChargingmode());

    if (chargingMode != null) {
      return chargerMode.setValue(chargingMode);
    }

    return false;
  }
}
