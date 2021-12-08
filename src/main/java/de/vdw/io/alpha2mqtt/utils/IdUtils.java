package de.vdw.io.alpha2mqtt.utils;

import de.vdw.it.hamqtt.devices.Device;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class IdUtils {

  public static final String DELIMITER = "_";

  public static String getUniqueId(String deviceId, String objectId) {
    return String.join(DELIMITER, deviceId, objectId);
  }

  public static String getDeviceId(Device device) {
    return getDeviceId(device.getManufacturer(), device.getModel(), device.getName());
  }

  public static String getDeviceId(String manufacturer, String model, String name) {
    return String.join(DELIMITER, manufacturer, model, name).toLowerCase().replace(" ", "");
  }
}
