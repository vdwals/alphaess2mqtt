package de.vdwals.io.alpha2mqtt.services;

import de.vdw.it.hamqtt.Service;
import de.vdw.it.hamqtt.devices.Device;
import de.vdwals.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdwals.io.alpha2mqtt.utils.IdUtils;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RunningDataUpdateService implements Runnable {

  private Device device;

  private final BatteryDeviceService batteryDeviceService;

  private final ScheduledExecutorService scheduledExecutorService;

  private final RunningDataService runningDataService;

  private final Service mqttService;

  public void init(Device device) {
    this.device = device;

    long nextRefresh = runningDataService.getNextRefreshInSeconds();
    log.info("Start scheduling live data in {} seconds", nextRefresh);
    scheduledExecutorService.schedule(this, nextRefresh, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    log.info("Update live data");
    RunningDataDto data = runningDataService.getData();

    String deviceId = IdUtils.getDeviceId(device.getDeviceInformation());

    double totalPvPower =
        data.getPpv1() + data.getPpv2() + data.getPpv3() + data.getPpv4() + data.getPmeter_dc();
    double totalGridPower = data.getPmeter_l1() + data.getPmeter_l2() + data.getPmeter_l3();

    device.updateValue("soc", data.getSoc());
    device.updateValue("pBat", data.getPbat());
    device.updateValue("ppvTotal", totalPvPower);
    device.updateValue("gridPower", totalGridPower);
    device.updateValue("powerConsumption", totalGridPower + totalPvPower + data.getPbat());

    mqttService.publishValues();

    long delay = runningDataService.getNextRefreshInSeconds();
    log.info("Next live data update in {} seconds", delay);
    scheduledExecutorService.schedule(this, delay, TimeUnit.SECONDS);
  }
}
