package de.vdw.io.alpha2mqtt.utils;

import de.vdw.it.hamqtt.devices.DeviceInformation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdUtils {

  public static final String DELIMITER = "_";

  public static String getUniqueId(String deviceId, String objectId) {
    return String.join(DELIMITER, deviceId, objectId);
  }

  public static String getDeviceId(DeviceInformation device) {
    return String.join(DELIMITER, device.getManufacturer(), device.getModel(), device.getName())
        .toLowerCase()
        .replace(" ", "");
  }
}
