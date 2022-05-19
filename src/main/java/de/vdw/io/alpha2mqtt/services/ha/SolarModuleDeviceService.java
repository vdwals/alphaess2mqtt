package de.vdw.io.alpha2mqtt.services.ha;

import java.time.LocalDate;
import javax.inject.Singleton;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.entities.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.RawEntity;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Value;

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
        .stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", START_OF_DAY))
        .build();
    getDevice().addEntity(pvToday);

    pvTotal =
        getEnergySensor("pvTotal", "PV Energie Gesamt")
        .stateClass(Sensor.StateClass.total_increasing)
        .build();
    getDevice().addEntity(pvTotal);

    startOfToday =
        RawEntity.builder().objectId(START_OF_DAY).className(pvToday.getClassName()).build();
    getDevice().addEntity(startOfToday);
  }

  @Override
  public void mapValues(RunningDataDto data) {
    pvPower.setValue(data.getPpv1() + data.getPpv2() + data.getPmeter_dc());

    ppv1.setValue(data.getPpv1());
    ppv2.setValue(data.getPpv2());

    pMeterDc.setValue(data.getPmeter_dc());
  }

  @Override
  public boolean mapValues(SummeryDto data) {

    boolean anyChange = pvToday.setValue(data.getEpvtoday());
    anyChange |= pvTotal.setValue(data.getEpvtotal());

    anyChange |= startOfToday.setValue(LocalDate.now().atStartOfDay());

    return anyChange;
  }
}
