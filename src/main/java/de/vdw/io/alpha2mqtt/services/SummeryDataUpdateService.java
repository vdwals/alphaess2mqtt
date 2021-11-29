package de.vdw.io.alpha2mqtt.services;

import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.alpha.SummeryService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class SummeryDataUpdateService implements Runnable {

  private final InverterDeviceService inverterDeviceService;

  private final SolarModuleDeviceService solarModuleDeviceService;

  private final SummeryService summeryService;

  private final ScheduledExecutorService scheduledExecutorService;

  private final HomeAssistantMQTTService mqttService;

  public void init() {
    log.info("Start scheduling summary data in {} seconds", 10);
    scheduledExecutorService.scheduleAtFixedRate(
        this, 10, summeryService.getRefreshRate(), TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update summary data");
    SummeryDto data = summeryService.getData();

    if (data == null) {
      log.error("No data available");
      return;
    }

    inverterDeviceService.mapValues(data);
    solarModuleDeviceService.mapValues(data);

    mqttService.publishValues();
  }
}
