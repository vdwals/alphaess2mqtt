package de.vdwals.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import static de.vdw.it.hamqtt.devices.sensor.Sensor.StateClass.total_increasing;

import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.DeviceClass;
import de.vdwals.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdwals.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdwals.io.alpha2mqtt.models.api.SummeryDto;
import de.vdwals.io.alpha2mqtt.utils.IdUtils;
import java.util.List;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;

@Slf4j
@Singleton
public class InverterDeviceService extends DeviceService {

  private final Sensor gridPower, powerConsumption, selfConsumption, selfSufficiency, carbonNum,
      treeNum;

  @Getter
  private final Device inverter;

  public InverterDeviceService() {
    log.info("Load and init batteries");
    DeviceInformation deviceInformation = Base.withDb(() -> AlphaEssBattery.findAll()
        .stream()
        .map(battery -> (AlphaEssBattery) battery)
        .map(battery -> DeviceInformation.builder()
            .manufacturer("Alpha ESS")
            .model("Smile5")
            .name("PV-Wechselrichter")
            .identifiers(List.of(battery.getSn()))
            .build())
        .findFirst()).get();

    log.info("Create sensors");
    String deviceId = IdUtils.getDeviceId(deviceInformation);

    inverter = new Device(deviceId, deviceInformation);

    gridPower = getPowerSensor(deviceInformation, deviceId, "gridPower", "Netz Leistung");
    inverter.addEntity(gridPower);

    powerConsumption =
        getPowerSensor(deviceInformation, deviceId, "powerConsumption", "Verbraucher Leistung");
    inverter.addEntity(powerConsumption);

    carbonNum = getSensor(deviceInformation,
        deviceId,
        DeviceClass.carbon_dioxide,
        "carbonNum",
        "CO2 Einsparung").unitOfMeasurement("kg").stateClass(total_increasing).build();
    inverter.addEntity(carbonNum);

    selfConsumption = getPercentSensor(deviceInformation,
        deviceId,
        "selfConsumption",
        "Anteil PV Energie Eigenverbrauch");
    inverter.addEntity(selfConsumption);

    selfSufficiency = getPercentSensor(deviceInformation, deviceId, "selfSufficiency", "Autarkie");
    inverter.addEntity(selfSufficiency);

    treeNum = getSensor(deviceInformation,
        deviceId,
        DeviceClass.None,
        "treeNum",
        "Gepflanzte Bäume").stateClass(total_increasing).build();
    inverter.addEntity(treeNum);

  }

  private Sensor getPercentSensor(DeviceInformation deviceInformation,
                                  String deviceId,
                                  String objectId,
                                  String name) {
    return getMeasurementSensor(deviceInformation,
        deviceId,
        DeviceClass.None,
        objectId,
        name).unitOfMeasurement(PERCENT.getUnit()).build();
  }

  public void mapValues(RunningDataDto data) {
    double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();

    inverter.updateValue(gridPower.getObjectId(), totalGridPower);
    inverter.updateValue(powerConsumption.getObjectId(),
        totalGridPower + data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4()
            + data.getPmeter_dc() + data.getPbat());
  }

  public void mapValues(SummeryDto data) {
    inverter.updateValue(carbonNum.getObjectId(), data.getCarbonNum());
    inverter.updateValue(selfConsumption.getObjectId(),
        getScaledValue(data.getEselfConsumption() * 100));
    inverter.updateValue(selfSufficiency.getObjectId(),
        getScaledValue(data.getEselfSufficiency() * 100));
    inverter.updateValue(treeNum.getObjectId(), data.getTreeNum());
  }
}
