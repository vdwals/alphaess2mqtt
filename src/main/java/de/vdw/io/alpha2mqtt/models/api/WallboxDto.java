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
  int time_charge_2;
  int chargingpile_phase;
  String time_charge_s1;
  String time_charge_e1;
  double max_current;
  String time_charge_s2;
  String time_charge_e2;
  String chargingpile_soft_ver;
  String chargingpile_hard_ver;
  String chargingpiletype;
  String chargingpile_startpower;
  String chargingpilename;
}
