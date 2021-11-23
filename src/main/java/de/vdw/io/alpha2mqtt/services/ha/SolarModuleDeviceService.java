package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.List;

import static de.vdw.it.hamqtt.devices.Units.KILO_WATT_PER_HOUR;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total_increasing;

@Slf4j
@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
public class SolarModuleDeviceService extends DeviceService {
  
  public static final String START_OF_DAY = "start_of_day";
  
  Sensor pvPower, ppv1, ppv2, ppv3, ppv4, pMeterDc, pvToday, pvTotal;
  
  public SolarModuleDeviceService() {
    super(DeviceInformation.builder()
            .manufacturer("Bauer Solar")
            .model("BS-6MHBB5-GG")
            .name("Solarmodule")
            .identifiers(List.of("BS-6MHBB5-GG"))
            .build());
    
    pvPower = getPowerSensor("ppvTotal", "PV Leistung");
    
    ppv1 = getPowerSensor("ppv1", "PV 1 Leistung");
    
    ppv2 = getPowerSensor("ppv2", "PV 2 Leistung");
    
    ppv3 = getPowerSensor("ppv3", "PV 3 Leistung");
    
    ppv4 = getPowerSensor("ppv4", "PV 4 Leistung");
    
    pMeterDc = getPowerSensor("pMeterDc", "PV SUN2000 Leistung");
    
    pvToday = getEnergySensor(
            "pvToday",
            "PV Energie Heute").stateClass(total)
            .lastResetValueTemplate(String.format("{{ value_json.%s }}", START_OF_DAY))
            .build();
    getDevice().addEntity(pvToday);
    
    pvTotal =
            getEnergySensor("pvTotal", "PV Energie Gesamt").stateClass(
                    total_increasing).build();
    getDevice().addEntity(pvTotal);
  }
  
  private SensorBuilder getEnergySensor(
          String objectId,
          String name) {
    return getSensor(
            DeviceClass.energy,
            objectId,
            name).unitOfMeasurement(KILO_WATT_PER_HOUR.getUnit());
  }
  
  public void mapValues(RunningDataDto data) {
    getDevice().updateValue(pvPower.getObjectId(),
            data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4() + data.getPmeter_dc());
    
    getDevice().updateValue(ppv1.getObjectId(), data.getPpv1());
    getDevice().updateValue(ppv2.getObjectId(), data.getPpv2());
    getDevice().updateValue(ppv3.getObjectId(), data.getPpv3());
    getDevice().updateValue(ppv4.getObjectId(), data.getPpv4());
    
    getDevice().updateValue(pMeterDc.getObjectId(), data.getPmeter_dc());
  }
  
  public void mapValues(SummeryDto data) {
    
    getDevice().updateValue(pvToday.getObjectId(), data.getEpvtoday());
    getDevice().updateValue(pvTotal.getObjectId(), data.getEpvtotal());
    getDevice().updateRawValue(pvToday.getClassName(),
            SolarModuleDeviceService.START_OF_DAY,
            LocalDate.now().atStartOfDay());
  }
}
