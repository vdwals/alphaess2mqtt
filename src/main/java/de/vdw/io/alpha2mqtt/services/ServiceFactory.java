package de.vdw.io.alpha2mqtt.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.SystemIdDto;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.alpha.get.PowerDataService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SummeryService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SystemService;
import de.vdw.io.alpha2mqtt.services.alpha.get.TokenService;
import de.vdw.io.alpha2mqtt.services.alpha.set.BatteryControlService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.DeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.updater.ChargingPileUpdateService;
import de.vdw.io.alpha2mqtt.services.updater.PowerDataUpdateService;
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
/**
 * This class makes the initial request to the Alpha API to get all batteries and their systems. For
 * each system, all related fetch services are created.
 *
 * @author Dennis van der Wals
 *
 */
public class ServiceFactory {
  SystemService systemService;

  TokenService tokenService;

  HomeAssistantMQTTService mqttService;

  EnvironmentService environmentService;

  List<DeviceService> deviceServices = new LinkedList<>();

  ObjectMapper objectMapper;

  List<Updater> updateServices = new ArrayList<>(3);

  List<ICommandListener> commandServices = new ArrayList<>(2);

  public void init() {
    List<BatteryDto> batteries = systemService.getData();

    if (batteries == null) {
      return;
    }

    List<SystemIdDto> systemIds = systemService.getSystemIds();

    if (systemIds == null) {
      return;
    }

    batteries.forEach(battery -> {
      log.info("Setup devices for {}", battery);

      String sys_sn = battery.getSys_sn();
      Optional<SystemIdDto> systemId = systemIds.stream()
          .filter(systemIdDto -> systemIdDto.getSys_sn().equals(sys_sn)).findFirst();

      if (systemId.isEmpty()) {
        log.error("No system ID found for SN: {}", sys_sn);
        return;
      }
      String system_id = systemId.get().getSystem_id();

      BatteryDeviceService batteryDeviceService = new BatteryDeviceService(battery);
      deviceServices.add(batteryDeviceService);

      InverterDeviceService inverterDeviceService = new InverterDeviceService(battery);
      deviceServices.add(inverterDeviceService);

      SummeryService summeryService = new SummeryService(objectMapper, tokenService, battery);
      SummeryDataUpdateService summeryDataUpdateService = new SummeryDataUpdateService(
          inverterDeviceService, summeryService, mqttService, environmentService);
      updateServices.add(summeryDataUpdateService);

      SettingService settingService = new SettingService(objectMapper, tokenService, system_id);
      BatteryControlService batteryControlService =
          new BatteryControlService(batteryDeviceService, settingService, mqttService);
      commandServices.add(batteryControlService);

      List<ChargingPileDeviceService> chargingPileDeviceServices =
          systemService.requestChargingPiles(sys_sn, system_id).stream()
              .map(ChargingPileDeviceService::new).collect(Collectors.toList());
      deviceServices.addAll(chargingPileDeviceServices);

      log.info("Setup device data mapping services for {}", battery);
      PowerDataService rds = new PowerDataService(objectMapper, tokenService, battery.getSys_sn());
      PowerDataUpdateService rdus = new PowerDataUpdateService(batteryDeviceService,
          inverterDeviceService, chargingPileDeviceServices, rds, mqttService, environmentService);
      updateServices.add(rdus);

      chargingPileDeviceServices.forEach(wallBoxDeviceService -> {
        ChargingService chargingService =
            new ChargingService(objectMapper, sys_sn, wallBoxDeviceService.getSn(), settingService,
                tokenService, wallBoxDeviceService, mqttService, wallBoxDeviceService.getId());
        commandServices.add(chargingService);

        SettingsUpdateService settingsUpdateService =
            new SettingsUpdateService(wallBoxDeviceService, batteryDeviceService, settingService,
                mqttService, environmentService);
        updateServices.add(settingsUpdateService);

        ChargingPileUpdateService chargingPileUpdateService = new ChargingPileUpdateService(
            chargingPileDeviceServices, environmentService, mqttService, chargingService);
        updateServices.add(chargingPileUpdateService);
      });

    });
  }

}
