package de.vdwals.io.alpha2mqtt.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AlphaService<P> {

    @Getter
    private final ObjectMapper objectMapper;

    private final TokenService tokenService;

    protected P latestResponse;

    @Getter
    private LocalDateTime nextRefresh;

    public P getData() {
        LocalDateTime now = LocalDateTime.now();

        if (latestResponse == null || nextRefresh == null || now.isAfter(nextRefresh)) {
            String token = tokenService.getToken();

            latestResponse = requestNewData(token, now);

            nextRefresh = calculateNextRefresh(latestResponse, now);
        }

        return latestResponse;
    }

    protected abstract P requestNewData(String token, LocalDateTime now);

    protected abstract LocalDateTime calculateNextRefresh(P responseData, LocalDateTime now);
}
