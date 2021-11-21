package de.vdw.io.alpha2mqtt.services.alpha;

import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.SummeryService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    scheduledExecutorService.schedule(this, 10, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update summary data");
    SummeryDto data = summeryService.getData();

    inverterDeviceService.mapValues(data);
    solarModuleDeviceService.mapValues(data);

    mqttService.publishValues();

    long delay = summeryService.getNextRefreshInSeconds();
    log.info("Next summary data update at in {} seconds", delay);
    scheduledExecutorService.schedule(this, delay, TimeUnit.SECONDS);
  }
}
