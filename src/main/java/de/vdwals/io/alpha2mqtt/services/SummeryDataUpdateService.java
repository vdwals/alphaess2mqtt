package de.vdwals.io.alpha2mqtt.services;

import de.vdw.it.hamqtt.Service;
import de.vdw.it.hamqtt.devices.Device;
import de.vdwals.io.alpha2mqtt.models.api.SummeryDto;
import de.vdwals.io.alpha2mqtt.utils.IdUtils;
import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SummeryDataUpdateService implements Runnable {

  private Device device;

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

    String deviceId = IdUtils.getDeviceId(device.getDeviceInformation());

    device.updateValue("carbonNum", data.getCarbonNum());
    device.updateValue("pvToday", data.getEpvtoday());
    device.updateValue("pvTotal", data.getEpvtotal());
    device.updateValue("selfConsumption", data.getEselfConsumption());
    device.updateValue("selfSufficiency", data.getEselfSufficiency());
    device.updateValue("treeNum", data.getTreeNum());

    device.updateRawValue("sensor", "start_of_day", LocalDate.now().atStartOfDay());

    mqttService.publishValues();

    long delay = summeryService.getNextRefreshInSeconds();
    log.info("Next summary data update at in {} seconds", delay);
    scheduledExecutorService.schedule(this, delay, TimeUnit.SECONDS);
  }
}
