package de.vdw.io.alpha2mqtt.services.alpha;

import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
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
  HomeAssistantMQTTService mqttService;

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
    boolean chargingModeSet = setChargingMode(chargingMode);

    if (!chargingModeSet) {
      log.error("Charging mode not set");
      return false;
    }

    // Post charging command.
    String payload = JsonHelper.toJsonString(chargingDto);
    log.debug("Calling charging url {}", url);
    log.trace("With payload: {}", payload);
    Post post =
        RequestUtils.addPostHeader(
            Http.post(
                url,
                payload.getBytes(StandardCharsets.UTF_8),
                (int) Constants.TIMEOUT,
                (int) Constants.TIMEOUT),
            token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging not started. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return false;
    }

    log.debug("Charging url called successfully");
    log.trace("Charging url post call response: {}", post.responseMessage());
    return true;
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
      log.debug(
          "Charging mode not changed. Expected: {}, Actual: {}", mode, systemDto.getChargingmode());
    }

    return modeSet;
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
