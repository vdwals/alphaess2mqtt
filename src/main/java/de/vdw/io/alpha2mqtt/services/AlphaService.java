package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import java.time.LocalDateTime;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.common.JsonHelper;
import org.javalite.http.Request;

@RequiredArgsConstructor
@Singleton
@Slf4j
public abstract class AlphaService<P> {

  @Getter private final ObjectMapper objectMapper;

  private final TokenService tokenService;

  protected P latestResponse;

  public P getData() {
    LocalDateTime now = LocalDateTime.now();

    String token = tokenService.getToken();

    latestResponse = requestNewData(token, now);

    log.debug("Response: {}", JsonHelper.toJsonString(latestResponse, true));

    return latestResponse;
  }

  protected abstract P requestNewData(String token, LocalDateTime now);

  public abstract long getRefreshRate();

  protected <T extends Request<T>> T addHeader(T request, String token) {
    return request
        .header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);
  }
}
