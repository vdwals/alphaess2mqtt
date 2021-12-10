package de.vdw.io.alpha2mqtt.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;

@Cached
public class AlphaEssLoadJob extends Model {
  private static final String LOWER_LIKE_CONCAT_LOWER =
      "LOWER(%s) LIKE CONCAT('%%', LOWER(?), '%%')";
  private static final String LOGIN = "login";
  private static final String SUMMERY = "summery";
  private static final String DAY = "day";
  private static final String SECOND = "second";
  private static final String COLUMN_INTERVAL_S = "interval_s";
  private static final String COLUMN_URL = "url";
  private static final String SETTING = "GetCustomUseESSSetting".toLowerCase();
  private static final String SET_SETTING = "/CustomUseESSSetting".toLowerCase();
  private static final String START_CHARGING = "start";
  private static final String STOP_CHARGING = "stop";

  public static AlphaEssLoadJob getLoginJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), LOGIN);
  }

  public static AlphaEssLoadJob getSummeryJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SUMMERY);
  }

  public static AlphaEssLoadJob getSecondDataJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SECOND);
  }

  public static AlphaEssLoadJob getSettingsJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SETTING);
  }

  public static AlphaEssLoadJob setSettingsJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SET_SETTING);
  }

  public static AlphaEssLoadJob getStartChargingJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), START_CHARGING);
  }

  public static AlphaEssLoadJob getStopChargingJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), STOP_CHARGING);
  }

  public String getUrl() {
    return getString(COLUMN_URL);
  }

  public int getIntervalInSeconds() {
    return getInteger(COLUMN_INTERVAL_S);
  }
}
