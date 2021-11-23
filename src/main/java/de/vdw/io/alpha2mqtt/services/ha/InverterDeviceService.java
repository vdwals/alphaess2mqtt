package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.devices.DeviceInformation;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.List;

import static de.vdw.io.alpha2mqtt.utils.IdUtils.getUniqueId;
import static de.vdw.it.hamqtt.devices.Units.PERCENT;

@Slf4j
@Singleton
public class InverterDeviceService extends DeviceService {
    
    private final Sensor gridPower, gridPowerIn, gridPowerOut, powerConsumption, selfConsumption, selfSufficiency, carbonNum,
            treeNum, vGridPowerIn, vGridPowerOut;
    
    public InverterDeviceService() {
        super(DeviceInformation.builder()
                .manufacturer("Alpha ESS")
                .model("Smile5")
                .name("PV-Wechselrichter")
                .identifiers(List.of("Smile5"))
                .build());
        
        gridPower = getPowerSensor("gridPower", "Netz Leistung");
        
        powerConsumption =
                getPowerSensor("powerConsumption", "Verbraucher Leistung");
        
        selfConsumption = getPercentSensor(
                "selfConsumption",
                "Anteil PV Energie Eigenverbrauch");
        
        selfSufficiency = getPercentSensor("selfSufficiency", "Autarkie");
        
        gridPowerIn = getPowerSensor("gridPowerIn", "Netzbezug");
        gridPowerOut =
                getPowerSensor("gridPowerOut", "Netzeinspeisung");
        
        vGridPowerIn = getPowerSensor("vGridPowerIn", "virtueller Netzbezug");
        vGridPowerOut =
                getPowerSensor("vGridPowerOut", "virtuelle Netzeinspeisung");
        
        treeNum = getNumberSensor(
                "treeNum",
                "Gepflanzte Bäume",
                "mdi:forest", "Stk");
        
        carbonNum = getNumberSensor(
                "carbonNum",
                "CO2 Einsparung",
                "mdi:molecule-co2", "kg");
    }
    
    private Sensor getNumberSensor(
            String id,
            String name,
            String icon,
            String unitOfMeasurement) {
        Sensor s = Sensor.builder()
                .device(getDevice().getDeviceInformation())
                .objectId(id)
                .uniqueId(getUniqueId(getDevice().getNodeId(), id))
                .name(name)
                .icon(icon).unitOfMeasurement(unitOfMeasurement).build();
        
        getDevice().addEntity(s);
        return s;
    }
    
    private Sensor getPercentSensor(
            String objectId,
            String name) {
        return getSensor(
                null,
                objectId,
                name).unitOfMeasurement(PERCENT.getUnit()).build();
    }
    
    public void mapValues(RunningDataDto data) {
        double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();
        
        getDevice().updateValue(gridPower.getObjectId(), totalGridPower);
        getDevice().updateValue(powerConsumption.getObjectId(),
                totalGridPower + data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4()
                        + data.getPmeter_dc() + data.getPbat());
        
        double gridIn = totalGridPower < 0 ? 0 : totalGridPower;
        getDevice().updateValue(gridPowerIn.getObjectId(), gridIn);
        double gridOut = totalGridPower < 0 ? Math.abs(totalGridPower) : 0;
        getDevice().updateValue(gridPowerOut.getObjectId(), gridOut);
        
        double pBat = data.getPbat();
        double batteryIn = pBat > 0 ? 0 : Math.abs(pBat);
        getDevice().updateValue(vGridPowerOut.getObjectId(), batteryIn + gridOut);
        double batteryOut = pBat > 0 ? pBat : 0;
        getDevice().updateValue(vGridPowerIn.getObjectId(), batteryOut + gridIn);
    }
    
    public void mapValues(SummeryDto data) {
        getDevice().updateValue(carbonNum.getObjectId(), data.getCarbonNum());
        getDevice().updateValue(selfConsumption.getObjectId(),
                getScaledValue(data.getEselfConsumption() * 100));
        getDevice().updateValue(selfSufficiency.getObjectId(),
                getScaledValue(data.getEselfSufficiency() * 100));
        getDevice().updateValue(treeNum.getObjectId(), getScaledValue(data.getTreeNum()));
    }
}
