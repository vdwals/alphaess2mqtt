package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssSetting;
import de.vdw.io.alpha2mqtt.models.AlphaEssToken;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.TokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class TokenService {

  private final ObjectMapper objectMapper;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public String getToken() {
    log.debug("Get token from db.");
    AlphaEssToken currentToken = Base.withDb(AlphaEssToken::findCurrentToken);

    if (currentToken == null) {
      log.debug("No valid token found.");
      String url = Base.withDb(() -> AlphaEssLoadJob.getLoginJob().getUrl());

      if (url == null) {
        log.error("No Login url found.");
        return null;
      }

      Map<String, String> settingMap = Base.withDb(AlphaEssSetting::getSettings);

      if (settingMap == null || settingMap.isEmpty()) {
        log.error("No Login settings found.");
        return null;
      }

      String settings = JsonHelper.toJsonString(settingMap);

      Post loginPost =
          Http.post(url, settings)
              .header("Accept", Constants.APPLICATION_JSON)
              .header("Content-Type", Constants.APPLICATION_JSON);

      if (loginPost.responseCode() != HttpURLConnection.HTTP_OK) {
        log.error(
            "Unexpected response code while receiving token {}: {}",
            loginPost.responseCode(),
            loginPost.responseMessage());
        return null;
      }

      String loginResponse = loginPost.text();

      try {
        ResponseDto<TokenDto> loginResponseDto =
            objectMapper.readValue(loginResponse, new TypeReference<>() {});

        log.debug("Login response: {}", loginResponse);

        TokenDto tokenDto = loginResponseDto.getData();

        LocalDateTime expirationTime =
            LocalDateTime.parse(tokenDto.getTokenCreateTime(), formatter)
                .plusSeconds(tokenDto.getExpiresIn());

        log.debug("Token extracted, expiration time calculated: {}", expirationTime);

        currentToken =
            Base.withDb(
                () -> {
                  // Delete all tokens as they are expired.
                  log.debug("Deleting all existing tokens.");
                  AlphaEssToken.deleteAll();

                  // Save new token.
                  log.debug("Creating new token entry.");
                  return AlphaEssToken.create(
                      tokenDto.getAccessToken(), expirationTime, tokenDto.getRefreshTokenKey());
                });

      } catch (IOException e) {
        log.error("Error receiving token:", e);
      }
    }

    if (currentToken == null) {
      log.error("No token created or received.");
      return null;
    }

    log.debug("Return token");
    return currentToken.getToken();
  }
}
