package de.vdw.io.alpha2mqtt.services.ha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.it.hamqtt.IMqttUpdateListener;
import de.vdw.it.hamqtt.devices.AbstractEntity;
import de.vdw.it.hamqtt.devices.sensor.Sensor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.inject.Singleton;

@Singleton
@Value
@EqualsAndHashCode(callSuper = true)
public class WallboxDeviceService extends DeviceService implements IMqttUpdateListener {

  AbstractEntity chargeEnergy, chargePower;

  public WallboxDeviceService() {
    super("Alpha ESS", "SMILE-EVCT11", "SMILE Wallbox", "ALP2021040257071");

    chargeEnergy =
        getEnergySensor("chargeEnergy", "Wallbox Energie geladen")
            .stateClass(Sensor.StateClass.total_increasing)
            .build();

    getDevice().addEntity(chargeEnergy);

    chargePower = getPowerSensor("chargePower", "Wallbox Ladeleistung");
  }

  @Override
  public void mapValues(RunningDataDto dataDto) {
    chargePower.setValue(dataDto.getEv1_power());
    chargeEnergy.setValue(dataDto.getEv1_chgenergy_real());
  }

  @Override
  public void mapValues(SummeryDto dataDto) {}
}
