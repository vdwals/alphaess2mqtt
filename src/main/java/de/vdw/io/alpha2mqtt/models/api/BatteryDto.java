package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class BatteryDto {
  String sys_sn, sys_name, minv, mbat, ems_status, parallel_mode;
  Double popv, poinv, cobat, surpluscobat, uscapacity, trans_frequency;
}
