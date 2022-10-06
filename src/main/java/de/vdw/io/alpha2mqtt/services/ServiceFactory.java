package de.vdw.io.alpha2mqtt.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.Cache;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.services.alpha.get.RunningDataService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SummeryService;
import de.vdw.io.alpha2mqtt.services.alpha.get.TokenService;
import de.vdw.io.alpha2mqtt.services.alpha.set.BatteryControlService;
import de.vdw.io.alpha2mqtt.services.alpha.set.ChargingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.DeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.io.alpha2mqtt.services.updater.RunningDataUpdateService;
import de.vdw.io.alpha2mqtt.services.updater.SettingsUpdateService;
import de.vdw.io.alpha2mqtt.services.updater.SummeryDataUpdateService;
import de.vdw.io.alpha2mqtt.services.updater.Updater;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.ICommandListener;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Singleton
@Slf4j
public class ServiceFactory {
  Cache cache;

  TokenService tokenService;

  HomeAssistantMQTTService mqttService;

  ScheduledExecutorService scheduledExecutorService;

  Map<BatteryDto, BatteryDeviceService> batteryDeviceServices = new HashMap<>();

  Map<BatteryDto, InverterDeviceService> inverterDeviceServices = new HashMap<>();

  Map<BatteryDto, List<WallBoxDeviceService>> wallBoxDeviceServices = new HashMap<>();

  ObjectMapper objectMapper;

  List<Updater> updateServices = new ArrayList<>(3);

  List<ICommandListener> commandServices = new ArrayList<>(2);

  public List<DeviceService> getDeviceServices() {
    return Stream
        .concat(wallBoxDeviceServices.values().stream(),
            Stream.of(batteryDeviceServices.values(), inverterDeviceServices.values()))
        .flatMap(Collection::stream).collect(Collectors.toList());
  }

  public void init() {
    cache.getBatteries().forEach(battery -> {
      log.info("Setup devices for {}", battery);

      BatteryDeviceService batteryDeviceService = new BatteryDeviceService(battery);
      batteryDeviceServices.put(battery, batteryDeviceService);
      InverterDeviceService inverterDeviceService = new InverterDeviceService(battery);
      inverterDeviceServices.put(battery, inverterDeviceService);

      List<WallBoxDeviceService> wallBoxDeviceServices =
          cache.getWallboxes().get(battery.getSys_sn()).stream().map(WallBoxDeviceService::new)
              .collect(Collectors.toList());
      this.wallBoxDeviceServices.put(battery, wallBoxDeviceServices);

      log.info("Setup device data mapping services for {}", battery);
      RunningDataService rds = new RunningDataService(objectMapper, tokenService, battery);
      RunningDataUpdateService rdus = new RunningDataUpdateService(batteryDeviceService,
          inverterDeviceService, wallBoxDeviceServices, scheduledExecutorService, rds, mqttService);
      updateServices.add(rdus);

      SummeryService summeryService = new SummeryService(objectMapper, tokenService);
      SummeryDataUpdateService summeryDataUpdateService = new SummeryDataUpdateService(
          inverterDeviceService, summeryService, scheduledExecutorService, mqttService);
      updateServices.add(summeryDataUpdateService);

      SettingService settingService = new SettingService(objectMapper, tokenService,
          cache.getSystemIdMap().get(battery.getSys_sn()));

      BatteryControlService batteryControlService =
          new BatteryControlService(batteryDeviceService, settingService, mqttService);
      commandServices.add(batteryControlService);

      wallBoxDeviceServices.forEach(wallBoxDeviceService -> {
        ChargingService chargingService =
            new ChargingService(battery.getSys_sn(), wallBoxDeviceService.getSn(), settingService,
                tokenService, wallBoxDeviceService, mqttService);
        commandServices.add(chargingService);

        SettingsUpdateService settingsUpdateService =
            new SettingsUpdateService(wallBoxDeviceService, batteryDeviceService,
                scheduledExecutorService, settingService, mqttService);
        updateServices.add(settingsUpdateService);
      });

    });
  }

}
