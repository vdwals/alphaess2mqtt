package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.WATT;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.measurement;
import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;

import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.commons.lang3.math.NumberUtils;

public abstract class DeviceService {

  protected Sensor getPowerSensor(DeviceInformation deviceInformation,
                                  String deviceId,
                                  String objectId,
                                  String name) {
    return getMeasurementSensor(deviceInformation,
        deviceId,
        DeviceClass.power,
        objectId,
        name).unitOfMeasurement(WATT.getUnit()).build();
  }

  protected SensorBuilder getMeasurementSensor(DeviceInformation deviceInformation,
                                               String deviceId,
                                               DeviceClass deviceClass,
                                               String id,
                                               String name) {
    return getSensor(deviceInformation, deviceId, deviceClass, id, name).stateClass(measurement);
  }

  protected SensorBuilder getSensor(DeviceInformation deviceInformation,
                                    String deviceId,
                                    DeviceClass deviceClass,
                                    String id,
                                    String name) {
    return Sensor.builder()
        .deviceClass(deviceClass)
        .device(deviceInformation)
        .objectId(id)
        .uniqueId(getUniqueId(deviceId, id))
        .name(name);
  }

  protected BigDecimal getScaledValue(double value) {
    return NumberUtils.toScaledBigDecimal(value, 3, RoundingMode.HALF_UP);
  }
}
