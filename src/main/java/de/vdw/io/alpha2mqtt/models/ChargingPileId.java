package de.vdw.io.alpha2mqtt.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChargingPileId {
  EV1("EV1"), EV2("EV2");

  @Getter
  private final String id;
}
