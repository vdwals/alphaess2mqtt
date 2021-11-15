package app.models.api;

import lombok.Value;

@Value
public class SummaryResponseDto extends ResponseDto{
    SummaryDto data;
}
