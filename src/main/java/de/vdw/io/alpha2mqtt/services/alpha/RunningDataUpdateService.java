package de.vdw.io.alpha2mqtt.services.alpha;

import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.services.RunningDataService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RunningDataUpdateService implements Runnable {

  private final BatteryDeviceService batteryDeviceService;

  private final SolarModuleDeviceService solarModuleDeviceService;

  private final InverterDeviceService inverterDeviceService;

  private final ScheduledExecutorService scheduledExecutorService;

  private final RunningDataService runningDataService;

  private final HomeAssistantMQTTService mqttService;

  public void init() {
    long nextRefresh = runningDataService.getRefreshRate();
    log.info("Start scheduling live data in {} seconds", nextRefresh);
    scheduledExecutorService.scheduleAtFixedRate(this, nextRefresh, nextRefresh, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update live data");
    RunningDataDto data = runningDataService.getData();

    batteryDeviceService.mapValues(data);
    solarModuleDeviceService.mapValues(data);
    inverterDeviceService.mapValues(data);

    mqttService.publishValues();
  }
}
