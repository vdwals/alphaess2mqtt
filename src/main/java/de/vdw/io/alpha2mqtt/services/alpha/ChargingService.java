package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.AbstractCommandEntity;
import de.vdw.it.hamqtt.devices.Device;
import de.vdw.it.hamqtt.devices.Payload;
import de.vdw.it.hamqtt.utils.JsonUtils;
import de.vdw.it.hamqtt.utils.TopicUtils;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Singleton
@Value
@Slf4j
public class ChargingService implements ICommandListener {
  SettingService settingService;
  TokenService tokenService;
  WallBoxDeviceService wallboxDeviceService;

  public ChargingService(
      SettingService settingService,
      TokenService tokenService,
      WallBoxDeviceService wallboxDeviceService) {
    this.settingService = settingService;
    this.tokenService = tokenService;
    this.wallboxDeviceService = wallboxDeviceService;
  }

  private boolean startCharging() {
    // Get url for charging start job.
    String url =
        Base.withDb(
            () -> {
              AlphaEssLoadJob startChargingJob = AlphaEssLoadJob.getStartChargingJob();

              if (startChargingJob == null) {
                logError();
                return null;
              }
              return startChargingJob.getUrl();
            });

    return callChargingUrl(url, ChargingMode.MAX);
  }

  private boolean callChargingUrl(String url, ChargingMode chargingMode) {
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
    Optional<ChargingDto> chargingDto = getChargingDto();

    if (chargingDto.isEmpty()) {
      logError();
      return false;
    }
    log.debug("System settings received: {}", chargingDto.get());

    // Set charging mode to max.
    boolean chargingModeSet = setChargingMode(token, chargingMode);

    if (!chargingModeSet) return false;

    // Post charging command.
    Post post =
        RequestUtils.addPostHeader(Http.post(url, JsonHelper.toJsonString(chargingDto)), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging not started. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return false;
    }

    log.debug("Start Charging response: {}", post.responseMessage());
    return true;
  }

  private boolean setChargingMode(String token, ChargingMode mode) {
    if (token == null) {
      token = tokenService.getToken();

      if (token == null) {
        log.error("No token available");
        return false;
      }
    }

    Optional<String> systemId = settingService.getSystemId();
    if (systemId.isEmpty()) {
      log.error("No System Id available.");
      return false;
    }

    SystemDto systemData = settingService.getSystemSettings();

    if (systemData == null) {
      log.error("Could not retrieve system settings.");
      return false;
    }

    String url =
        Base.withDb(
            () -> {
              AlphaEssLoadJob alphaEssLoadJob = AlphaEssLoadJob.setSettingsJob();

              if (alphaEssLoadJob == null) {
                log.error("No Settings set job found");
                return null;
              }

              return alphaEssLoadJob.getUrl();
            });

    SettingDto settingDto;
    try {
      // Copy system from response to setting for request.
      String systemString = JsonUtils.jsonMapper.writeValueAsString(systemData);

      settingDto = JsonUtils.jsonMapper.readValue(systemString, SettingDto.class);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
      return false;
    }

    // Adjust mode.
    settingDto.setChargingmode(mode.mode);

    // Add wallbox values
    settingDto.setWallbox(systemData.getCharging_pile_list().get(0));

    // Set system id
    settingDto.setSystem_id(systemId.get());

    String setting = JsonHelper.toJsonString(settingDto);

    log.debug("Posting charging mode request: {}", setting);

    Post post = RequestUtils.addPostHeader(Http.post(url, setting), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging mode not changed. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return false;
    }

    log.debug("Charging mode changed to {}. Response: {}", mode, post.text());

    // Validate
    systemData = settingService.getSystemSettings();

    return systemData.getChargingmode() == mode.mode;
  }

  private Optional<ChargingDto> getChargingDto() {
    return Base.withDb(
        () -> {
          AlphaEssLoadJob startChargingJob = AlphaEssLoadJob.getStartChargingJob();

          if (startChargingJob == null) {
            logError();
            return Optional.empty();
          }

          //noinspection unchecked
          Optional<ChargingDto> chargingDto =
              AlphaEssBattery.findAll().include(AlphaEssWallbox.class).stream()
                  .map(battery -> (AlphaEssBattery) battery)
                  .map(
                      battery -> {
                        String sn = battery.getSn();
                        Optional<String> wallBoxSn =
                            battery.getAll(AlphaEssWallbox.class).stream()
                                .map(AlphaEssWallbox::getSn)
                                .findFirst();

                        if (wallBoxSn.isEmpty()) {
                          logError();
                          return null;
                        }
                        return new ChargingDto(sn, wallBoxSn.get());
                      })
                  .findFirst();

          if (chargingDto.isEmpty()) {
            logError();
            return Optional.empty();
          }

          return chargingDto;
        });
  }

  private void logError() {
    log.error("No charging job found");
  }

  private boolean stopCharging() {
    // Get url for charging stop job.
    String url =
        Base.withDb(
            () -> {
              AlphaEssLoadJob stopChargingJob = AlphaEssLoadJob.getStopChargingJob();

              if (stopChargingJob == null) {
                logError();
                return null;
              }
              return stopChargingJob.getUrl();
            });

    return callChargingUrl(url, ChargingMode.NORMAL);
  }

  @Override
  public void received(String topic, byte[] bytes) {
    String command = new String(bytes, StandardCharsets.UTF_8);

    log.debug("Command received: {}", command);

    AbstractCommandEntity charger = wallboxDeviceService.getCharger();
    AbstractCommandEntity chargerMode = wallboxDeviceService.getChargerMode();

    if (topic.endsWith(TopicUtils.removeRelativeTopic(charger.getCommandTopic()))) {
      Payload payload = EnumUtils.getEnum(Payload.class, command);
      if (payload == null) {
        log.error("Command {} could not be interpreted as expected payload.", command);
        return;
      }
      log.debug("Execute command for charger with payload {}", payload);

      switch (payload) {
        case ON:
          if (startCharging()) charger.setValue(payload);

          break;

        case OFF:
          if (stopCharging()) charger.setValue(payload);
          break;
      }
    } else if (topic.endsWith(TopicUtils.removeRelativeTopic(chargerMode.getCommandTopic()))) {
      ChargingMode chargingMode = EnumUtils.getEnumIgnoreCase(ChargingMode.class, command);
      if (chargingMode == null) {
        log.error("Command {} could not be interpreted as expected chargingMode.", command);
        return;
      }
      log.debug("Execute command for charge mode with chargingMode {}", chargingMode);

      setChargingMode(null, chargingMode);
    }
  }

  @Override
  public List<Device> getDevices() {
    return List.of(wallboxDeviceService.getDevice());
  }

  @RequiredArgsConstructor
  public enum ChargingMode {
    SLOW(1),
    NORMAL(2),
    FAST(3),
    MAX(4);

    final int mode;

    public static ChargingMode chargingModeByValue(int value) {
      for (ChargingMode chargingMode : ChargingMode.values()) {
        if (chargingMode.mode == value) return chargingMode;
      }
      return null;
    }
  }
}
