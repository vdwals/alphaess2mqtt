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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@RequiredArgsConstructor
/**
 * Class for updating power data by calling the related API at fixed rate.
 *
 * @author Dennis van der Wals
 *
 */
public class PowerDataUpdateService implements Updater {

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
  public void init() {
    delay = RandomUtils.nextLong(1, 11);

    interval =
        Math.max((long) batteryDeviceService.getFrequency(), environmentService.getIntervall());
    log.info("Start scheduling live data in {} seconds with interval {}", delay, interval);
  }

  @Override
  public void run() {
    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(delay));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    while (true) {
      log.info("Update live data.");
      PowerDataDto data = powerDataService.getData();

      if (data == null) {
        log.error("No live data available.");
        return;
      }
      log.debug("Live data received.");

      batteryDeviceService.mapValues(data);
      inverterDeviceService.mapValues(data);
      wallboxDeviceServices.forEach(wallboxDeviceService -> wallboxDeviceService.mapValues(data));

      log.debug("Live data mapped. Publishing via service.");
      mqttService.publishValues();

      log.debug("Live data updated successfully.");

      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(interval));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
