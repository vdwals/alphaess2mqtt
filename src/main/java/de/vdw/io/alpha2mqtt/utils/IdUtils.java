package de.vdw.io.alpha2mqtt.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class IdUtils {

  public static final String DELIMITER = "_";

  public static String getDeviceId(String... values) {
    return String.join(DELIMITER, values).toLowerCase().replace(" ", "");
  }

  public static String getUniqueId(String deviceId, String objectId) {
    return String.join(DELIMITER, deviceId, objectId);
  }
}
