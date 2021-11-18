package app.services;

import app.models.AlphaEssBattery;
import app.models.AlphaEssLoadJob;
import app.models.api.ResponseDto;
import app.models.api.RunningDataDto;
import app.services.injections.IRunningDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.javalite.http.Get;
import org.javalite.http.Http;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static app.util.Tokens.APPLICATION_JSON;

public class RunningDataService extends AlphaService<RunningDataDto>
        implements IRunningDataService {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Inject
    public RunningDataService(ObjectMapper objectMapper, TokenService tokenService) {
        super(objectMapper, tokenService);
    }
    
    public RunningDataDto requestNewData(String token, LocalDateTime now) {
        
        AlphaEssLoadJob dataJob = AlphaEssLoadJob.getSecondDataJob();
        AlphaEssBattery battery = (AlphaEssBattery) AlphaEssBattery.findAll().limit(1).get(0);
        
        String url = String.format(dataJob.getUrl(), battery.getSn());
        
        Get dataGet = Http.get(url)
                          .header("Accept", APPLICATION_JSON)
                          .header("authorization", "Bearer " + token);
        
        String dataResponse = dataGet.text();
        
        try {
            ResponseDto<RunningDataDto> runningDataResponseDto = getObjectMapper().readValue(
                    dataResponse,
                    new TypeReference<ResponseDto<RunningDataDto>>() {});
    
            return runningDataResponseDto.getData();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Override
    public LocalDateTime calculateNextRefresh(RunningDataDto responseData, LocalDateTime now) {
        AlphaEssLoadJob dataJob = AlphaEssLoadJob.getSecondDataJob();
        return LocalDateTime.parse(latestResponse.getUploadtime(), formatter)
                            .plusSeconds(dataJob.getIntervalInSeconds());
    }
    
    @Override
    public RunningDataDto getRunningData() {
        return getData();
    }
}
