package de.vdw.io.alpha2mqtt.services.updater;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SummeryService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Value
public class SummeryDataUpdateService implements Updater {

  InverterDeviceService inverterDeviceService;

  SummeryService summeryService;

  ScheduledExecutorService scheduledExecutorService;

  HomeAssistantMQTTService mqttService;

  EnvironmentService environmentService;

  @Override
  public void init() {
    long delay = RandomUtils.nextLong(1, 11);

    long interval = Math.max(summeryService.getRefreshRate(), environmentService.getIntervall());
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

    boolean anyChange = inverterDeviceService.mapValues(data);

    if (anyChange) {
      log.debug("Summary data mapped. Publishing via service.");
      mqttService.publishValues();

      log.debug("Summary data updated successfully.");
    } else {
      log.debug("No summary data updated.");
    }
  }
}
