package app.models;

import org.javalite.activejdbc.Model;

public class AlphaEssLoadJob extends Model {
    private static final String COLUMN_URL = "url";
    public static final String LOWER_LIKE_CONCAT_LOWER = "LOWER(?) LIKE CONCAT('%', LOWER(?), '%')";
    public static final String LOGIN = "login";
    public static final String SUMMARY = "summary";
    public static final String DAY = "day";
    public static final String COLUMN_INTERVAL_S = "interval_s";
    
    public static AlphaEssLoadJob getLoginJob() {
        return findFirst(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL, LOGIN);
    }

    public static AlphaEssLoadJob getSummaryJob() {
        return findFirst(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL, SUMMARY);
    }

    public static AlphaEssLoadJob getTicsJob() {
        return findFirst(LOWER_LIKE_CONCAT_LOWER, COLUMN_URL, DAY);
    }
    
    public String getUrl() {
        return getString(COLUMN_URL);
    }
    
    public int getIntervalInSeconds() {
        return getInteger(COLUMN_INTERVAL_S);
    }
}
