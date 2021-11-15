package app.models.api;

import lombok.Value;

import java.util.Map;

public abstract class ResponseDto {
    int code;
    String info;
}
