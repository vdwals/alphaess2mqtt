package de.vdw.io.alpha2mqtt.models.api.charge;

import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

@Value
@With
@EqualsAndHashCode(callSuper = true)
public class SettingDto extends SystemDto {

  // TODO füge fehlende Informationen hinzu

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
  int sts_en = 1;
  int auto_startdg_en = 0;
  int max_gridcharge = 10;
  String systemId;
  String languageCode = "de-DE";
}
