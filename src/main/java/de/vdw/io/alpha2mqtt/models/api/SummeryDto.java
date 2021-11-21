package de.vdw.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class SummeryDto {
    double CarbonNum, ToalIncome, TodayIncome, TreeNum;
    
    String money_type;
    
    double Epvtoday;
    double Epvtotal;
    double EselfConsumption;
    double EselfSufficiency;
}
