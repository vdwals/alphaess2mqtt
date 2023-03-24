package de.vdw.io.alpha2mqtt.services.updater;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
/**
 * Class for updating charging pile devices by calling the API at fixed rate.
 *
 * @author Dennis van der Wals
 *
 */
public class ChargingPileUpdateService extends Updater {

  List<ChargingPileDeviceService> wallboxDeviceServices;

  EnvironmentService environmentService;

  HomeAssistantMQTTService mqttService;

  ChargingService chargingService;

  @NonFinal
  long delay;

  @NonFinal
  long interval;

  @Override
  public void doUpdate() {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(this.delay));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    while (true) {
      log.info("Update charging pile states.");
      Integer data = this.chargingService.getData();

      if (data == null) {
        log.error("No charging data available.");
        continue;
      }
      log.debug("Charging data received.");

      this.wallboxDeviceServices
          .forEach(wallboxDeviceService -> wallboxDeviceService.mapValues(data));

      log.debug("Charging data mapped. Publishing via service.");
      this.mqttService.publishValues();

      log.debug("Charging data updated successfully.");

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(this.interval));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void init() {
    this.delay = RandomUtils.nextLong(1, 11);

    this.interval =
        Math.max(this.chargingService.getRefreshRate(), this.environmentService.getIntervall());
    log.info("Start scheduling charging pile state in {} seconds with interval {}", this.delay,
        this.interval);
  }

}
