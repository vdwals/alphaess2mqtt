package de.vdw.io.alpha2mqtt.services;

import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.alpha.SummeryService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

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
    long delay = RandomUtils.nextLong(1, 11);
    long interval = summeryService.getRefreshRate();
    log.info("Start scheduling summary data in {} seconds with interval {}", delay, interval);
    scheduledExecutorService.scheduleAtFixedRate(this, delay, interval, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update summary data");
    SummeryDto data = summeryService.getData();

    if (data == null) {
      log.error("No data available");
      return;
    }

    boolean anyChange = false;

    anyChange |= inverterDeviceService.mapValues(data);
    anyChange |= solarModuleDeviceService.mapValues(data);

    if (anyChange) {
      log.debug("Summary data mapped. Publishing via service.");
      mqttService.publishValues();

      log.debug("Summary data updated successfully.");
    } else {
      log.debug("No summary data updated.");
    }
  }
}
