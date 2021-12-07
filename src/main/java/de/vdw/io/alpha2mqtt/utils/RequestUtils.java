package de.vdw.io.alpha2mqtt.utils;

import de.vdw.io.alpha2mqtt.config.Constants;
import lombok.experimental.UtilityClass;
import org.javalite.http.Post;
import org.javalite.http.Request;

@UtilityClass
public final class RequestUtils {

  public static Post addPostHeader(Post postRequest, String token) {
    return addHeader(postRequest.header("Content-Type", Constants.APPLICATION_JSON), token);
  }

  public static <T extends Request<T>> T addHeader(T request, String token) {
    return request
        .header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);
  }
}
