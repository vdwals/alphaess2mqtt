package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import static de.vdw.it.hamqtt.devices.Units.WATT;
import java.time.LocalDate;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import de.vdw.it.hamqtt.devices.Units;
import de.vdw.it.hamqtt.devices.entities.AbstractAvailabilityEntity.EntityCategory;
import de.vdw.it.hamqtt.devices.entities.AbstractEntity;
import de.vdw.it.hamqtt.devices.entities.AbstractSensorEntity;
import de.vdw.it.hamqtt.devices.entities.RawEntity;
import de.vdw.it.hamqtt.devices.entities.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Class for inverter device and its entities.
 *
 * @author Dennis van der Wals
 *
 */
public class InverterDeviceService extends DeviceService {

  AbstractEntity gridPower, gridPowerIn, gridPowerOut, powerConsumption, selfConsumption,
      selfSufficiency, carbonNum, treeNum, vGridPowerIn, vGridPowerOut, pvPower, ppv1, ppv2,
      pMeterDc, pvToday, pvTotal, startOfToday, popv, poinv, todayCharge, todayDischarge,
      todayIncome, totalIncome;

  public InverterDeviceService(BatteryDto battery) {
    super("Alpha ESS", battery.getMinv(), "PV-Wechselrichter",
        String.join("_", battery.getSys_sn(), battery.getMinv()));

    String nodeIdCurrent =
        IdUtils.getDeviceId("Alpha ESS", battery.getMinv(), "PV-Wechselrichter", "2");

    gridPower = getPowerSensor("gridPower", "Netz Leistung", nodeIdCurrent);

    powerConsumption = getPowerSensor("powerConsumption", "Verbraucher Leistung", nodeIdCurrent);

    selfConsumption = getPercentSensor("selfConsumption", "Anteil PV Energie Eigenverbrauch");

    selfSufficiency = getPercentSensor("selfSufficiency", "Autarkie");

    gridPowerIn = getPowerSensor("gridPowerIn", "Netzbezug", nodeIdCurrent);
    gridPowerOut = getPowerSensor("gridPowerOut", "Netzeinspeisung", nodeIdCurrent);

    vGridPowerIn = getPowerSensor("vGridPowerIn", "virtueller Netzbezug", nodeIdCurrent);
    vGridPowerOut = getPowerSensor("vGridPowerOut", "virtuelle Netzeinspeisung", nodeIdCurrent);

    treeNum = getNumberSensor("treeNum", "Gepflanzte Bäume", "mdi:forest", "Stk", null);

    carbonNum = getNumberSensor("carbonNum", "CO2 Einsparung", "mdi:molecule-co2", "kg", null);

    todayCharge =
        getNumberSensor("Echarge", "Geladene Energiemenge", "mdi:battery-arrow-up", "kWh", null);

    todayDischarge = getNumberSensor("EDischarge", "Entladene Energiemenge",
        "mdi:battery-arrow-down-outline", "kWh", null);

    todayIncome = getNumberSensor("TodayIncome", "Einnahmen heute", "mdi:cash-100", "€", null);

    totalIncome = getNumberSensor("TotalIncome", "Einnahmen gesamt", "mdi:cash-100", "€", null);


    poinv = getNumberSensor("power_output_inverter", "Power Output Inverter", "mdi:power",
        Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    poinv.setValue(battery.getPoinv());

    popv = getNumberSensor("power_output_solar_modules", "Power Output Solar Modules", "mdi:power",
        Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    popv.setValue(battery.getPopv());

    pvPower = getPowerSensor("ppvTotal", "PV Leistung", nodeIdCurrent);

    ppv1 = getPowerSensor("ppv1", "PV 1 Leistung", nodeIdCurrent);

    ppv2 = getPowerSensor("ppv2", "PV 2 Leistung", nodeIdCurrent);

    pMeterDc = getPowerSensor("pMeterDc", "PV SUN2000 Leistung", nodeIdCurrent);

    pvToday = getEnergySensor("pvToday", "PV Energie Heute").stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", Constants.START_OF_DAY))
        .build();
    getDevice().addEntity(pvToday);

    pvTotal = getEnergySensor("pvTotal", "PV Energie Gesamt")
        .stateClass(Sensor.StateClass.total_increasing).build();
    getDevice().addEntity(pvTotal);

    startOfToday = RawEntity.builder().objectId(Constants.START_OF_DAY)
        .className(pvToday.getClassName()).build();
    getDevice().addEntity(startOfToday);
  }

  private Sensor getPercentSensor(String objectId, String name) {
    Sensor s = getSensor(null, objectId, name).unitOfMeasurement(PERCENT.getUnit()).build();
    getDevice().addEntity(s);
    return s;
  }

  private AbstractSensorEntity getPowerSensor(String objectId, String name, String nodeId) {
    AbstractSensorEntity s = getMeasurementSensor(Sensor.DeviceClass.power, objectId, name)
        .unitOfMeasurement(WATT.getUnit()).forceUpdate(true).expireAfter(Constants.EXPIRE).build();

    getDevice().addEntity(s, nodeId);

    return s;
  }

  @Override
  public boolean mapValues(PowerDataDto data) {
    double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();

    boolean anyChange = gridPower.setValue(totalGridPower);
    anyChange |= powerConsumption.setValue(
        totalGridPower + data.getPpv1() + data.getPpv2() + data.getPmeter_dc() + data.getPbat());

    double gridIn = totalGridPower < 0 ? 0 : totalGridPower;
    anyChange |= gridPowerIn.setValue(gridIn);
    double gridOut = totalGridPower < 0 ? Math.abs(totalGridPower) : 0;
    anyChange |= gridPowerOut.setValue(gridOut);

    double pBat = data.getPbat();
    double batteryIn = pBat > 0 ? 0 : Math.abs(pBat);
    anyChange |= vGridPowerOut.setValue(batteryIn + gridOut);
    double batteryOut = pBat > 0 ? pBat : 0;
    anyChange |= vGridPowerIn.setValue(batteryOut + gridIn);

    anyChange |= pvPower.setValue(data.getPpv1() + data.getPpv2() + data.getPmeter_dc());

    anyChange |= ppv1.setValue(data.getPpv1());
    anyChange |= ppv2.setValue(data.getPpv2());

    anyChange |= pMeterDc.setValue(data.getPmeter_dc());
    return anyChange;
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
    anyChange |= totalIncome.setValue(data.getTotalIncome());
    anyChange |= todayIncome.setValue(data.getTodayIncome());
    anyChange |= todayCharge.setValue(data.getEcharge());
    anyChange |= todayDischarge.setValue(data.getEDisCharge());

    return anyChange;
  }

  @Override
  public boolean mapValues(SystemDto dataDto) {
    return false;
  }
}
