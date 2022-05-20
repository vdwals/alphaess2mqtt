package de.vdw.io.alpha2mqtt.services.alpha.set;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Singleton;
import org.apache.commons.lang3.math.NumberUtils;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.Device;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Value
@Slf4j
public class BatteryControlService implements ICommandListener {
  BatteryDeviceService batteryDeviceService;
  SettingService settingService;
  HomeAssistantMQTTService mqttService;

  private void changeBatteryReserveSetting(String batteryReserve) {
    SettingDto settingDto = settingService.getSettingDto();

    // Replace decimal places
    batteryReserve = batteryReserve.split("\\.")[0];

    settingDto.setBat_use_cap(batteryReserve);

    SystemDto systemDto = settingService.updateSetting(settingDto);

    // Check if settings have been set
    // Publish update of value
    if (systemDto.getBat_use_cap().equals(batteryReserve) && batteryDeviceService.getUseCapacity().setValue(Integer.parseInt(batteryReserve))) {
      mqttService.publishValues();
    }
  }

  @Override
  public List<Device> getDevices() {
    return List.of(batteryDeviceService.getDevice());
  }

  @Override
  public void received(String topic, byte[] payload) {
    String command = new String(payload, StandardCharsets.UTF_8);
    log.debug("Topic {}, Payload {}", topic, command);

    if (NumberUtils.isParsable(command)) {
      changeBatteryReserveSetting(command);
    }
  }
}
