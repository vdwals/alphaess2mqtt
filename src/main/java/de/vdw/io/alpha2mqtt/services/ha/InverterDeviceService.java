package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.PERCENT;

@Slf4j
@Singleton
@Value
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
      vGridPowerOut;

  public InverterDeviceService() {
    super("Alpha ESS", "Smile5", "PV-Wechselrichter", "Smile5");

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
  }

  @Override
  public void mapValues(SummeryDto data) {
    carbonNum.setValue(data.getCarbonNum());
    selfConsumption.setValue(getScaledValue(data.getEselfConsumption() * 100));
    selfSufficiency.setValue(getScaledValue(data.getEselfSufficiency() * 100));
    treeNum.setValue(getScaledValue(data.getTreeNum()));
  }
}
