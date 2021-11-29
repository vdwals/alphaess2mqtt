package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;
import lombok.With;

@Value
@With
public class WallboxDto {
  String chargingpile_sn;
  String chargingpile_id;
  int chargingpile_switch;
  int priority;
  int time_charge_1;
  String time_charge_s1;
  String time_charge_e1;
  int time_charge_2;
  String time_charge_s2;
  String time_charge_e2;
  Object chargingpile_soft_ver;
  Object chargingpile_hard_ver;
  Object chargingpiletype;
  Object chargingpile_startpower;
  Object chargingpilename;
}
