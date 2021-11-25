package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.raw.RawEntity;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.time.LocalDate;

import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total_increasing;

@Slf4j
@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
public class SolarModuleDeviceService extends DeviceService {

  public static final String START_OF_DAY = "start_of_day";

  AbstractEntity pvPower, ppv1, ppv2, pMeterDc, pvToday, pvTotal, startOfToday;

  public SolarModuleDeviceService() {
    super("Bauer Solar", "BS-6MHBB5-GG", "Solarmodule", "BS-6MHBB5-GG");

    pvPower = getPowerSensor("ppvTotal", "PV Leistung");

    ppv1 = getPowerSensor("ppv1", "PV 1 Leistung");

    ppv2 = getPowerSensor("ppv2", "PV 2 Leistung");

    pMeterDc = getPowerSensor("pMeterDc", "PV SUN2000 Leistung");

    pvToday =
        getEnergySensor("pvToday", "PV Energie Heute")
            .stateClass(total)
            .lastResetValueTemplate(String.format("{{ value_json.%s }}", START_OF_DAY))
            .build();
    getDevice().addEntity(pvToday);

    pvTotal = getEnergySensor("pvTotal", "PV Energie Gesamt").stateClass(total_increasing).build();
    getDevice().addEntity(pvTotal);

    startOfToday =
        RawEntity.builder().objectId(START_OF_DAY).className(pvToday.getClassName()).build();
  }

  @Override
  public void mapValues(RunningDataDto data) {
    pvPower.setValue(
        data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4() + data.getPmeter_dc());

    ppv1.setValue(data.getPpv1());
    ppv2.setValue(data.getPpv2());

    pMeterDc.setValue(data.getPmeter_dc());
  }

  @Override
  public void mapValues(SummeryDto data) {

    pvToday.setValue(data.getEpvtoday());
    pvTotal.setValue(data.getEpvtotal());

    startOfToday.setValue(LocalDate.now().atStartOfDay());
  }
}
