package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import java.time.LocalDate;
import javax.inject.Singleton;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
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
public class InverterDeviceService extends DeviceService {

  AbstractEntity gridPower,
  gridPowerIn,
  gridPowerOut,
  powerConsumption,
  selfConsumption,
  selfSufficiency,
  carbonNum,
  treeNum,
  vGridPowerIn,
  vGridPowerOut, pvPower, ppv1, ppv2, pMeterDc, pvToday, pvTotal, startOfToday;

  public InverterDeviceService(BatteryDto battery) {
    super("Alpha ESS", battery.getMinv(), "PV-Wechselrichter", battery.getSys_sn());

    gridPower = getPowerSensor("gridPower", "Netz Leistung");

    powerConsumption = getPowerSensor("powerConsumption", "Verbraucher Leistung");

    selfConsumption = getPercentSensor("selfConsumption", "Anteil PV Energie Eigenverbrauch");

    selfSufficiency = getPercentSensor("selfSufficiency", "Autarkie");

    gridPowerIn = getPowerSensor("gridPowerIn", "Netzbezug");
    gridPowerOut = getPowerSensor("gridPowerOut", "Netzeinspeisung");

    vGridPowerIn = getPowerSensor("vGridPowerIn", "virtueller Netzbezug");
    vGridPowerOut = getPowerSensor("vGridPowerOut", "virtuelle Netzeinspeisung");

    treeNum = getNumberSensor("treeNum", "Gepflanzte Bäume", "mdi:forest", "Stk");

    carbonNum = getNumberSensor("carbonNum", "CO2 Einsparung", "mdi:molecule-co2", "kg");

    pvPower = getPowerSensor("ppvTotal", "PV Leistung");

    ppv1 = getPowerSensor("ppv1", "PV 1 Leistung");

    ppv2 = getPowerSensor("ppv2", "PV 2 Leistung");

    pMeterDc = getPowerSensor("pMeterDc", "PV SUN2000 Leistung");

    pvToday = getEnergySensor("pvToday", "PV Energie Heute").stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", Constants.START_OF_DAY))
        .build();
    getDevice().addEntity(pvToday);

    pvTotal = getEnergySensor("pvTotal", "PV Energie Gesamt")
        .stateClass(Sensor.StateClass.total_increasing).build();
    getDevice().addEntity(pvTotal);

    startOfToday =
        RawEntity.builder().objectId(Constants.START_OF_DAY).className(pvToday.getClassName())
        .build();
    getDevice().addEntity(startOfToday);
  }

  private Sensor getNumberSensor(String id, String name, String icon, String unitOfMeasurement) {
    Sensor s =
        Sensor.builder()
        .objectId(id)
        .uniqueId(getUniqueId(getDevice().getNodeId(), id))
        .name(name)
        .icon(icon)
        .unitOfMeasurement(unitOfMeasurement)
        .build();

    getDevice().addEntity(s);
    return s;
  }

  private Sensor getPercentSensor(String objectId, String name) {
    Sensor s = getSensor(null, objectId, name).unitOfMeasurement(PERCENT.getUnit()).build();
    getDevice().addEntity(s);
    return s;
  }

  @Override
  public void mapValues(RunningDataDto data) {
    double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();

    gridPower.setValue(totalGridPower);
    powerConsumption.setValue(
        totalGridPower + data.getPpv1() + data.getPpv2() + data.getPmeter_dc() + data.getPbat());

    double gridIn = totalGridPower < 0 ? 0 : totalGridPower;
    gridPowerIn.setValue(gridIn);
    double gridOut = totalGridPower < 0 ? Math.abs(totalGridPower) : 0;
    gridPowerOut.setValue(gridOut);

    double pBat = data.getPbat();
    double batteryIn = pBat > 0 ? 0 : Math.abs(pBat);
    vGridPowerOut.setValue(batteryIn + gridOut);
    double batteryOut = pBat > 0 ? pBat : 0;
    vGridPowerIn.setValue(batteryOut + gridIn);

    pvPower.setValue(data.getPpv1() + data.getPpv2() + data.getPmeter_dc());

    ppv1.setValue(data.getPpv1());
    ppv2.setValue(data.getPpv2());

    pMeterDc.setValue(data.getPmeter_dc());
  }

  @Override
  public boolean mapValues(SummeryDto data) {
    boolean anyChange = carbonNum.setValue(data.getCarbonNum());
    anyChange |= selfConsumption.setValue(getScaledValue(data.getEselfConsumption() * 100));
    anyChange |= selfSufficiency.setValue(getScaledValue(data.getEselfSufficiency() * 100));
    anyChange |= treeNum.setValue(getScaledValue(data.getTreeNum()));
    anyChange |= pvToday.setValue(data.getEpvtoday());
    anyChange |= pvTotal.setValue(data.getEpvtotal());
    anyChange |= startOfToday.setValue(LocalDate.now().atStartOfDay());
    return anyChange;
  }
}
