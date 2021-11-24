package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class TokenDto {
  String AccessToken, RefreshTokenKey, TokenCreateTime;

  int ExpiresIn;
}
