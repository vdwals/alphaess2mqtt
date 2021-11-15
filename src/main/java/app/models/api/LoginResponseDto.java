package app.models.api;

import lombok.Value;

@Value
public class LoginResponseDto extends ResponseDto{
    TokenDto data;
}
