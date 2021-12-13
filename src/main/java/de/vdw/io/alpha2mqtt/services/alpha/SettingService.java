package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.Post;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class SettingService extends AlphaService<SystemDto> {

  public SettingService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  public SettingDto getSettingDto() {
    log.debug("Build settings DTO.");
    Optional<String> systemId =
        Base.withDb(
            () ->
                AlphaEssBattery.findAll().stream()
                    .map(model -> (AlphaEssBattery) model)
                    .map(AlphaEssBattery::getSystemId)
                    .findFirst());

    if (systemId.isEmpty()) {
      log.error("No System Id available.");
      return null;
    }

    SystemDto systemData = getSystemSettings();

    if (systemData == null) {
      log.error("Could not retrieve system settings.");
      return null;
    }

    log.debug("Build system settings.");
    SettingDto settingDto;
    try {
      // Copy system from response to setting for request.
      String systemString = JsonUtils.jsonMapper.writeValueAsString(systemData);

      settingDto = JsonUtils.jsonMapper.readValue(systemString, SettingDto.class);
    } catch (JsonProcessingException e) {
      log.error(e.getMessage(), e);
      return null;
    }

    // Add wallbox values
    settingDto.setWallbox(systemData.getCharging_pile_list().get(0));

    // Set system id
    settingDto.setSystem_id(systemId.get());

    return settingDto;
  }

  public SystemDto updateSetting(SettingDto settingDto) {
    String token = tokenService.getToken();

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

    String setting = JsonHelper.toJsonString(settingDto);

    log.debug("Posting charging mode request.");
    log.trace("Request: {}", setting);

    Post post = RequestUtils.addPostHeader(Http.post(url, setting), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Charging mode not changed. Code: {}, Message: {}",
          post.responseCode(),
          post.responseMessage());
      return null;
    }

    log.debug("Settings changed to {}.", setting);
    log.trace("Response: {}", post.text());

    return getSystemSettings();
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

      log.trace("Settings response: {}", systemResponseDto);

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

    log.info("Load system settings information.");
    Optional<String> settingsUrl =
        Base.withDb(
            () -> {
              String url = AlphaEssLoadJob.getSettingsJob().getUrl();
              return AlphaEssBattery.findAll().stream()
                  .map(model -> (AlphaEssBattery) model)
                  .map(battery -> String.format(url, battery.getSystemId()))
                  .findFirst();
            });

    if (settingsUrl.isEmpty()) {
      log.error("No settings url available");
      return null;
    }

    String url = settingsUrl.get();

    return getResponse(token, url);
  }

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSettingsJob().getIntervalInSeconds());
  }
}
