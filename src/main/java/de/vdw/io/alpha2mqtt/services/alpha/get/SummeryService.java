package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SummaryRequestDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
public class SummeryService extends AlphaService<SummeryDto> {

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SummeryService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  public long getRefreshRate() {
    return 300L;
  }

  @Override
  public SummeryDto requestNewData(String token, LocalDateTime now) {
    String url = Constants.summeryUrl;

    SummaryRequestDto requestDto =
        SummaryRequestDto.builder().showLoading(true).tday(now.format(formatter)).build();

    String payload = JsonHelper.toJsonString(requestDto);

    log.debug("Posting summary request");
    log.trace("Payload: {}", payload);

    Post summaryPost =
        RequestUtils
            .addPostHeader(
                Http.post(url, payload.getBytes(StandardCharsets.UTF_8), (int) Constants.TIMEOUT,
                    (int) Constants.TIMEOUT).header("Content-Type", Constants.APPLICATION_JSON),
                token);

    if (summaryPost.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving summary data {}: {}",
          summaryPost.responseCode(), summaryPost.responseMessage());
      return null;
    }

    String summaryResponse = summaryPost.text();
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
