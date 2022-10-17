package de.vdw.io.alpha2mqtt.utils;

import org.javalite.http.Post;
import org.javalite.http.Request;
import de.vdw.io.alpha2mqtt.config.Constants;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class RequestUtils {

  public static <T extends Request<T>> T addHeader(T request, String token) {
    return addSignatureHeader(request.header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token));
  }

  public static Post addPostHeader(Post postRequest) {
    return addSignatureHeader(postRequest.header("Content-Type", Constants.APPLICATION_JSON));
  }

  public static Post addPostHeader(Post postRequest, String token) {
    return addHeader(postRequest.header("Content-Type", Constants.APPLICATION_JSON), token);
  }

  private static <T extends Request<T>> T addSignatureHeader(T request) {
    return request.header(Constants.AUTH_SIGNATURE_HEADER, Constants.AUTH_SIGNATURE_VALUE)
        .header(Constants.AUTH_TIMESTAMP_HEADER, Constants.AUTH_TIMESTAMP_VALUE)
        .header("Accept", Constants.APPLICATION_JSON);
  }
}
