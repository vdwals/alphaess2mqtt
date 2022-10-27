package de.vdw.io.alpha2mqtt.services.updater;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
public class SettingsUpdateService implements Updater {

  WallBoxDeviceService wallboxDeviceService;

  BatteryDeviceService batteryDeviceService;

  ScheduledExecutorService scheduledExecutorService;

  SettingService settingService;

  HomeAssistantMQTTService mqttService;

  EnvironmentService environmentService;

  @Override
  public void init() {
    long delay = RandomUtils.nextLong(1, 11);

    long interval = Math.max(settingService.getRefreshRate(), environmentService.getIntervall());
    log.info("Start scheduling settings update in {} seconds with interval {}", delay, interval);
    scheduledExecutorService.scheduleAtFixedRate(this, delay, interval, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update setting data.");
    SystemDto data = settingService.getData();

    if (data == null) {
      log.error("No setting data available.");
      return;
    }
    log.debug("Setting data received.");

    boolean anyChange = wallboxDeviceService.mapValues(data);

    anyChange |= batteryDeviceService.mapValues(data);

    if (anyChange) {
      log.debug("Setting data mapped. Publishing via service.");
      mqttService.publishValues();
      log.debug("Setting data updated successfully");

    } else {
      log.debug("No changes in settings to publish");
    }
  }
}

