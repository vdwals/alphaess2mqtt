package de.vdw.io.alpha2mqtt.models.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ResponseDto<P> {
  int code;
  String info;

  P data;
}
