package de.vdw.io.alpha2mqtt.services.updater;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SummeryService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@RequiredArgsConstructor
/**
 * Class for updating summary data and related devices by calling the API at fixes rates.
 *
 * @author Dennis van der Wals
 *
 */
public class SummeryDataUpdateService implements Updater {

  InverterDeviceService inverterDeviceService;

  SummeryService summeryService;

  HomeAssistantMQTTService mqttService;

  EnvironmentService environmentService;

  @NonFinal
  long delay;

  @NonFinal
  long interval;

  @Override
  public void init() {
    this.delay = RandomUtils.nextLong(1, 11);

    this.interval =
        Math.max(this.summeryService.getRefreshRate(), this.environmentService.getIntervall());
    log.info("Start scheduling summary data in {} seconds with interval {}", this.delay,
        this.interval);
  }

  @Override
  public void run() {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(this.delay));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    while (true) {
      log.info("Update summary data");
      SummeryDto data = this.summeryService.getData();

      if (data == null) {
        log.error("No data available");
        continue;
      }

      boolean anyChange = this.inverterDeviceService.mapValues(data);

      if (anyChange) {
        log.debug("Summary data mapped. Publishing via service.");
        this.mqttService.publishValues();

        log.debug("Summary data updated successfully.");
      } else {
        log.debug("No summary data updated.");
      }

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(this.interval));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
