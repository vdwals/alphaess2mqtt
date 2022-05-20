package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.time.LocalDateTime;
import org.javalite.common.JsonHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class AlphaService<P> {

  @Getter
  private final ObjectMapper objectMapper;

  protected final TokenService tokenService;

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

  public abstract long getRefreshRate();

  protected abstract P requestNewData(String token, LocalDateTime now);
}
