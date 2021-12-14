package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.AbstractSensorEntity;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.KILO_WATT_PER_HOUR;
import static de.vdw.it.hamqtt.devices.Units.WATT;

@SuppressWarnings("rawtypes")
@Slf4j
public abstract class DeviceService {
  @Getter private final Device device;

  protected DeviceService(String manufacturer, String model, String name, String identifier) {
    log.info("Create device");

    this.device =
        Device.builder()
            .manufacturer(manufacturer)
            .model(model)
            .name(name)
            .identifier(identifier)
            .nodeId(IdUtils.getDeviceId(manufacturer, model, name))
            .build();
  }

  protected AbstractSensorEntity getPowerSensor(String objectId, String name) {
    AbstractSensorEntity s =
        getMeasurementSensor(Sensor.DeviceClass.power, objectId, name)
            .unitOfMeasurement(WATT.getUnit())
            .forceUpdate(true)
            .expireAfter(Constants.TIMEOUT)
            .build();

    getDevice().addEntity(s);

    return s;
  }

  protected Sensor.SensorBuilder getMeasurementSensor(
      Sensor.DeviceClass deviceClass, String id, String name) {
    return getSensor(deviceClass, id, name).stateClass(Sensor.StateClass.measurement);
  }

  protected Sensor.SensorBuilder getSensor(Sensor.DeviceClass deviceClass, String id, String name) {
    return Sensor.builder()
        .deviceClass(deviceClass)
        .objectId(id)
        .uniqueId(getUniqueId(device.getNodeId(), id))
        .name(name);
  }

  protected BigDecimal getScaledValue(double value) {
    return NumberUtils.toScaledBigDecimal(value, 3, RoundingMode.HALF_UP);
  }

  protected Sensor.SensorBuilder getEnergySensor(String objectId, String name) {
    return getSensor(Sensor.DeviceClass.energy, objectId, name)
        .unitOfMeasurement(KILO_WATT_PER_HOUR.getUnit());
  }

  public abstract void mapValues(RunningDataDto dataDto);

  public abstract boolean mapValues(SummeryDto dataDto);
}
