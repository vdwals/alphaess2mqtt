package de.vdw.io.alpha2mqtt.models.api;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Singular;
import lombok.Value;
import lombok.With;

@Value
@With
public class ChargingPileDto {
  String chargingpile_hard_ver;
  String chargingpile_id;
  int chargingpile_phase;
  int chargingpile_strategy;
  String chargingpile_sn;
  String chargingpile_soft_ver;
  String chargingpile_startpower;
  int chargingpile_switch;
  String chargingpilename;
  String chargingpiletype;
  double max_current;
  int priority;
  int time_charge_1;
  int time_charge_2;
  String time_charge_e1;
  String time_charge_e2;
  String time_charge_s1;
  String time_charge_s2;

  @JsonAnySetter
  @Singular("any")
  Map<String, String> properties;
}
