package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.javalite.http.Get;
import org.javalite.http.Http;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
public class SummeryService extends AlphaService<SummeryDto> {
  String sn;
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SummeryService(ObjectMapper objectMapper, TokenService tokenService, BatteryDto battery) {
    super(objectMapper, tokenService);

    sn = battery.getSys_sn();
  }

  @Override
  public long getRefreshRate() {
    return 300L;
  }

  @Override
  public SummeryDto requestNewData(String token, LocalDateTime now) {
    String url = String.format(Constants.summeryUrl, sn, formatter.format(now));

    log.debug("Get summary request");
    log.trace("URL: " + url);

    Get summary =
        RequestUtils.addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT)
            .header("Content-Type", Constants.APPLICATION_JSON), token);

    if (summary.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving summary data {}: {}",
          summary.responseCode(), summary.responseMessage());
      return null;
    }

    String summaryResponse = summary.text();
    try {
      ResponseDto<SummeryDto> summaryResponseDto =
          getObjectMapper().readValue(summaryResponse, new TypeReference<>() {});

      log.trace("Response: {}", summaryResponseDto);

      return summaryResponseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving summary data:", e);
      return null;
    }
  }
}
