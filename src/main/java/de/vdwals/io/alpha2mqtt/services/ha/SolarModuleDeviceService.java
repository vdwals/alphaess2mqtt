package de.vdwals.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.KILO_WATT_PER_HOUR;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total_increasing;

import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import de.vdwals.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdwals.io.alpha2mqtt.models.api.SummeryDto;
import de.vdwals.io.alpha2mqtt.utils.IdUtils;
import java.time.LocalDate;
import java.util.List;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Value
public class SolarModuleDeviceService extends DeviceService {

  public static final String START_OF_DAY = "start_of_day";

  Sensor pvPower, ppv1, ppv2, ppv3, ppv4, pMeterDc, pvToday, pvTotal;

  Device solarModules;

  public SolarModuleDeviceService() {
    log.info("Init solar modules");
    DeviceInformation solarModuleDeviceInformation = DeviceInformation.builder()
        .manufacturer("Bauer Solar")
        .model("BS-6MHBB5-GG")
        .name("Solarmodule")
        .identifiers(List.of("BS-6MHBB5-GG"))
        .build();

    String deviceId = IdUtils.getDeviceId(solarModuleDeviceInformation);

    solarModules = new Device(deviceId, solarModuleDeviceInformation);

    pvPower = getPowerSensor(solarModuleDeviceInformation, deviceId, "ppvTotal", "PV Leistung");

    ppv1 = getPowerSensor(solarModuleDeviceInformation, deviceId, "ppv1", "PV 1 Leistung");

    ppv2 = getPowerSensor(solarModuleDeviceInformation, deviceId, "ppv2", "PV 2 Leistung");

    ppv3 = getPowerSensor(solarModuleDeviceInformation, deviceId, "ppv3", "PV 3 Leistung");

    ppv4 = getPowerSensor(solarModuleDeviceInformation, deviceId, "ppv4", "PV 4 Leistung");

    pMeterDc = getPowerSensor(solarModuleDeviceInformation, deviceId, "pMeterDc", "PV DC Leistung");

    solarModules.addEntity(pvPower);

    pvToday = getEnergySensor(solarModuleDeviceInformation,
        deviceId,
        "pvToday",
        "PV Energie Heute").stateClass(total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", START_OF_DAY))
        .build();
    solarModules.addEntity(pvToday);

    pvTotal = getEnergySensor(solarModuleDeviceInformation,
        deviceId,
        "pvTotal",
        "PV Energie Gesamt").stateClass(total_increasing).build();
    solarModules.addEntity(pvTotal);
  }

  private SensorBuilder getEnergySensor(DeviceInformation deviceInformation,
                                        String deviceId,
                                        String objectId,
                                        String name) {
    return getSensor(deviceInformation,
        deviceId,
        DeviceClass.energy,
        objectId,
        name).unitOfMeasurement(KILO_WATT_PER_HOUR.getUnit());
  }

  public void mapValues(RunningDataDto data) {
    solarModules.updateValue(pvPower.getObjectId(),
        data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4() + data.getPmeter_dc());

  }

  public void mapValues(SummeryDto data) {

    solarModules.updateValue(pvToday.getObjectId(), data.getEpvtoday());
    solarModules.updateValue(pvTotal.getObjectId(), data.getEpvtotal());
    solarModules.updateRawValue(pvToday.getClassName(),
        SolarModuleDeviceService.START_OF_DAY,
        LocalDate.now().atStartOfDay());
  }
}
