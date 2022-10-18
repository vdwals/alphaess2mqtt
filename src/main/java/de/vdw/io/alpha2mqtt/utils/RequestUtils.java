package de.vdw.io.alpha2mqtt.utils;

import java.util.Date;
import org.apache.commons.codec.digest.DigestUtils;
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
    String timestamp = String.valueOf(new Date().getTime() / 1000);

    String formatHex = DigestUtils.sha512Hex(Constants.AUTH_SIGNATURE_HASH + timestamp);

    String signature = Constants.AUTH_SIGNATURE_START + formatHex + Constants.AUTH_SIGNATURE_END;

    return request.header(Constants.AUTH_SIGNATURE_HEADER, signature)
        .header(Constants.AUTH_TIMESTAMP_HEADER, timestamp)
        .header("Accept", Constants.APPLICATION_JSON);
  }

}
