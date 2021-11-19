package de.vdwals.io.alpha2mqtt.services;

import de.vdw.it.hamqtt.Service;
import de.vdw.it.hamqtt.devices.Device;
import de.vdwals.io.alpha2mqtt.models.api.SummeryDto;
import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SummeryDataUpdateService implements Runnable {

  private Device device;

  private final BatteryDeviceService batteryDeviceService;

  private final SummeryService summeryService;

  private final ScheduledExecutorService scheduledExecutorService;

  private final Service mqttService;

  public void init(Device device) {
    this.device = device;

    long nextRefresh = summeryService.getNextRefreshInSeconds();
    log.info("Start scheduling summary data in {} seconds", nextRefresh);
    scheduledExecutorService.schedule(this, nextRefresh, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update summary data");
    SummeryDto data = summeryService.getData();

    device.updateValue(batteryDeviceService.getCarbonNum().getObjectId(), data.getCarbonNum());
    device.updateValue(batteryDeviceService.getPvToday().getObjectId(), data.getEpvtoday());
    device.updateValue(batteryDeviceService.getPvTotal().getObjectId(), data.getEpvtotal());
    device.updateValue(batteryDeviceService.getSelfConsumption().getObjectId(),
        data.getEselfConsumption());
    device.updateValue(batteryDeviceService.getSelfSufficiency().getObjectId(),
        data.getEselfSufficiency());
    device.updateValue(batteryDeviceService.getTreeNum().getObjectId(), data.getTreeNum());

    device.updateRawValue(batteryDeviceService.getPvToday().getClassName(),
        BatteryDeviceService.START_OF_DAY,
        LocalDate.now().atStartOfDay());

    mqttService.publishValues();

    long delay = summeryService.getNextRefreshInSeconds();
    log.info("Next summary data update at in {} seconds", delay);
    scheduledExecutorService.schedule(this, delay, TimeUnit.SECONDS);
  }
}
