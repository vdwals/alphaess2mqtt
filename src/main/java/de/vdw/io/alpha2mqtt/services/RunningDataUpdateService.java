package de.vdw.io.alpha2mqtt.services;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.services.alpha.RunningDataService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

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

  HomeAssistantMQTTService mqttService;

  public void init() {
    long nextRefresh = runningDataService.getRefreshRate();
    log.info("Start scheduling live data in {} seconds", nextRefresh);
    scheduledExecutorService.scheduleAtFixedRate(this, nextRefresh, nextRefresh, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update live data");
    RunningDataDto data = runningDataService.getData();

    if (data == null) {
      log.error("No data available");
      return;
    }

    batteryDeviceService.mapValues(data);
    solarModuleDeviceService.mapValues(data);
    inverterDeviceService.mapValues(data);
    wallboxDeviceService.mapValues(data);

    mqttService.publishValues();
  }
}
