package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import javax.inject.Singleton;
import org.javalite.http.Http;
import org.javalite.http.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.Credentials;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.TokenDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Singleton
@Slf4j
@Value
/**
 * Service for requesting and managing access token.
 *
 * @author Dennis van der Wals
 *
 */
public class TokenService {

  ObjectMapper objectMapper;

  Credentials credentials;

  @NonFinal
  String token;

  @NonFinal
  LocalDateTime validTill;

  private String getCurrentToken() {
    if ((this.token == null) || (this.validTill == null)
        || LocalDateTime.now().isAfter(this.validTill))
      return null;

    return this.token;
  }

  @Synchronized
  /**
   * Returns token if still valid or requests new token
   *
   * @return
   */
  public String getToken() {
    log.debug("Check cached token.");

    String currentToken = getCurrentToken();

    if (currentToken == null) {
      log.debug("No valid token found.");
      String url = Constants.loginUrl;

      String settings;
      try {
        settings = objectMapper.writeValueAsString(credentials);
      } catch (JsonProcessingException e1) {
        log.error("Error receiving token:", e1);
        return null;
      }

      Post loginPost =
          RequestUtils.addPostHeader(Http.post(url, settings.getBytes(StandardCharsets.UTF_8),
              (int) Constants.TIMEOUT, (int) Constants.TIMEOUT));

      if (loginPost.responseCode() != HttpURLConnection.HTTP_OK) {
        log.error("Unexpected response code while receiving token {}: {}", loginPost.responseCode(),
            loginPost.responseMessage());
        return null;
      }

      String loginResponse = loginPost.text();

      try {
        ResponseDto<TokenDto> loginResponseDto =
            objectMapper.readValue(loginResponse, new TypeReference<>() {});

        log.trace("Login response: {}", loginResponse);

        TokenDto tokenDto = loginResponseDto.getData();

        token = tokenDto.getAccessToken();
        validTill = LocalDateTime.parse(tokenDto.getTokenCreateTime(), Constants.formatter)
            .plusSeconds(tokenDto.getExpiresIn());

        log.debug("Token extracted, expiration time calculated: {}", validTill);


        currentToken = token;

      } catch (IOException e) {
        log.error("Error receiving token:", e);
      }
    }

    if (currentToken == null) {
      log.error("No token created or received.");
      return null;
    }

    log.debug("Return token");
    return currentToken;
  }
}
