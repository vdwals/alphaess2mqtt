package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import javax.inject.Singleton;
import org.javalite.common.JsonHelper;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.javalite.http.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.charge.SettingDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import de.vdw.it.hamqtt.utils.JsonUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Class for calling the API to retrieve current settings and update the settings.
 *
 * @author Dennis van der Wals
 *
 */
public class SettingService extends AlphaService<SystemDto> {
  String systemId;

  public SettingService(ObjectMapper objectMapper, TokenService tokenService, String systemId) {
    super(objectMapper, tokenService);
    this.systemId = systemId;
  }

  @Override
  public long getRefreshRate() {
    return 600L;
  }

  /**
   * Fetches System settings and transforms it to a settings dto, to adjust settings.
   *
   * @return SettingDto created from latest system details.
   */
  public SettingDto getSettingDto() {
    log.debug("Build settings DTO.");

    SystemDto systemData = getData();

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
    if (systemData.getCharging_pile_list() != null && !systemData.getCharging_pile_list().isEmpty()) {
      settingDto.setWallbox(systemData.getCharging_pile_list().get(0));
    } else {
      log.warn("No charging pile found in system settings – skipping wallbox setup.");
    }

    // Set system id
    settingDto.setSystem_id(systemId);

    return settingDto;
  }

  @Override
  protected SystemDto requestNewData(String token, LocalDateTime now) {
    log.info("Load settings.");

    String url = String.format(Constants.getSettingUrl, systemId);

    log.debug("Requesting settings from {}", url);

    Get dataGet = RequestUtils
        .addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving items {}: {}", dataGet.responseCode(),
          dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    log.trace("Response: {}", dataResponse);

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

  /**
   * Call to API to adjust settings.
   *
   * @param settingDto Settings to set
   * @return Updated system dto
   */
  public SystemDto updateSetting(SettingDto settingDto) {
    String token = this.tokenService.getToken();

    String url = Constants.setSettingUrl;

    String setting = JsonHelper.toJsonString(settingDto);

    log.debug("Posting charging mode request.");
    log.trace("Request: {}", setting);

    Post post = RequestUtils.addPostHeader(Http.post(url, setting.getBytes(StandardCharsets.UTF_8),
        (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (post.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Charging mode not changed. Code: {}, Message: {}", post.responseCode(),
          post.responseMessage());
      return null;
    }

    log.debug("Settings changed to {}.", setting);
    log.trace("Response: {}", post.text());

    return getData();
  }
}
