package de.vdw.io.alpha2mqtt.services.alpha;

import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.models.api.charge.ChargingDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.net.HttpURLConnection;
import java.time.LocalTime;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ChargingService {
  private final RunningDataService runningDataService;
  private final ItemListService itemListService;
  private final TokenService tokenService;

  public enum ChargingTimeSlot {
    FIRST,
    SECOND;
  }

  public boolean startCharging() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return false;
    }

    RunningDataDto data = runningDataService.getData();
    SystemDto systemData = itemListService.getData();

    if (systemData == null) {
      log.error("Could not retrieve system settings");
      return false;
    }

    if (systemData.getCharging_pile_list() == null
        || systemData.getCharging_pile_list().isEmpty()) {
      log.error("No charging piles found");
      return false;
    }

    WallboxDto wallboxDto = systemData.getCharging_pile_list().get(0);
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

    Optional<ChargingDto> chargingDto1 =
        Base.withDb(
            () -> {
              AlphaEssLoadJob startChargingJob = AlphaEssLoadJob.getStartChargingJob();

              if (startChargingJob == null) {
                logError();
                return null;
              }

              Optional<ChargingDto> chargingDto =
                  AlphaEssBattery.findAll().include(AlphaEssWallbox.class).limit(1).stream()
                      .map(battery -> (AlphaEssBattery) battery)
                      .map(
                          battery -> {
                            String sn = battery.getSn();
                            Optional<String> wallboxSn =
                                battery.getAll(AlphaEssWallbox.class).limit(1).stream()
                                    .map(wallbox -> (AlphaEssWallbox) wallbox)
                                    .map(AlphaEssWallbox::getSn)
                                    .findFirst();

                            if (wallboxSn.isEmpty()) {
                              logError();
                              return null;
                            }
                            return new ChargingDto(sn, wallboxSn.get());
                          })
                      .findFirst();

              if (chargingDto.isEmpty()) {
                logError();
                return null;
              }

              return chargingDto;
            });

    if (chargingDto1.isEmpty()) {
      logError();
      return false;
    }

    Post post =
        RequestUtils.addHeader(Http.post(url, JsonHelper.toJsonString(chargingDto1)), token);

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

  private void logError() {
    log.error("No charging job found");
  }

  public void stopCharging() {}

  public void setChargingTime(ChargingTimeSlot timeSlot, LocalTime time) {}

  public void setCharingTimeActive(ChargingTimeSlot timeSlot, boolean active) {}
}
