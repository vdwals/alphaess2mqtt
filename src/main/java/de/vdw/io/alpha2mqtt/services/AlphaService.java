package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.common.JsonHelper;
import org.javalite.http.Request;

import javax.inject.Singleton;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Singleton
@Slf4j
public abstract class AlphaService<P> {

  @Getter private final ObjectMapper objectMapper;

  private final TokenService tokenService;

  public P getData() {
    LocalDateTime now = LocalDateTime.now();

    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return null;
    }

    P response = requestNewData(token, now);

    if (response == null) log.error("No response available.");
    else log.debug("Response: {}", JsonHelper.toJsonString(response, true));

    return response;
  }

  protected abstract P requestNewData(String token, LocalDateTime now);

  public abstract long getRefreshRate();

  protected <T extends Request<T>> T addHeader(T request, String token) {
    return request
        .header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);
  }
}
