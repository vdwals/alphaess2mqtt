package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.time.LocalDateTime;
import org.javalite.common.JsonHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
/**
 * Parent class of all Alpha API Services as Interface and token management.
 *
 * @author Dennis van der Wals
 *
 * @param <P>
 */
public abstract class AlphaService<P> {

  @Getter
  private final ObjectMapper objectMapper;

  protected final TokenService tokenService;

  /**
   * Retrieves the data from the API with token.
   *
   * @return Result Dto or null
   */
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

  /**
   * Returns the usual update rate of the API endpoint.
   *
   * @return Seconds between consecutive calls.
   */
  public abstract long getRefreshRate();

  /**
   * Calls the API and returns the result extracted from the response envelope.
   *
   * @param token Access token
   * @param now Current timestamp
   * @return API response or null
   */
  protected abstract P requestNewData(String token, LocalDateTime now);
}
