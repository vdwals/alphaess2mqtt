package de.vdw.io.alpha2mqtt.models.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResponseDto<P> {
  int code;
  String info;

  P data;
}
