package app.services;

import app.models.AlphaEssLoadJob;
import app.models.api.ResponseDto;
import app.models.api.SummaryRequestDto;
import app.models.api.SummeryDto;
import app.services.injections.ISummeryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static app.util.Tokens.APPLICATION_JSON;

public class SummeryService extends AlphaService<SummeryDto> implements ISummeryService {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Inject
    public SummeryService(ObjectMapper objectMapper, TokenService tokenService) {
        super(objectMapper, tokenService);
    }
    
    public SummeryDto getSummary() {
        return getData();
    }
    
    @Override
    public SummeryDto requestNewData(String token, LocalDateTime now) {
        AlphaEssLoadJob summaryJob = AlphaEssLoadJob.getSummeryJob();
        
        SummaryRequestDto requestDto = SummaryRequestDto.builder()
                .showLoading(true)
                .tday(now.format(formatter))
                .build();
    
        Post summaryPost = Http.post(summaryJob.getUrl(), JsonHelper.toJsonString(requestDto))
                .header("Accept", APPLICATION_JSON)
                .header("Content-Type", APPLICATION_JSON)
                .header("authorization", "Bearer " + token);
        
        String summaryResponse = summaryPost.text();
        try {
            ResponseDto<SummeryDto> summaryResponseDto = getObjectMapper().readValue(summaryResponse,
                    new TypeReference<ResponseDto<SummeryDto>>() {
                    });
    
            return summaryResponseDto.getData();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public LocalDateTime calculateNextRefresh(SummeryDto responseData, LocalDateTime now) {
        AlphaEssLoadJob summaryJob = AlphaEssLoadJob.getSummeryJob();
        
        return now.plusSeconds(summaryJob.getIntervalInSeconds());
    }
}
