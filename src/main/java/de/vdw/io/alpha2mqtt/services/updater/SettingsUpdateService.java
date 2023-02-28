package de.vdw.io.alpha2mqtt.services.updater;

import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@RequiredArgsConstructor
/**
 * Class for updating devices related to settings by calling the API at fixed rates.
 *
 * @author Dennis van der Wals
 *
 */
public class SettingsUpdateService implements Updater {

  ChargingPileDeviceService wallboxDeviceService;

  BatteryDeviceService batteryDeviceService;

  SettingService settingService;

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
        Math.max(this.settingService.getRefreshRate(), this.environmentService.getIntervall());
    log.info("Start scheduling settings update in {} seconds with interval {}", this.delay,
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
      log.info("Update setting data.");
      SystemDto data = this.settingService.getData();

      if (data == null) {
        log.error("No setting data available.");
        continue;
      }
      log.debug("Setting data received.");

      boolean anyChange = this.wallboxDeviceService.mapValues(data);

      anyChange |= this.batteryDeviceService.mapValues(data);

      if (anyChange) {
        log.debug("Setting data mapped. Publishing via service.");
        this.mqttService.publishValues();
        log.debug("Setting data updated successfully");

      } else {
        log.debug("No changes in settings to publish");
      }

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(this.interval));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}

