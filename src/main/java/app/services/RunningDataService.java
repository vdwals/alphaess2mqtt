package app.services;

import app.models.AlphaEssBattery;
import app.models.AlphaEssLoadJob;
import app.models.api.RunningDataDto;
import app.models.api.RunningDataResponseDto;
import app.services.injections.IRunningDataService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.javalite.activejdbc.Model;
import org.javalite.http.Get;
import org.javalite.http.Http;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static app.util.Tokens.APPLICATION_JSON;

public class RunningDataService implements IRunningDataService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Inject private final TokenService tokenService;
    
    @Inject
    public RunningDataService(TokenService tokenService) {this.tokenService = tokenService;}
    
    private RunningDataDto latestResponse;
    
    private LocalDateTime nextRefresh;
    
    @Override
    public RunningDataDto getRunningData() {
        LocalDateTime now = LocalDateTime.now();
        
        if (latestResponse == null || nextRefresh == null || now.isAfter(nextRefresh)) {
            
            AlphaEssLoadJob dataJob = AlphaEssLoadJob.getSecondDataJob();
            String          token   = tokenService.getToken();
            AlphaEssBattery battery = (AlphaEssBattery) AlphaEssBattery.findAll().limit(1).get(0);
            
            String url = String.format(dataJob.getUrl(), battery.getSn());
            
            Get dataGet = Http.get(url)
                              .header("Accept", APPLICATION_JSON)
                              .header("authorization", "Bearer " + token);
            
            String dataResponse = dataGet.text();
            
            try {
                RunningDataResponseDto runningDataResponseDto = objectMapper.readValue(dataResponse,
                                                                                       RunningDataResponseDto.class);
                
                latestResponse = runningDataResponseDto.getData();
                
                nextRefresh = LocalDateTime.parse(latestResponse.getUploadtime(),
                                                  formatter)
                                           .plusSeconds(dataJob.getIntervalInSeconds());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return latestResponse;
    }
}
