package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.common.JsonHelper;

import javax.inject.Singleton;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Singleton
@Slf4j
public abstract class AlphaService<P> {

  @Getter private final ObjectMapper objectMapper;

  protected final TokenService tokenService;

  protected abstract P requestNewData(String token, LocalDateTime now);

  public abstract long getRefreshRate();

  public P getData() {
    log.debug("Get token from service.");
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return null;
    }

    log.debug("Request data from API");
    P response = requestNewData(token, LocalDateTime.now());

    if (response == null) {
      log.error("No response available.");
    } else {
      log.debug("Response received.");
      log.trace("Response: {}", JsonHelper.toJsonString(response, true));
    }

    return response;
  }
}
