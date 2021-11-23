package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.WATT;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.measurement;

@Slf4j
public abstract class DeviceService {
    @Getter
    private final Device device;
    
    protected DeviceService(DeviceInformation deviceInformation) {
        log.info("Create sensors");
        String deviceId = IdUtils.getDeviceId(deviceInformation);
        
        device = new Device(deviceId, deviceInformation);
    }
    
    protected Sensor getPowerSensor(
            String objectId,
            String name) {
        Sensor s = getMeasurementSensor(
                DeviceClass.power,
                objectId,
                name).unitOfMeasurement(WATT.getUnit()).build();
        
        getDevice().addEntity(s);
        
        return s;
    }
    
    protected SensorBuilder getMeasurementSensor(
            DeviceClass deviceClass,
            String id,
            String name) {
        return getSensor(deviceClass, id, name).stateClass(measurement);
    }
    
    protected SensorBuilder getSensor(DeviceClass deviceClass,
                                      String id,
                                      String name) {
        return Sensor.builder()
                .deviceClass(deviceClass)
                .device(device.getDeviceInformation())
                .objectId(id)
                .uniqueId(getUniqueId(device.getNodeId(), id))
                .name(name);
    }
    
    protected BigDecimal getScaledValue(double value) {
        return NumberUtils.toScaledBigDecimal(value, 3, RoundingMode.HALF_UP);
    }
}
