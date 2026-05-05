package de.vdw.io.alpha2mqtt.services.updater;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.get.PowerDataService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
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
 * Class for updating power data by calling the related API at fixed rate.
 *
 * @author Dennis van der Wals
 *
 */
public class PowerDataUpdateService extends Updater {

  BatteryDeviceService batteryDeviceService;

  InverterDeviceService inverterDeviceService;

  List<ChargingPileDeviceService> wallboxDeviceServices;

  PowerDataService powerDataService;

  HomeAssistantMQTTService mqttService;

  EnvironmentService environmentService;

  @NonFinal
  long delay;

  @NonFinal
  long interval;

  @Override
  public void doUpdate() {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(this.delay));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!Thread.currentThread().isInterrupted()) {
      log.info("Update live data.");
      PowerDataDto data = this.powerDataService.getData();

      if (data == null) {
        log.error("No live data available. Waiting before retry.");
      } else {
        log.debug("Live data received.");

        this.batteryDeviceService.mapValues(data);
        this.inverterDeviceService.mapValues(data);
        this.wallboxDeviceServices
            .forEach(wallboxDeviceService -> wallboxDeviceService.mapValues(data));

        log.debug("Live data mapped. Publishing via service.");
        this.mqttService.publishValues();
        log.debug("Live data updated successfully.");
      }

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(this.interval));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  @Override
  public void init() {
    this.delay = RandomUtils.nextLong(1, 11);

    this.interval = Math.max((long) this.batteryDeviceService.getFrequency(),
        this.environmentService.getIntervall());
    log.info("Start scheduling live data in {} seconds with interval {}", this.delay,
        this.interval);
  }
}
