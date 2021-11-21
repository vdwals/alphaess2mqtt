package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Singleton
public abstract class AlphaService<P> {

    @Getter
    private final ObjectMapper objectMapper;

    private final TokenService tokenService;

    protected P latestResponse;

    private LocalDateTime nextRefresh;

    public P getData() {
        LocalDateTime now = LocalDateTime.now();

        if (latestResponse == null || nextRefresh == null || now.isAfter(nextRefresh)) {
            String token = tokenService.getToken();

            latestResponse = requestNewData(token, now);

            nextRefresh = LocalDateTime.now().plusSeconds(getNextRefreshInSeconds());
        }

        return latestResponse;
    }

    protected abstract P requestNewData(String token, LocalDateTime now);

    public abstract long getNextRefreshInSeconds();
}
