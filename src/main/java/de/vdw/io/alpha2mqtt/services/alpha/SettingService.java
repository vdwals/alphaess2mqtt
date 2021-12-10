package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.http.Get;
import org.javalite.http.Http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class SettingService extends AlphaService<SystemDto> {

  public SettingService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  public Optional<String> getSystemId() {
    return Base.withDb(
        () ->
            AlphaEssBattery.findAll().stream()
                .map(model -> (AlphaEssBattery) model)
                .map(AlphaEssBattery::getSystemId)
                .findFirst());
  }

  @Override
  protected SystemDto requestNewData(String token, LocalDateTime now) {
    log.info("Load wallbox information.");

    String url = Base.withDb(() -> AlphaEssLoadJob.getSettingsJob().getUrl());

    if (url == null) {
      log.error("No Settings url available");
      return null;
    }

    Optional<AlphaEssBattery> alphaEssBattery =
        Base.withDb(
            () ->
                AlphaEssBattery.findAll().stream()
                    .map(model -> (AlphaEssBattery) model)
                    .findFirst());

    if (alphaEssBattery.isEmpty()) {
      log.error("No Battery found");
      return null;
    }

    AlphaEssBattery battery = alphaEssBattery.get();
    url = String.format(url, battery.getSystemId());

    SystemDto systemResponseDto = getResponse(token, url);

    if (systemResponseDto == null) {
      log.error("No settings received.");
      return null;
    }

    Base.withDb(
        () -> {
          for (WallboxDto wallboxDto : systemResponseDto.getCharging_pile_list()) {
            AlphaEssWallbox.create(battery, wallboxDto.getChargingpile_sn());
          }
          return null;
        });

    return systemResponseDto;
  }

  private SystemDto getResponse(String token, String url) {
    Get dataGet = RequestUtils.addHeader(Http.get(url), token);

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Unexpected response code while receiving items {}: {}",
          dataGet.responseCode(),
          dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    try {
      ResponseDto<SystemDto> systemResponseDto =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      log.debug("Settings response: {}", systemResponseDto);

      return systemResponseDto.getData();

    } catch (IOException e) {
      log.error("Could not parse response.", e);
      return null;
    }
  }

  public SystemDto getSystemSettings() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return null;
    }

    log.info("Load wallbox information.");
    Optional<String> settingsUrl =
        Base.withDb(
            () -> {
              String url = AlphaEssLoadJob.getSettingsJob().getUrl();
              return AlphaEssBattery.findAll().stream()
                  .map(model -> (AlphaEssBattery) model)
                  .map(battery -> String.format(url, battery.getSystemId()))
                  .findFirst();
            });

    if (settingsUrl.isEmpty()) return null;

    String url = settingsUrl.get();

    return getResponse(token, url);
  }

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSettingsJob().getIntervalInSeconds());
  }
}
