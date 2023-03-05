package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Payload.OFF;
import static de.vdw.it.hamqtt.devices.Payload.ON;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.ChargingPileId;
import de.vdw.io.alpha2mqtt.models.api.ChargingPileDto;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractCommandEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.BinarySensor;
import de.vdw.it.hamqtt.devices.entities.Select;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import de.vdw.it.hamqtt.devices.entities.Switch;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@EqualsAndHashCode(callSuper = true)
@Slf4j
/**
 * Class for charging pile device
 *
 * @author Dennis van der Wals
 *
 */
public class ChargingPileDeviceService extends DeviceService {

  AbstractEntity chargeEnergy, chargePower, chargeState, plugState, pluggedCarState;
  AbstractCommandEntity charger, chargerMode;

  ChargingPileId id;
  String sn;

  public ChargingPileDeviceService(ChargingPileDto wallbox) {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", wallbox.getChargingpile_sn(),
        wallbox.getChargingpile_id());

    id = ChargingPileId.valueOf(wallbox.getChargingpile_id().toUpperCase());
    sn = wallbox.getChargingpile_sn();

    this.chargePower = getPowerSensor("chargePower", "Ladeleistung");

    this.chargeEnergy = getEnergySensor("chargeEnergy", "Wallbox geladen")
        .stateClass(Sensor.StateClass.total_increasing)
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic).build();
    getDevice().addEntity(this.chargeEnergy);

    this.chargeState = buildBinarySensor("Ladestatus", "charging",
        BinarySensor.DeviceClass.battery_charging, null);

    this.plugState =
        buildBinarySensor("Steckerzustand", "plug", BinarySensor.DeviceClass.plug, null);

    this.pluggedCarState = buildBinarySensor("E-Auto Zustand", "ev_car",
        BinarySensor.DeviceClass.connectivity, "mdi:car-electric");

    this.charger = Switch.builder().device(getDevice()).name("Laden").objectId("charger_switch")
        .uniqueId(getUniqueId(getDevice().getNodeId(), "charger_switch")).icon("mdi:toggle-switch")
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.config).build();
    this.charger.setValue(OFF);
    getDevice().addEntity(this.charger);

    this.chargerMode = Select.builder().device(getDevice()).name("Lademodus")
        .objectId("charger_mode").uniqueId(getUniqueId(getDevice().getNodeId(), "charger_mode"))
        .option(ChargingService.ChargingMode.SLOW.name())
        .option(ChargingService.ChargingMode.NORMAL.name())
        .option(ChargingService.ChargingMode.FAST.name())
        .option(ChargingService.ChargingMode.MAX.name()).icon("mdi:car-select")
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.config).build();
    getDevice().addEntity(this.chargerMode);
  }

  private AbstractEntity buildBinarySensor(String name, String id,
      BinarySensor.DeviceClass deviceClass, String icon) {
    final AbstractEntity binarySensor = BinarySensor.builder().device(getDevice()).name(name)
        .objectId(id).uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), id))
        .expireAfter(Constants.EXPIRE).forceUpdate(true)
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.diagnostic)
        .deviceClass(deviceClass).icon(icon).build();
    getDevice().addEntity(binarySensor);
    return binarySensor;
  }

  public void mapValues(Integer mode) {
    // 1: Nicht angeschlossen
    // 2: Angeschlossen, nicht laden
    // 3: Laden
    // 4: Unzureichende Leisuntg
    // 5: Warten auf Antwort des E-Autos (EV)
    // 6:

    this.charger.setValue(OFF);
    this.chargeState.setValue(OFF);
    this.plugState.setValue(OFF);
    this.pluggedCarState.setValue(OFF);

    switch (mode) {
      case 1:
        break;
      case 3:
        this.charger.setValue(ON);
        this.chargeState.setValue(ON);
      case 2:
      case 4:
      case 5:
        this.pluggedCarState.setValue(ON);
      case 6:
        this.plugState.setValue(ON);
        break;
    }
  }

  @Override
  public boolean mapValues(PowerDataDto dataDto) {

    double wallBoxPower = switch (id) {
      case EV1 -> dataDto.getEv1_power();
      case EV2 -> dataDto.getEv2_power();
      default -> throw new IllegalArgumentException("Unexpected value: " + id);
    };

    double totalAvailablePower =
        dataDto.getPmeter_l1() + dataDto.getPmeter_l2() + dataDto.getPmeter_l3()
            + dataDto.getPmeter_dc() + dataDto.getPpv1() + dataDto.getPpv2() + dataDto.getPbat();

    if (totalAvailablePower > 0 && wallBoxPower > 0 && totalAvailablePower < wallBoxPower) {
      log.warn(
          "Wallbox power {} W exceeds total available power of {} W. Fixing by recalculating wallbox power",
          wallBoxPower, totalAvailablePower);

      wallBoxPower = Math.max(
          Math.min(wallBoxPower, wallBoxPower - 2 * Math.abs(totalAvailablePower - wallBoxPower)),
          0.0);
    }

    boolean anyChange = this.chargePower.setValue(wallBoxPower);

    if (wallBoxPower > 0) {
      anyChange |= this.charger.setValue(ON);
      anyChange |= this.chargeState.setValue(ON);
      anyChange |= this.plugState.setValue(ON);
      anyChange |= this.pluggedCarState.setValue(ON);
    }
    return anyChange;
  }

  @Override
  public boolean mapValues(SummeryDto dataDto) {
    return false;
  }

  @Override
  public boolean mapValues(SystemDto dataDto) {
    ChargingService.ChargingMode chargingMode =
        ChargingService.ChargingMode.chargingModeByValue(dataDto.getChargingmode());

    if (chargingMode != null) {
      return this.chargerMode.setValue(chargingMode);
    }

    return false;
  }
}
