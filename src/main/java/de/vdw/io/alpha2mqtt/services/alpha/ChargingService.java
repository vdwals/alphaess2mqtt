package de.vdw.io.alpha2mqtt.services.alpha;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.ChargingPileId;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingPileDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.services.alpha.get.AlphaService;
import de.vdw.io.alpha2mqtt.services.alpha.get.SettingService;
import de.vdw.io.alpha2mqtt.services.alpha.get.TokenService;
import de.vdw.io.alpha2mqtt.services.ha.ChargingPileDeviceService;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.Payload;
import de.vdw.it.hamqtt.devices.entities.AbstractCommandEntity;
import de.vdw.it.hamqtt.utils.TopicUtils;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Command listener and state service for Charging Pile related API.
 *
 * @author Dennis van der Wals
 *
 */
public class ChargingService extends AlphaService<Integer> implements ICommandListener {

  @RequiredArgsConstructor
  /**
   * Available charging modes
   *
   * @author Dennis van der Wals
   *
   */
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

  byte[] chargingDto, chargingPileDto;
  SettingService settingService;
  TokenService tokenService;
  ChargingPileDeviceService wallboxDeviceService;

  HomeAssistantMQTTService mqttService;

  public ChargingService(ObjectMapper objectMapper, String batterySn, String wallboxSn,
      SettingService settingService, TokenService tokenService,
      ChargingPileDeviceService chargingPileDeviceService, HomeAssistantMQTTService mqttService,
      ChargingPileId chargingPileId) {
    super(objectMapper, tokenService);

    this.settingService = settingService;
    this.tokenService = tokenService;
    this.wallboxDeviceService = chargingPileDeviceService;
    this.mqttService = mqttService;

    chargingDto = JsonHelper.toJsonString(new ChargingDto(batterySn, wallboxSn))
        .getBytes(StandardCharsets.UTF_8);
    chargingPileDto = JsonHelper.toJsonString(new ChargingPileDto(chargingPileId, batterySn))
        .getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Common method to call the charging API to start/stop charging. Takes care of post body and
   * token.
   *
   * @param url URL to call
   * @return Success of calling the URL
   */
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

    // Post charging command.
    log.debug("Calling charging url {}", url);
    Post post = RequestUtils.addPostHeader(
        Http.post(url, chargingDto, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Charging not started. Code: {}, Message: {}", post.responseCode(),
          post.responseMessage());
      return false;
    }

    String response = post.text();

    log.debug("Charging url called successfully");
    log.trace("Charging url post call response: {}", response);

    try {
      ResponseDto<String> responseDto =
          getObjectMapper().readValue(response, new TypeReference<>() {});

      if (responseDto.getCode() == HttpURLConnection.HTTP_OK) {
        log.trace("Charging url post call response: {}", responseDto);
        return true;
      }
      log.warn("Charging url post call response: {}", responseDto);

    } catch (IOException e) {
      log.error("Could not parse response.", e);
    }
    return false;
  }

  @Override
  public List<Device> getDevices() {
    return List.of(wallboxDeviceService.getDevice());
  }

  @Override
  public long getRefreshRate() {
    return 30;
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

  @Override
  protected Integer requestNewData(String token, LocalDateTime now) {
    String url = String.format(Constants.chargingStateUpdateUrl);

    Post dataGet = RequestUtils.addPostHeader(
        Http.post(url, chargingPileDto, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);
    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving vharging data {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    try {
      ResponseDto<Integer> value =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      if (value.getCode() != HttpURLConnection.HTTP_OK) {
        log.error("Response: {}", dataResponse);
        return null;
      }

      log.trace("Response: {}", value);

      return value.getData();

    } catch (IOException e) {
      log.error("Error receiving charging data:", e);
      log.error("Response: {}", dataResponse);
      return null;
    }
  }

  /**
   * Method to change the charging mode via settings service. Retrieves current system settings and
   * adapts the values. Sends the updates to the API.
   *
   * @param mode Charging mode to set
   * @return success of mode change
   */
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

  /**
   * Call API to start charging.
   *
   * @return success of API call
   */
  private boolean startCharging() {
    return callChargingUrl(Constants.startCharginUrl);
  }

  /**
   * Call API to stop charging.
   *
   * @return success of API call
   */
  private boolean stopCharging() {
    return callChargingUrl(Constants.stopCharginUrl);
  }
}
