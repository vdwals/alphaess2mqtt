package de.vdwals.io.alpha2mqtt.models.api;

import lombok.Value;

@Value
public class ResponseDto<P> {
    int code;
    String info;
    
    P data;
}
