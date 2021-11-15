package app.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class SummaryDto {
    double carbonNum, totalIncome, todayIncome, treeNum;

    @JsonProperty("money_type")
    String moneyType;

    @JsonProperty("EpvToday")
    double pvToday;

    @JsonProperty("EpvTotal")
    double pvTotal;
    
    @JsonProperty("eSelfConsumption")
    double selfConsumption;
    
    @JsonProperty("eSelfSufficiency")
    double selfSufficiency;
}
