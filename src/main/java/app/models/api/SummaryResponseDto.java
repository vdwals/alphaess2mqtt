package app.models.api;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
public class SummaryResponseDto extends ResponseDto{
    SummeryDto data;
}
