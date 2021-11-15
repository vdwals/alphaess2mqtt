package app.models.api;

import lombok.Value;

@Value
public class TokenDto {
    String accessToken, refreshTokenKey, tokenCreationTime;
    int expiresIn;
}
