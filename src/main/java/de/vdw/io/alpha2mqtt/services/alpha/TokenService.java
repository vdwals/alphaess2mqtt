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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class TokenService {

  private final ObjectMapper objectMapper;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public String getToken() {
    AlphaEssToken currentToken = Base.withDb(AlphaEssToken::findCurrentToken);

    if (currentToken == null) {
      String url = Base.withDb(() -> AlphaEssLoadJob.getLoginJob().getUrl());

      String settings = Base.withDb(() -> JsonHelper.toJsonString(AlphaEssSetting.getSettings()));

      Post loginPost =
          Http.post(url, settings)
              .header("Accept", Constants.APPLICATION_JSON)
              .header("Content-Type", Constants.APPLICATION_JSON);

      String loginResponse = loginPost.text();

      try {
        ResponseDto<TokenDto> loginResponseDto =
            objectMapper.readValue(loginResponse, new TypeReference<>() {});
        TokenDto tokenDto = loginResponseDto.getData();

        LocalDateTime expirationTime =
            LocalDateTime.parse(tokenDto.getTokenCreateTime(), formatter)
                .plusSeconds(tokenDto.getExpiresIn());

        currentToken =
            Base.withDb(
                () -> {
                  // Delete all tokens as they are expired.
                  AlphaEssToken.deleteAll();

                  // Save new token.
                  return AlphaEssToken.create(
                      tokenDto.getAccessToken(), expirationTime, tokenDto.getRefreshTokenKey());
                });

      } catch (IOException e) {
        log.error("Error receiving token:", e);
      }
    }

    if (currentToken == null) {
      return null;
    }

    return currentToken.getToken();
  }
}
