package de.vdw.io.alpha2mqtt.services;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.alpha.RunningDataService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Value
public class RunningDataUpdateService implements Runnable {

  BatteryDeviceService batteryDeviceService;

  SolarModuleDeviceService solarModuleDeviceService;

  InverterDeviceService inverterDeviceService;

  WallBoxDeviceService wallboxDeviceService;

  ScheduledExecutorService scheduledExecutorService;

  RunningDataService runningDataService;

  ChargingService chargingService;

  HomeAssistantMQTTService mqttService;

  public void init() {
    long delay = RandomUtils.nextLong(1, 11);
    long interval = runningDataService.getRefreshRate();
    log.info("Start scheduling live data in {} seconds with interval {}", delay, interval);
    scheduledExecutorService.scheduleAtFixedRate(this, delay, interval, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update live data.");
    RunningDataDto data = runningDataService.getData();

    if (data == null) {
      log.error("No live data available.");
      return;
    }
    log.debug("Live data received.");

    boolean anyChange = false;

    anyChange |= batteryDeviceService.mapValues(data);
    anyChange |= solarModuleDeviceService.mapValues(data);
    anyChange |= inverterDeviceService.mapValues(data);
    anyChange |= wallboxDeviceService.mapValues(data);

    if (anyChange) {
      log.debug("Live data mapped. Publishing via service.");
      mqttService.publishValues();

      log.debug("Live data updated successfully.");
    } else {
      log.debug("No live data updated.");
    }
  }
}
