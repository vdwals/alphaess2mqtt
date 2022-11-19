package de.vdw.io.alpha2mqtt.models.api.charge;

import de.vdw.io.alpha2mqtt.models.ChargingPileId;
import lombok.Value;

@Value
public class ChargingPileDto {
  ChargingPileId chargingpile_id;
  String sys_sn;
}
