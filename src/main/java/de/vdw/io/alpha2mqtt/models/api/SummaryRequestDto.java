package de.vdw.io.alpha2mqtt.models.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SummaryRequestDto {
    boolean showLoading;
    String sn, tday;
}
