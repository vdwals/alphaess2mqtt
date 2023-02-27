package de.vdw.io.alpha2mqtt.models.api;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Singular;
import lombok.Value;

@Value
public class SummeryDto implements DataDto {
  double CarbonNum, TotalIncome, TodayIncome, TreeNum;

  String money_type;

  double Epvtoday;
  double Epvtotal;
  double Eload;
  double Eoutput;
  double Einput;
  double Echarge;
  double EDisCharge;
  double EselfConsumption;
  double EselfSufficiency;

  @JsonAnySetter
  @Singular("any")
  Map<String, String> properties;
}
