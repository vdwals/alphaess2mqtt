package de.vdw.io.alpha2mqtt.services.alpha.set;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.Payload;
import de.vdw.it.hamqtt.devices.entities.AbstractCommandEntity;
import de.vdw.it.hamqtt.utils.TopicUtils;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Value
@Slf4j
/**
 * Command listener service to update system settings.
 *
 * @author Dennis van der Wals
 *
 */
public class BatteryControlService implements ICommandListener {
  BatteryDeviceService batteryDeviceService;
  SettingService settingService;
  HomeAssistantMQTTService mqttService;

  /**
   * Retrieves current system settings, adapts the battery reserve and sends the update to the
   * settings update.
   *
   * @param batteryReserve Battery reserve to set
   */
  private void changeBatteryReserveSetting(String batteryReserve) {
    SettingDto settingDto = settingService.getSettingDto();

    if (settingDto == null) {
      log.error("Could not retrieve settings for battery reserve update.");
      return;
    }

    Double reserve = NumberUtils.createDouble(batteryReserve);
    reserve = Math.min(BatteryDeviceService.MAX_USV_CAPACITY,
        Math.max(reserve, BatteryDeviceService.MIN_USV_CAPACITY));

    settingDto.setBat_use_cap(String.valueOf(reserve));

    SystemDto systemDto = settingService.updateSetting(settingDto);

    if (systemDto == null) {
      log.error("No response after updating battery reserve.");
      return;
    }

    // Check if settings have been set
    // Publish update of value
    if (Double.valueOf(systemDto.getBat_use_cap()).equals(reserve)
        && batteryDeviceService.getUseCapacity().setValue(reserve)) {
      mqttService.publishValues();
    }
  }

  /**
   * Retrieves current system settings, adapts the battery usv mode and sends the update to the
   * settings update.
   *
   * @param usvMode usv mode to set
   */
  private void changeBatteryUsvModeSetting(int usvMode, Payload payload) {
    SettingDto settingDto = settingService.getSettingDto();

    if (settingDto == null) {
      log.error("Could not retrieve settings for USV mode update.");
      return;
    }

    settingDto.setUpsReserve(usvMode);

    SystemDto systemDto = settingService.updateSetting(settingDto);

    if (systemDto == null) {
      log.error("No response after updating USV mode.");
      return;
    }

    // Check if settings have been set
    // Publish update of value
    if (systemDto.getUpsReserve() == usvMode
        && batteryDeviceService.getUsvMode().setValue(payload)) {
      mqttService.publishValues();
    }
  }

  @Override
  public List<Device> getDevices() {
    return List.of(batteryDeviceService.getDevice());
  }

  @Override
  public void received(String topic, byte[] bytes) {
    String command = new String(bytes, StandardCharsets.UTF_8);

    log.debug("Command received: {}", command);
    log.trace("On topic: {}", topic);

    AbstractCommandEntity usvMode = batteryDeviceService.getUsvMode();
    AbstractCommandEntity usvReserveCapacity = batteryDeviceService.getUseCapacity();

    if (topic.endsWith(TopicUtils.removeRelativeTopic(usvReserveCapacity.getCommandTopic()))
        && NumberUtils.isCreatable(command)) {
      changeBatteryReserveSetting(command);

    } else if (topic.endsWith(TopicUtils.removeRelativeTopic(usvMode.getCommandTopic()))) {
      Payload payload = EnumUtils.getEnum(Payload.class, command);
      if (payload == null) {
        log.error("Command {} could not be interpreted as expected payload.", command);
        return;
      }
      log.debug("Execute command for usv mode with payload {}", payload);

      int targetUsvMode = 0;

      if (payload == Payload.ON)
        targetUsvMode = 1;

      changeBatteryUsvModeSetting(targetUsvMode, payload);
    }
  }
}
