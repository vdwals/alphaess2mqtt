package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class TokenDto implements DataDto {
  String AccessToken, RefreshTokenKey, TokenCreateTime;

  int ExpiresIn;
}
