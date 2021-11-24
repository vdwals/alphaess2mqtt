package de.vdw.io.alpha2mqtt.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;

@Cached
public class AlphaEssLoadJob extends Model {
  public static final String LOWER_LIKE_CONCAT_LOWER =
      "LOWER(%s) LIKE CONCAT('%%', LOWER(?), '%%')";
  public static final String LOGIN = "login";
  public static final String SUMMERY = "summery";
  public static final String DAY = "day";
  public static final String SECOND = "second";
  public static final String COLUMN_INTERVAL_S = "interval_s";
  private static final String COLUMN_URL = "url";

  public static AlphaEssLoadJob getLoginJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), LOGIN);
  }

  public static AlphaEssLoadJob getSummeryJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SUMMERY);
  }

  public static AlphaEssLoadJob getTicsJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), DAY);
  }

  public static AlphaEssLoadJob getSecondDataJob() {
    return findFirst(String.format(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL), SECOND);
  }

  public String getUrl() {
    return getString(COLUMN_URL);
  }

  public int getIntervalInSeconds() {
    return getInteger(COLUMN_INTERVAL_S);
  }
}
