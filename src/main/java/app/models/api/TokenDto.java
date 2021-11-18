package app.models.api;

import lombok.Value;

@Value
public class TokenDto {
    String AccessToken, RefreshTokenKey, TokenCreateTime;
    
    int ExpiresIn;
}
