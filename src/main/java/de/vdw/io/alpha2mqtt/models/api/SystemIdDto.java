package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class SystemIdDto {
  String system_id, sys_sn, bakbox_ver;
  Double icdc_id;
}
