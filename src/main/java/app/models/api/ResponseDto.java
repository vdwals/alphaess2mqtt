package app.models.api;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class ResponseDto {
    int code;
    String info;
}
