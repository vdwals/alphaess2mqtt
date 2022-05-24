package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.WATT_PER_HOUR;
import javax.inject.Singleton;
import org.apache.commons.lang3.math.NumberUtils;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.Units;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity.EntityCategory;
import de.vdw.it.hamqtt.devices.entities.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.Number;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
public class BatteryDeviceService extends DeviceService {
  AbstractEntity batteryLoad, batteryEnergy, batteryInput, batteryOutput, batteryLoadEnergy,
      useCapacity, systemStatus, updateInterval, cobat, surpluscobat;

  double capacity;

  @Getter
  double frequency;

  public BatteryDeviceService(BatteryDto battery) {
    super("Alpha ESS", battery.getMbat(), "PV-Batterie", battery.getSys_sn(),
        battery.getSys_name());

    capacity = battery.getUscapacity();
    frequency = battery.getTrans_frequency();

    batteryLoad = getMeasurementSensor(Sensor.DeviceClass.battery, "pv_soc", "Batterie Ladung")
        .unitOfMeasurement(Units.PERCENT.getUnit()).forceUpdate(true).build();
    getDevice().addEntity(batteryLoad);

    batteryEnergy = getPowerSensor("pBat", "Batterie Leistung");
    batteryInput = getPowerSensor("pBatIn", "Batterie Lade-Leistung");
    batteryOutput = getPowerSensor("pBatOut", "Batterie Entlade-Leistung");

    batteryLoadEnergy = getSensor(Sensor.DeviceClass.energy, "pBatLoad", "Batterie Ladung (Wh)")
        .unitOfMeasurement(WATT_PER_HOUR.getUnit()).stateClass(Sensor.StateClass.measurement)
        .build();
    getDevice().addEntity(batteryLoadEnergy);

    systemStatus = getSensor("PV Battery System Status", "bat_system_status")
        .value(battery.getEms_status()).build();
    getDevice().addEntity(systemStatus);

    updateInterval = getNumberSensor("bat_system_update_interval", "PV Data Update Interval",
        "mdi:battery-clock", "s", EntityCategory.diagnostic);
    updateInterval.setValue(battery.getTrans_frequency());

    useCapacity = Number.builder().max(100).min(1).device(getDevice())
        .name("Batteriereserve für Notstrom").objectId("bat_use_cap")
        .uniqueId(IdUtils.getUniqueId(getDevice().getNodeId(), "bat_use_cap"))
        .entityCategory(AbstractAvailabilityEntity.EntityCategory.config).step(1).build();
    getDevice().addEntity(useCapacity);

    cobat = getNumberSensor("battery_total_capacity", "Total Battery Capacity", "mdi:battery",
        Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.system);
    cobat.setValue(battery.getCobat());

    surpluscobat = getNumberSensor("battery_usable_capacity", "Usable Battery Capacity",
        "mdi:battery", Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.system);
    surpluscobat.setValue(battery.getSurpluscobat());
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
  public boolean mapValues(SummeryDto dataDto) {
    return false;
  }

  public boolean mapValues(SystemDto data) {
    if (!NumberUtils.isCreatable(data.getBat_use_cap())) {
      return false;
    }

    return useCapacity.setValue(NumberUtils.createNumber(data.getBat_use_cap()));
  }
}
