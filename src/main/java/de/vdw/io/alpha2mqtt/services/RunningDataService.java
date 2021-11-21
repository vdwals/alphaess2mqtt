package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.services.alpha.AlphaService;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.javalite.activejdbc.Base;
import org.javalite.http.Get;
import org.javalite.http.Http;

public class RunningDataService extends AlphaService<RunningDataDto> {

  final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public RunningDataService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  public RunningDataDto requestNewData(String token, LocalDateTime now) {

    String url = Base.withDb(() -> {
      AlphaEssLoadJob dataJob = AlphaEssLoadJob.getSecondDataJob();
      AlphaEssBattery battery = (AlphaEssBattery) AlphaEssBattery.findAll().limit(1).get(0);

      return String.format(dataJob.getUrl(), battery.getSn());
    });

    Get dataGet = Http.get(url)
        .header("Accept", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);

    String dataResponse = dataGet.text();

    try {
      ResponseDto<RunningDataDto> runningDataResponseDto =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      return runningDataResponseDto.getData();

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public long getNextRefreshInSeconds() {
    Integer seconds = Base.withDb(() -> AlphaEssLoadJob.getSecondDataJob().getIntervalInSeconds());

    long diff = Long.MAX_VALUE;
    if (latestResponse != null) {
      LocalDateTime updateAvailableAt =
          LocalDateTime.parse(latestResponse.getUploadtime(), formatter).plusSeconds(seconds);

      long now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
      long updateAvailable = updateAvailableAt.atZone(ZoneId.systemDefault()).toEpochSecond();
      diff = Math.abs(updateAvailable - now);
    }

    return Math.min(seconds, diff);
  }
}
