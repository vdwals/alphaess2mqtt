package de.vdw.io.alpha2mqtt.services.alpha.set;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.alpha.get.TokenService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.Payload;
import de.vdw.it.hamqtt.devices.entities.AbstractCommandEntity;
import de.vdw.it.hamqtt.utils.TopicUtils;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Value
@Slf4j
public class ChargingService implements ICommandListener {
  @RequiredArgsConstructor
  public enum ChargingMode {
    SLOW(1), NORMAL(2), FAST(3), MAX(4);

    public static ChargingMode chargingModeByValue(int value) {
      for (ChargingMode chargingMode : ChargingMode.values()) {
        if (chargingMode.mode == value)
          return chargingMode;
      }
      return null;
    }

    final int mode;
  }

  String batterySn, wallboxSn;
  SettingService settingService;
  TokenService tokenService;
  WallBoxDeviceService wallboxDeviceService;

  HomeAssistantMQTTService mqttService;

  private boolean callChargingUrl(String url) {
    if (url == null) {
      log.error("No url available for changing process");
      return false;
    }
    log.debug("Charging command url: {}", url);

    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return false;
    }

    // Build post body.
    ChargingDto chargingDto = getChargingDto();

    log.debug("System settings received: {}", chargingDto);

    // Post charging command.
    String payload = JsonHelper.toJsonString(chargingDto);
    log.debug("Calling charging url {}", url);
    log.trace("With payload: {}", payload);
    Post post = RequestUtils.addPostHeader(Http.post(url, payload.getBytes(StandardCharsets.UTF_8),
        (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Charging not started. Code: {}, Message: {}", post.responseCode(),
          post.responseMessage());
      return false;
    }

    log.debug("Charging url called successfully");
    log.trace("Charging url post call response: {}", post.responseMessage());
    return true;
  }

  private ChargingDto getChargingDto() {
    return new ChargingDto(batterySn, wallboxSn);
  }

  @Override
  public List<Device> getDevices() {
    return List.of(wallboxDeviceService.getDevice());
  }

  @Override
  public void received(String topic, byte[] bytes) {
    String command = new String(bytes, StandardCharsets.UTF_8);

    log.debug("Command received: {}", command);
    log.trace("On topic: {}", topic);

    AbstractCommandEntity charger = wallboxDeviceService.getCharger();
    AbstractCommandEntity chargerMode = wallboxDeviceService.getChargerMode();

    boolean anyChange = false;

    if (topic.endsWith(TopicUtils.removeRelativeTopic(charger.getCommandTopic()))) {
      Payload payload = EnumUtils.getEnum(Payload.class, command);
      if (payload == null) {
        log.error("Command {} could not be interpreted as expected payload.", command);
        return;
      }
      log.debug("Execute command for charger with payload {}", payload);

      switch (payload) {
        case ON:
          if (startCharging()) {
            anyChange = charger.setValue(payload);
          }
          break;

        case OFF:
          if (stopCharging()) {
            anyChange = charger.setValue(payload);
          }
          break;
      }
    } else if (topic.endsWith(TopicUtils.removeRelativeTopic(chargerMode.getCommandTopic()))) {
      ChargingMode chargingMode = EnumUtils.getEnumIgnoreCase(ChargingMode.class, command);
      if (chargingMode == null) {
        log.error("Command {} could not be interpreted as expected chargingMode.", command);
        return;
      }
      log.debug("Execute command for charge mode with chargingMode {}", chargingMode);

      setChargingMode(chargingMode);
      anyChange = true;
    }

    // Publish updated states
    if (anyChange) {
      mqttService.publishValues();
    }
  }

  private boolean setChargingMode(ChargingMode mode) {
    log.debug("Setting charging mode to {}", mode);

    log.debug("Retrieve settingsDto.");
    SettingDto settingDto = settingService.getSettingDto();

    // Set mode
    settingDto.setChargingmode(mode.mode);

    log.debug("Set updated settings.");
    SystemDto systemDto = settingService.updateSetting(settingDto);

    boolean modeSet = systemDto.getChargingmode() == mode.mode;

    if (modeSet) {
      // Update value of select entity
      wallboxDeviceService.getChargerMode().setValue(mode);
      log.debug("Charging mode updated successfully");
    } else {
      log.debug("Charging mode not changed. Expected: {}, Actual: {}", mode,
          systemDto.getChargingmode());
    }

    return modeSet;
  }

  private boolean startCharging() {
    return callChargingUrl(Constants.startCharginUrl);
  }

  private boolean stopCharging() {
    return callChargingUrl(Constants.stopCharginUrl);
  }
}
