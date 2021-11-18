package app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.Getter;

import java.time.LocalDateTime;

public abstract class AlphaService<P> {
    @Getter
    @Inject
    private final ObjectMapper objectMapper;
    
    @Inject
    private final TokenService tokenService;
    
    protected P latestResponse;
    
    private LocalDateTime nextRefresh;
    
    @Inject
    public AlphaService(ObjectMapper objectMapper, TokenService tokenService) {
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
    }
    
    public P getData() {
        LocalDateTime now = LocalDateTime.now();
        
        if (latestResponse == null || nextRefresh == null || now.isAfter(nextRefresh)) {
            String token = tokenService.getToken();
            
            latestResponse = requestNewData(token, now);
            
            nextRefresh = calculateNextRefresh(latestResponse, now);
        }
        
        return latestResponse;
    }
    
    public abstract P requestNewData(String token, LocalDateTime now);
    
    public abstract LocalDateTime calculateNextRefresh(P responseData, LocalDateTime now);
}
