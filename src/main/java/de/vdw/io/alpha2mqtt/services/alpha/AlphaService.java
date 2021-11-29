package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javalite.common.JsonHelper;
import org.javalite.http.Post;
import org.javalite.http.Request;

import javax.inject.Singleton;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Singleton
@Slf4j
public abstract class AlphaService<P> {

  @Getter private final ObjectMapper objectMapper;

  private final TokenService tokenService;

  protected static <T extends Request<T>> T addHeader(T request, String token) {
    return request
        .header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);
  }

  protected abstract P requestNewData(String token, LocalDateTime now);

  public abstract long getRefreshRate();

  protected static Post addHeader(Post postRequest, String token) {
    return addHeader(postRequest.header("Content-Type", Constants.APPLICATION_JSON), token);
  }

  public P getData() {
    String token = tokenService.getToken();

    if (token == null) {
      log.error("No token available");
      return null;
    }

    P response = requestNewData(token, LocalDateTime.now());

    if (response == null) log.error("No response available.");
    else log.debug("Response: {}", JsonHelper.toJsonString(response, true));

    return response;
  }
}
