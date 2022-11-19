package de.vdw.io.alpha2mqtt.services.updater;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import de.vdw.io.alpha2mqtt.services.EnvironmentService;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
/**
 * Class for updating charging pile devices by calling the API at fixed rate.
 *
 * @author Dennis van der Wals
 *
 */
public class ChargingPileUpdateService implements Updater {

  List<ChargingPileDeviceService> wallboxDeviceServices;

  EnvironmentService environmentService;

  ScheduledExecutorService scheduledExecutorService;

  HomeAssistantMQTTService mqttService;

  ChargingService chargingService;

  @Override
  public void init() {
    long delay = RandomUtils.nextLong(1, 11);

    long interval = Math.max(chargingService.getRefreshRate(), environmentService.getIntervall());
    log.info("Start scheduling charging pile state in {} seconds with interval {}", delay,
        interval);
    scheduledExecutorService.scheduleAtFixedRate(this, delay, interval, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update charging pile states.");
    Integer data = chargingService.getData();

    if (data == null) {
      log.error("No charging data available.");
      return;
    }
    log.debug("Charging data received.");

    wallboxDeviceServices.forEach(wallboxDeviceService -> wallboxDeviceService.mapValues(data));

    log.debug("Charging data mapped. Publishing via service.");
    mqttService.publishValues();

    log.debug("Charging data updated successfully.");
  }

}
