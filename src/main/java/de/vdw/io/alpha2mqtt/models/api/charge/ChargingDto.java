package de.vdw.io.alpha2mqtt.models.api.charge;

import lombok.Value;

@Value
public class ChargingDto {
    String sys_sn, chargingpile_sn;
}
