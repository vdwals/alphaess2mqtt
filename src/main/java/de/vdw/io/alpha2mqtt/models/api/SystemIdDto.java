package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Singular;

@Value
public class SystemIdDto {
  String system_id, sys_sn, bakbox_ver;
  Double icdc_id;
  
  @JsonAnySetter

  @Singular("any")

Map<String, String> properties;
}
