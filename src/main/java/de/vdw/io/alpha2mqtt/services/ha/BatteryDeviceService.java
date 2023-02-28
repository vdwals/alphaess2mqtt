package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Payload.OFF;
import static de.vdw.it.hamqtt.devices.Units.WATT_PER_HOUR;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.Payload;
import de.vdw.it.hamqtt.devices.Units;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity.EntityCategory;
import de.vdw.it.hamqtt.devices.entities.AbstractCommandEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.Number;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import de.vdw.it.hamqtt.devices.entities.Switch;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Class for the Battery device with its attributes.
 *
 * @author Dennis van der Wals
 *
 */
public class BatteryDeviceService extends DeviceService {
  public static final Integer MIN_USV_CAPACITY = 4;
  public static final Integer MAX_USV_CAPACITY = 100;

  AbstractEntity batteryLoad, batteryEnergy, batteryInput, batteryOutput, batteryLoadEnergy,
      systemStatus, updateInterval, cobat, surpluscobat, todayCharge, todayDischarge;

  AbstractCommandEntity useCapacity, usvMode;

  double capacity;

  @Getter
  double frequency;

  public BatteryDeviceService(BatteryDto battery) {
    super("Alpha ESS", battery.getMbat(), "PV-Batterie",
        String.join("_", battery.getSys_sn(), battery.getMbat()), battery.getSys_name());

    this.capacity = battery.getSurpluscobat();
    this.frequency = battery.getTrans_frequency();

    this.batteryLoad = getMeasurementSensor(Sensor.DeviceClass.battery, "pv_soc", "Batterie Ladung")
        .unitOfMeasurement(Units.PERCENT.getUnit()).forceUpdate(true).build();
    getDevice().addEntity(this.batteryLoad);

    this.batteryEnergy = getPowerSensor("pBat", "Batterie Leistung");
    this.batteryInput = getPowerSensor("pBatIn", "Batterie Lade-Leistung");
    this.batteryOutput = getPowerSensor("pBatOut", "Batterie Entlade-Leistung");

    this.batteryLoadEnergy =
        getSensor(Sensor.DeviceClass.energy, "pBatLoad", "Batterie Ladung (Wh)")
            .unitOfMeasurement(WATT_PER_HOUR.getUnit()).stateClass(Sensor.StateClass.measurement)
            .build();
    getDevice().addEntity(this.batteryLoadEnergy);

    this.systemStatus = getSensor("bat_system_status", "PV Battery System Status")
        .value(battery.getEms_status()).build();
    getDevice().addEntity(this.systemStatus);

    this.updateInterval = getNumberSensor("bat_system_update_interval", "PV Data Update Interval",
        "mdi:battery-clock", "s", EntityCategory.diagnostic);
    this.updateInterval.setValue(battery.getTrans_frequency());

    this.useCapacity = Number.builder().max(MAX_USV_CAPACITY).min(MIN_USV_CAPACITY)
        .device(getDevice()).name("Batteriereserve für Notstrom").objectId("bat_use_cap")
        .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "bat_use_cap"))
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.config).step(1).build();
    getDevice().addEntity(this.useCapacity);

    this.cobat = getNumberSensor("battery_total_capacity", "Total Battery Capacity", "mdi:battery",
        Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    this.cobat.setValue(battery.getCobat());

    this.surpluscobat = getNumberSensor("battery_usable_capacity", "Usable Battery Capacity",
        "mdi:battery", Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    this.surpluscobat.setValue(battery.getSurpluscobat());

    this.usvMode = Switch.builder().device(getDevice()).name("USV-Mode").objectId("usv_mode_switch")
        .uniqueId(getUniqueId(getDevice().getNodeId(), "usv_mode_switch")).icon("mdi:toggle-switch")
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.config).build();
    this.usvMode.setValue(OFF);
    getDevice().addEntity(this.usvMode);

    this.todayCharge = getDailyEnergySensor("Echarge", "Gespeicherte Energiemenge", null);

    this.todayDischarge = getDailyEnergySensor("EDischarge", "Entnommene Energiemenge", null);
  }

  @Override
  public boolean mapValues(PowerDataDto data) {
    boolean anyChange = this.batteryLoad.setValue(data.getSoc());

    double pBat = data.getPbat();
    anyChange |= this.batteryEnergy.setValue(pBat);
    anyChange |= this.batteryInput.setValue(pBat > 0 ? 0 : Math.abs(pBat));
    anyChange |= this.batteryOutput.setValue(pBat > 0 ? pBat : 0);

    return anyChange
        || this.batteryLoadEnergy.setValue(getScaledValue(data.getSoc() * this.capacity * 10));
  }

  @Override
  public boolean mapValues(SummeryDto data) {
    boolean anyChange = this.todayCharge.setValue(data.getEcharge());
    anyChange |= this.todayDischarge.setValue(data.getEDisCharge());

    return anyChange;
  }

  @Override
  public boolean mapValues(SystemDto data) {
    boolean anyChange = this.useCapacity.setValue(data.getBat_use_cap());

    if (data.getUpsReserve() == 1) {
      anyChange |= this.usvMode.setValue(Payload.ON);
    } else {
      anyChange |= this.usvMode.setValue(OFF);
    }

    return anyChange;
  }
}
