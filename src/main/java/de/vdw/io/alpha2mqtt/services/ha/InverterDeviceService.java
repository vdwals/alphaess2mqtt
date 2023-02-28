package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import static de.vdw.it.hamqtt.devices.Units.WATT;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
      todayIncome, totalIncome, eload, einput, eoutput;

  public InverterDeviceService(BatteryDto battery) {
    super("Alpha ESS", battery.getMinv(), "PV-Wechselrichter",
        String.join("_", battery.getSys_sn(), battery.getMinv()));

    String nodeIdCurrent =
        IdUtils.getDeviceId("Alpha ESS", battery.getMinv(), "PV-Wechselrichter", "2");

    String nodeIdStats =
        IdUtils.getDeviceId("Alpha ESS", battery.getMinv(), "PV-Wechselrichter", "3");

    this.gridPower = getPowerSensor("gridPower", "Netz Leistung", nodeIdCurrent);

    this.powerConsumption =
        getPowerSensor("powerConsumption", "Verbraucher Leistung", nodeIdCurrent);

    this.selfConsumption = getPercentSensor("selfConsumption", "Anteil PV Energie Eigenverbrauch");

    this.selfSufficiency = getPercentSensor("selfSufficiency", "Autarkie");

    this.gridPowerIn = getPowerSensor("gridPowerIn", "Netzbezug", nodeIdCurrent);
    this.gridPowerOut = getPowerSensor("gridPowerOut", "Netzeinspeisung", nodeIdCurrent);

    this.vGridPowerIn = getPowerSensor("vGridPowerIn", "virtueller Netzbezug", nodeIdCurrent);
    this.vGridPowerOut =
        getPowerSensor("vGridPowerOut", "virtuelle Netzeinspeisung", nodeIdCurrent);

    this.treeNum = getNumberSensor("treeNum", "Gepflanzte Bäume", "mdi:forest", "Stk", null);

    this.carbonNum = getNumberSensor("carbonNum", "CO2 Einsparung", "mdi:molecule-co2", "kg", null);

    this.todayCharge = getDailyEnergySensor("Echarge", "Gespeicherte Energiemenge");

    this.todayDischarge = getDailyEnergySensor("EDischarge", "Entnommene Energiemenge");

    this.todayIncome = getSensor(Sensor.DeviceClass.monetary, "TodayIncome", "Einnahmen heute")
        .unitOfMeasurement("€").stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", Constants.START_OF_DAY))
        .build();
    getDevice().addEntity(this.todayIncome, nodeIdStats);

    this.totalIncome = getSensor(Sensor.DeviceClass.monetary, "TotalIncome", "Einnahmen gesamt")
        .unitOfMeasurement("€").stateClass(Sensor.StateClass.total_increasing).build();
    getDevice().addEntity(this.totalIncome, nodeIdStats);


    this.poinv = getNumberSensor("power_output_inverter", "Power Output Inverter", "mdi:power",
        Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    this.poinv.setValue(battery.getPoinv());

    this.popv = getNumberSensor("power_output_solar_modules", "Power Output Solar Modules",
        "mdi:power", Units.KILO_WATT_PER_HOUR.getUnit(), EntityCategory.diagnostic);
    this.popv.setValue(battery.getPopv());

    this.pvPower = getPowerSensor("ppvTotal", "PV Leistung", nodeIdCurrent);

    this.ppv1 = getPowerSensor("ppv1", "PV 1 Leistung", nodeIdCurrent);

    this.ppv2 = getPowerSensor("ppv2", "PV 2 Leistung", nodeIdCurrent);

    this.pMeterDc = getPowerSensor("pMeterDc", "PV SUN2000 Leistung", nodeIdCurrent);

    this.eload = getDailyEnergySensor("eload", "Geladene Energie");

    this.eoutput = getDailyEnergySensor("eoutput", "Eingespeiste Energie");

    this.einput = getDailyEnergySensor("einput", "Netzbezogene Energie");

    this.pvToday = getDailyEnergySensor("pvToday", "PV Energie Heute");

    this.pvTotal = getDailyEnergySensor("pvTotal", "PV Energie Gesamt");

    this.startOfToday = RawEntity.builder().objectId(Constants.START_OF_DAY)
        .className(this.pvToday.getClassName()).build();
    getDevice().addEntity(this.startOfToday);
  }

  private Sensor getDailyEnergySensor(String objectId, String name) {
    Sensor sensor = getEnergySensor(objectId, name).stateClass(Sensor.StateClass.total)
        .lastResetValueTemplate(String.format("{{ value_json.%s }}", Constants.START_OF_DAY))
        .build();
    getDevice().addEntity(sensor);

    return sensor;
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

    boolean anyChange = this.gridPower.setValue(totalGridPower);
    anyChange |= this.powerConsumption.setValue(
        totalGridPower + data.getPpv1() + data.getPpv2() + data.getPmeter_dc() + data.getPbat());

    double gridIn = totalGridPower < 0 ? 0 : totalGridPower;
    anyChange |= this.gridPowerIn.setValue(gridIn);
    double gridOut = totalGridPower < 0 ? Math.abs(totalGridPower) : 0;
    anyChange |= this.gridPowerOut.setValue(gridOut);

    double pBat = data.getPbat();
    double batteryIn = pBat > 0 ? 0 : Math.abs(pBat);
    anyChange |= this.vGridPowerOut.setValue(batteryIn + gridOut);
    double batteryOut = pBat > 0 ? pBat : 0;
    anyChange |= this.vGridPowerIn.setValue(batteryOut + gridIn);

    anyChange |= this.pvPower.setValue(data.getPpv1() + data.getPpv2() + data.getPmeter_dc());

    anyChange |= this.ppv1.setValue(data.getPpv1());
    anyChange |= this.ppv2.setValue(data.getPpv2());

    anyChange |= this.pMeterDc.setValue(data.getPmeter_dc());
    return anyChange;
  }

  @Override
  public boolean mapValues(SummeryDto data) {
    boolean anyChange = this.carbonNum.setValue(data.getCarbonNum());
    anyChange |= this.selfConsumption.setValue(getScaledValue(data.getEselfConsumption() * 100));
    anyChange |= this.selfSufficiency.setValue(getScaledValue(data.getEselfSufficiency() * 100));
    anyChange |= this.treeNum.setValue(getScaledValue(data.getTreeNum()));
    anyChange |= this.pvToday.setValue(data.getEpvtoday());
    anyChange |= this.pvTotal.setValue(data.getEpvtotal());
    anyChange |=
        this.startOfToday.setValue(LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC));
    anyChange |= this.totalIncome.setValue(data.getTotalIncome());
    anyChange |= this.todayIncome.setValue(data.getTodayIncome());
    anyChange |= this.todayCharge.setValue(data.getEcharge());
    anyChange |= this.todayDischarge.setValue(data.getEDisCharge());
    anyChange |= this.eload.setValue(data.getEload());
    anyChange |= this.einput.setValue(data.getEinput());
    anyChange |= this.eoutput.setValue(data.getEoutput());

    return anyChange;
  }

  @Override
  public boolean mapValues(SystemDto dataDto) {
    return false;
  }
}
