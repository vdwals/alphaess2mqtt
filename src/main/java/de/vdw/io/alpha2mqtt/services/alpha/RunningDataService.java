package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.http.Get;
import org.javalite.http.Http;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;

@Slf4j
@Singleton
public class RunningDataService extends AlphaService<RunningDataDto> {

  public RunningDataService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  public RunningDataDto requestNewData(String token, LocalDateTime now) {

    String url =
        Base.withDb(
            () -> {
              AlphaEssLoadJob dataJob = AlphaEssLoadJob.getSecondDataJob();
              if (dataJob == null) {
                log.error("No dataJob received from DB.");
                return null;
              }

              AlphaEssBattery battery = (AlphaEssBattery) AlphaEssBattery.findAll().limit(1).get(0);
              if (battery == null) {
                log.error("No battery received from DB.");
                return null;
              }

              return String.format(dataJob.getUrl(), battery.getSn());
            });

    if (url == null) {
      log.error("No url generated");
      return null;
    }

    Get dataGet = addHeader(Http.get(url), token);
    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Unexpected response code while receiving live data {}: {}",
          dataGet.responseCode(),
          dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    try {
      ResponseDto<RunningDataDto> runningDataResponseDto =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      return runningDataResponseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving live data:", e);

      return null;
    }
  }

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSecondDataJob().getIntervalInSeconds());
  }
}
