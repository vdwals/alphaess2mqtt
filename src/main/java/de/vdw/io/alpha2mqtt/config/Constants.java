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

  public static final String summeryUrl =
      basicUrl + "ESS/SticsSummeryDataForCustomer?sn=%s&tday=%s&showLoading=true";

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

  public static final String AUTH_SIGNATURE_HEADER = "authsignature";

  public static final String AUTH_SIGNATURE_START = "al8e4s";

  public static final String AUTH_SIGNATURE_HASH =
      "LS885ZYDA95JVFQKUIUUUV7PQNODZRDZIS4ERREDS0EED8BCWSS";

  public static final String AUTH_SIGNATURE_END = "ui893ed";

  public static final String AUTH_TIMESTAMP_HEADER = "authtimestamp";
}
