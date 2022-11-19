package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import javax.inject.Singleton;
import org.javalite.http.Get;
import org.javalite.http.Http;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.PowerDataDto;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Class for API call to retrieve latest power data of System.
 *
 * @author dvw
 *
 */
public class PowerDataService extends AlphaService<PowerDataDto> {
  String sn;

  public PowerDataService(ObjectMapper objectMapper, TokenService tokenService, String battery_sn) {
    super(objectMapper, tokenService);
    this.sn = battery_sn;
  }

  @Override
  public long getRefreshRate() {
    return 10L;
  }

  @Override
  public PowerDataDto requestNewData(String token, LocalDateTime now) {

    String url = String.format(Constants.dataUrl, sn);

    Get dataGet = RequestUtils
        .addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving live data {}: {}", dataGet.responseCode(),
          dataGet.responseMessage());
      return null;
    }

    String dataResponse = dataGet.text();

    try {
      ResponseDto<PowerDataDto> runningDataResponseDto =
          getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

      log.trace("Response: {}", runningDataResponseDto);

      return runningDataResponseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving live data:", e);

      return null;
    }
  }
}

