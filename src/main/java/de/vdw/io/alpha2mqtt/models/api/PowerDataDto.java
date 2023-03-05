package de.vdw.io.alpha2mqtt.models.api;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PowerDataDto implements DataDto {
  String _id;
  String createtime;
  double ev1_power;
  double ev2_power;
  double ev3_power;
  double ev4_power;
  int factory_flag;
  double pbat;
  double pmeter_dc;
  double pmeter_l1;
  double pmeter_l2;
  double pmeter_l3;
  double poc_meter_l1;
  double poc_meter_l2;
  double poc_meter_l3;
  double ppv1;
  double ppv2;
  double ppv3;
  double ppv4;
  double preal_l1;
  double preal_l2;
  double preal_l3;
  String sn;
  Double soc;
  double sva;
  String uploadtime;
  double varac;
  double vardc;

  @JsonAnySetter
  @Singular("any")
  Map<String, String> properties;
}
