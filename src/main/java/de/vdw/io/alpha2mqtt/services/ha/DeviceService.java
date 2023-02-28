package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.KILO_WATT_PER_HOUR;
import static de.vdw.it.hamqtt.devices.Units.WATT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.Device.DeviceBuilder;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity.EntityCategory;
import de.vdw.it.hamqtt.devices.entities.AbstractSensorEntity;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import de.vdw.it.hamqtt.devices.entities.Sensor.SensorBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
/**
 * Parent class of services holding devices. Interface for updating mqtt values.
 *
 * @author Dennis van der Wals
 *
 */
public abstract class DeviceService {
  @Getter
  private final Device device;

  protected DeviceService(String manufacturer, String model, String name, String... identifier) {
    log.info("Create device");


    DeviceBuilder deviceBuilder = Device.builder().manufacturer(manufacturer).model(model)
        .name(manufacturer + " " + name).nodeId(IdUtils.getDeviceId(manufacturer, model, name));

    for (String id : identifier) {
      if (StringUtils.isNotBlank(id)) {
        deviceBuilder.identifier(id);
      }
    }

    this.device = deviceBuilder.build();
  }

  protected Sensor getDailyEnergySensor(String objectId, String name, String nodeId) {
    Sensor sensor = getEnergySensor(objectId, name).stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", Constants.START_OF_DAY))
        .build();

    if (nodeId != null) {
      getDevice().addEntity(sensor, nodeId);
    } else {
      getDevice().addEntity(sensor);
    }

    return sensor;
  }

  protected Sensor.SensorBuilder getEnergySensor(String objectId, String name) {
    return getSensor(Sensor.DeviceClass.energy, objectId, name)
        .unitOfMeasurement(KILO_WATT_PER_HOUR.getUnit());
  }

  protected Sensor.SensorBuilder getMeasurementSensor(Sensor.DeviceClass deviceClass, String id,
      String name) {
    return getSensor(deviceClass, id, name).stateClass(Sensor.StateClass.measurement);
  }

  protected Sensor getNumberSensor(String id, String name, String icon, String unitOfMeasurement,
      EntityCategory entityCategory) {
    SensorBuilder sensorBuilder =
        Sensor.builder().objectId(id).uniqueId(getUniqueId(getDevice().getNodeId(), id)).name(name)
            .icon(icon).unitOfMeasurement(unitOfMeasurement);

    if (entityCategory != null) {
      sensorBuilder.entityCategory(entityCategory);
    }

    Sensor s = sensorBuilder.build();

    getDevice().addEntity(s);
    return s;
  }

  protected AbstractSensorEntity getPowerSensor(String objectId, String name) {
    AbstractSensorEntity s = getMeasurementSensor(Sensor.DeviceClass.power, objectId, name)
        .unitOfMeasurement(WATT.getUnit()).forceUpdate(true).expireAfter(Constants.EXPIRE).build();

    getDevice().addEntity(s);

    return s;
  }

  protected BigDecimal getScaledValue(double value) {
    return NumberUtils.toScaledBigDecimal(value, 3, RoundingMode.HALF_UP);
  }

  protected Sensor.SensorBuilder getSensor(Sensor.DeviceClass deviceClass, String id, String name) {
    return Sensor.builder().deviceClass(deviceClass).objectId(id)
        .uniqueId(getUniqueId(this.device.getNodeId(), id)).name(name);
  }

  protected Sensor.SensorBuilder getSensor(String id, String name) {
    return Sensor.builder().objectId(id).uniqueId(getUniqueId(this.device.getNodeId(), id))
        .name(name);
  }

  /**
   * Method to map dto values to device entity values.
   *
   * @param dataDto source dto
   * @return true if values changes, false otherwise
   */
  public abstract boolean mapValues(PowerDataDto dataDto);

  /**
   * Method to map dto values to device entity values.
   *
   * @param dataDto source dto
   * @return true if values changes, false otherwise
   */
  public abstract boolean mapValues(SummeryDto dataDto);

  /**
   * Method to map dto values to device entity values.
   *
   * @param dataDto source dto
   * @return true if values changes, false otherwise
   */
  public abstract boolean mapValues(SystemDto dataDto);
}
