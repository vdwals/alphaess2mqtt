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
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class TokenService {

  private final ObjectMapper objectMapper;

  private final Credentials credentials;

  private String token;

  private LocalDateTime validTill;

  private String getCurrentToken() {
    if ((token == null) || validTill == null || LocalDateTime.now().isAfter(validTill)) {
      return null;
    }

    return token;
  }

  @Synchronized
  public String getToken() {
    log.debug("Get token from db.");

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

      Post loginPost = Http
          .post(url, settings.getBytes(StandardCharsets.UTF_8), (int) Constants.TIMEOUT,
              (int) Constants.TIMEOUT)
          .header("Accept", Constants.APPLICATION_JSON)
          .header("Content-Type", Constants.APPLICATION_JSON);

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
