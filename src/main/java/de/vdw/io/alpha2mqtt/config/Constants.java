package de.vdw.io.alpha2mqtt.config;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public static final String APPLICATION_JSON = "application/json";

  public static final long EXPIRE = TimeUnit.SECONDS.toSeconds(30);

  public static final long TIMEOUT = TimeUnit.SECONDS.toMillis(EXPIRE);

  public static final String START_OF_DAY = "start_of_day";

  public static final String basicUrl = "https://cloud.alphaess.com/api/";

  public static final String loginUrl = basicUrl + "Account/Login";

  public static final String summeryUrl = basicUrl + "ESS/SticsSummeryDataForCustomer";

  public static final String setSettingUrl = basicUrl + "Account/CustomUseESSSetting";

  public static final String getSettingUrl =
      basicUrl + "Account/GetCustomUseESSSetting?system_id=%s";

  public static final String startCharginUrl = basicUrl + "ESS/StartCharging";

  public static final String stopCharginUrl = basicUrl + "ESS/StopCharging";

  public static final String dataUrl = basicUrl + "ESS/GetSecondDataBySn?sys_sn=%s&noLoading=true";

  public static final String batteriesUrl = basicUrl + "Account/GetCustomMenuESSlist";

  public static final String systemIdUrl = basicUrl + "Account/GetCustomUseESSList";

  public static final String chargingPileId1 = "EV1";

  public static final String chargingPileId2 = "EV2";

  public static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy/M/d ah:mm:ss").localizedBy(Locale.SIMPLIFIED_CHINESE);

  public static final String ALPHA_SIGNATURE = "ALPHA.AUTH_SIGNATURE";

  public static final String ALPHA_TIMESTAMP = "ALPHA.AUTH_TIMESTAMP";

  public static final String AUTH_SIGNATURE_HEADER = "authsignature";

  public static final String AUTH_SIGNATURE_VALUE = System.getenv().getOrDefault(ALPHA_SIGNATURE,
      "al8e4s13dc9f335861149638562609cd1dd60f441bd656e4c6510a8f1b07ba6bc105605df2bb3c6e81589012bc54cf1f51a04c7453d609d0006a624491a393ed3a30d2ui893ed");

  public static final String AUTH_TIMESTAMP_HEADER = "authsignature";

  public static final String AUTH_TIMESTAMP_VALUE =
      System.getenv().getOrDefault(ALPHA_TIMESTAMP, "1666023810");

  public static final String ALPHA_PASSWORD = "ALPHA.PASSWORD";

  public static final String ALPHA_USERNAME = "ALPHA.USERNAME";

  public static final String MQTT_PROTOCOLL = "MQTT.PROTOCOLL";

  public static final String MQTT_DISCOVERY_TOPIC = "MQTT.DISCOVERY_TOPIC";

  public static final String MQTT_TOPIC = "MQTT.TOPIC";

  public static final String MQTT_PASSWORD = "MQTT.PASSWORD";

  public static final String MQTT_USERNAME = "MQTT.USERNAME";

  public static final String MQTT_HOST = "MQTT.HOST";

  public static final String MQTT_PORT = "MQTT.PORT";
}
