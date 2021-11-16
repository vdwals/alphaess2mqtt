package app.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
public class SummeryDto {
    double CarbonNum, ToalIncome, TodayIncome, TreeNum;

    String money_type;

    double Epvtoday;
    double Epvtotal;
    double EselfConsumption;
    double EselfSufficiency;
}
