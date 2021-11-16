package app.models.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.codehaus.jackson.annotate.JsonProperty;

@Value
public class TokenDto {
    String AccessToken, RefreshTokenKey, TokenCreateTime;
    
    int ExpiresIn;
}
