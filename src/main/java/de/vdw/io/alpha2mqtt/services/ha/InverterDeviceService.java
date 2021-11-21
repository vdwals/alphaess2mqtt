package de.vdw.io.alpha2mqtt.services.ha;

import static de.vdw.it.hamqtt.devices.Units.PERCENT;
import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import de.vdw.it.hamqtt.devices.sensor.Sensor.SensorBuilder;
import de.vdw.io.alpha2mqtt.utils.IdUtils;
import java.util.List;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InverterDeviceService extends DeviceService {

  private final Sensor gridPower, powerConsumption, selfConsumption, selfSufficiency, carbonNum,
      treeNum;

  @Getter
  private final Device inverter;

  public InverterDeviceService() {
    log.info("Load and init batteries");
    DeviceInformation deviceInformation = DeviceInformation.builder()
        .manufacturer("Alpha ESS")
        .model("Smile5")
        .name("PV-Wechselrichter")
        .identifiers(List.of("Smile5"))
        .build();

    log.info("Create sensors");
    String deviceId = IdUtils.getDeviceId(deviceInformation);

    inverter = new Device(deviceId, deviceInformation);

    gridPower = getPowerSensor(deviceInformation, deviceId, "gridPower", "Netz Leistung");
    inverter.addEntity(gridPower);

    powerConsumption =
        getPowerSensor(deviceInformation, deviceId, "powerConsumption", "Verbraucher Leistung");
    inverter.addEntity(powerConsumption);

    selfConsumption = getPercentSensor(deviceInformation,
        deviceId,
        "selfConsumption",
        "Anteil PV Energie Eigenverbrauch");
    inverter.addEntity(selfConsumption);

    selfSufficiency = getPercentSensor(deviceInformation, deviceId, "selfSufficiency", "Autarkie");
    inverter.addEntity(selfSufficiency);

    treeNum = getNumberSensor(deviceInformation,
        deviceId,
        "treeNum",
        "Gepflanzte Bäume",
        "mdi:forest").build();
    inverter.addEntity(treeNum);

    carbonNum = getNumberSensor(deviceInformation,
        deviceId,
        "carbonNum",
        "CO2 Einsparung",
        "mdi:molecule-co2").unitOfMeasurement("kg").build();
    inverter.addEntity(carbonNum);
  }

  private SensorBuilder getNumberSensor(DeviceInformation deviceInformation,
                                        String deviceId,
                                        String id,
                                        String name,
                                        String icon) {
    return Sensor.builder()
        .device(deviceInformation)
        .objectId(id)
        .uniqueId(getUniqueId(deviceId, id))
        .name(name)
        .icon(icon);
  }

  private Sensor getPercentSensor(DeviceInformation deviceInformation,
                                  String deviceId,
                                  String objectId,
                                  String name) {
    return getSensor(deviceInformation,
        deviceId,
        null,
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
    inverter.updateValue(treeNum.getObjectId(), getScaledValue(data.getTreeNum()));
  }
}
