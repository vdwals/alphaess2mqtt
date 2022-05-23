package de.vdw.io.alpha2mqtt.services;

import javax.inject.Singleton;
import de.vdw.io.alpha2mqtt.models.Cache;
import de.vdw.io.alpha2mqtt.services.ha.DeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.Value;

@Value
@Singleton
public class MqttService {
  ServiceFactory serviceFactory;

  HomeAssistantMQTTService mqttService;

  Cache cache;

  public void init() {
    serviceFactory.getDeviceServices().stream().map(DeviceService::getDevice)
        .forEach(mqttService::addDevice);

    serviceFactory.getCommandServices().forEach(mqttService::addCommandListener);

    mqttService.connect();
  }

}
