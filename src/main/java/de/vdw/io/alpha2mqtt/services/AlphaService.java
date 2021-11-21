package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Singleton
public abstract class AlphaService<P> {
    
    @Getter
    private final ObjectMapper objectMapper;
    
    private final TokenService tokenService;
    
    protected P latestResponse;
    
    
    public P getData() {
        LocalDateTime now = LocalDateTime.now();
        
        String token = tokenService.getToken();
        
        latestResponse = requestNewData(token, now);
        
        return latestResponse;
    }
    
    protected abstract P requestNewData(String token, LocalDateTime now);
    
    public abstract long getRefreshRate();
}
