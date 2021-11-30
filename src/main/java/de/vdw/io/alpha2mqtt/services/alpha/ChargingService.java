package de.vdw.io.alpha2mqtt.services.alpha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.ICommandListener;
import de.vdw.it.hamqtt.devices.Device;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;

@Singleton
@Value
@Slf4j
public class ChargingService implements ICommandListener {
  ItemListService itemListService;
  TokenService tokenService;
  WallBoxDeviceService wallboxDeviceService;

  private boolean startCharging() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return false;
    }

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

    if (url == null) {
      log.error("No url available for starting charging process");
      return false;
    }

    Optional<ChargingDto> chargingDto = getChargingDto();

    if (chargingDto.isEmpty()) {
      logError();
      return false;
    }

    boolean chargingModeSet = setChargingMode(token, ChargingMode.MAX);

    if (!chargingModeSet) return false;

    Post post = RequestUtils.addHeader(Http.post(url, JsonHelper.toJsonString(chargingDto)), token);

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
    SystemDto systemData = itemListService.getData();

    if (systemData == null) {
      log.error("Could not retrieve system settings.");
      return false;
    }

    SystemDto systemDtoToSet = systemData.withCharge_mode1(mode.mode);

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

    Post post =
        RequestUtils.addHeader(Http.post(url, JsonHelper.toJsonString(systemDtoToSet)), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging mode not changed. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return false;
    }

    return true;
  }

  private Optional<ChargingDto> getChargingDto() {
    return Base.withDb(
        () -> {
          AlphaEssLoadJob startChargingJob = AlphaEssLoadJob.getStartChargingJob();

          if (startChargingJob == null) {
            logError();
            return Optional.empty();
          }

          Optional<ChargingDto> chargingDto =
              AlphaEssBattery.findAll().include(AlphaEssWallbox.class).limit(1).stream()
                  .map(battery -> (AlphaEssBattery) battery)
                  .map(
                      battery -> {
                        String sn = battery.getSn();
                        Optional<String> wallBoxSn =
                            battery.getAll(AlphaEssWallbox.class).limit(1).stream()
                                .map(wallBox -> (AlphaEssWallbox) wallBox)
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

  private boolean resetCharging() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return false;
    }

    return setChargingMode(token, ChargingMode.NORMAL);
  }

  private void logError() {
    log.error("No charging job found");
  }

  private boolean stopCharging() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return false;
    }

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

    if (url == null) {
      log.error("No url available for stopping charging process");
      return false;
    }

    Optional<ChargingDto> chargingDto = getChargingDto();

    if (chargingDto.isEmpty()) {
      logError();
      return false;
    }

    boolean chargingModeReset = resetCharging();

    if (!chargingModeReset) {
      log.error("Charging mode not rested");
      return false;
    }

    Post post = RequestUtils.addHeader(Http.post(url, JsonHelper.toJsonString(chargingDto)), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging not stopped. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return false;
    }

    log.debug("Stop Charging response: {}", post.responseMessage());
    return true;
  }

  @Override
  public void received(String topic, byte[] bytes) {
    // Topic und command prüfen
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
  }
}
