package de.vdwals.io.alpha2mqtt.services.alpha;

import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdwals.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdwals.io.alpha2mqtt.services.RunningDataService;
import de.vdwals.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdwals.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdwals.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    long nextRefresh = runningDataService.getNextRefreshInSeconds();
    log.info("Start scheduling live data in {} seconds", nextRefresh);
    scheduledExecutorService.schedule(this, nextRefresh, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update live data");
    RunningDataDto data = runningDataService.getData();

    batteryDeviceService.mapValues(data);
    solarModuleDeviceService.mapValues(data);
    inverterDeviceService.mapValues(data);

    mqttService.publishValues();

    long delay = runningDataService.getNextRefreshInSeconds();
    log.info("Next live data update in {} seconds", delay);
    scheduledExecutorService.schedule(this, delay, TimeUnit.SECONDS);
  }
}
