package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import org.javalite.http.Get;
import org.javalite.http.Http;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.RunningDataDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunningDataService extends AlphaService<RunningDataDto> {
  private final BatteryDto battery;

  public RunningDataService(ObjectMapper objectMapper, TokenService tokenService,
      BatteryDto battery) {
    super(objectMapper, tokenService);
    this.battery = battery;
  }

  @Override
  public long getRefreshRate() {
    return 10L;
  }

  @Override
  public RunningDataDto requestNewData(String token, LocalDateTime now) {

    String url = String.format(Constants.dataUrl, battery.getSys_sn());

    Get dataGet = RequestUtils
        .addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);
    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving live data {}: {}", dataGet.responseCode(),
          dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    try {
      ResponseDto<RunningDataDto> runningDataResponseDto =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      log.trace("Response: {}", runningDataResponseDto);

      return runningDataResponseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving live data:", e);

      return null;
    }
  }
}

