package app.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;

import java.time.LocalDateTime;

@Cached
public class AlphaEssToken extends Model {
    private static final String COLUMN_TOKEN = "token";
    
    private static final String COLUMN_EXPIRATION_TIME = "expiration_time";
    
    private static final String COLUMN_REFRESH_TOKEN = "refresh_token_key";
    
    public static AlphaEssToken create(String token, LocalDateTime expirationTime, String refreshToken) {
        return createIt(COLUMN_TOKEN, token, COLUMN_EXPIRATION_TIME, expirationTime, COLUMN_REFRESH_TOKEN, refreshToken);
    }
    
    public static AlphaEssToken findCurrentToken() {
        return findFirst(String.format("%s > ?", COLUMN_EXPIRATION_TIME), LocalDateTime.now());
    }
    
    public String getToken() {
        return getString(COLUMN_TOKEN);
    }
    
    public LocalDateTime getExpirationTime() {
        return getTimestamp(COLUMN_EXPIRATION_TIME).toLocalDateTime();
    }
    
    public String getRefreshToken() {
        return getString(COLUMN_REFRESH_TOKEN);
    }
}
