package app.services;

import app.models.AlphaEssLoadJob;
import app.models.api.SummaryDto;
import app.models.api.SummaryRequestDto;
import app.models.api.SummaryResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static app.utils.Tokens.APPLICATION_JSON;

public class SummaryService implements ISummaryService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private TokenService tokenService;
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private SummaryDto latestResponse;
    
    private LocalDateTime nextRefresh;

    public SummaryDto getSummary() {
        LocalDateTime now = LocalDateTime.now();
    
        if (latestResponse != null && nextRefresh != null && now.isBefore(nextRefresh)) {
            return latestResponse;
        }
    
        AlphaEssLoadJob summaryJob = AlphaEssLoadJob.getSummaryJob();
        String token = tokenService.getToken();
    
        SummaryRequestDto requestDto = SummaryRequestDto
                .builder()
                .showLoading(true)
                .tday(now.format(formatter))
                .build();
        
        Post summaryPost = Http
                .post(
                        summaryJob.getUrl(),
                        JsonHelper.toJsonString(requestDto))
                .header("Accept", APPLICATION_JSON)
                .header("Content-Type", APPLICATION_JSON)
                .header("authorization", "Bearer " + token);
    
        String summaryResponse = summaryPost.text();
        try {
            SummaryResponseDto summaryResponseDto = objectMapper.readValue(summaryResponse, SummaryResponseDto.class);
            
            nextRefresh = now.plusSeconds(summaryJob.getIntervalInSeconds());
            
            return summaryResponseDto.getData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
