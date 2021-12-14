package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SummaryRequestDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Singleton
@Slf4j
public class SummeryService extends AlphaService<SummeryDto> {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public SummeryService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  public SummeryDto requestNewData(String token, LocalDateTime now) {
    String url =
        Base.withDb(
            () -> {
              AlphaEssLoadJob summeryJob = AlphaEssLoadJob.getSummeryJob();

              if (summeryJob == null) {
                log.error("No summary job received from DB");
                return null;
              }

              return summeryJob.getUrl();
            });

    if (url == null) {
      log.error("No summary url available");
      return null;
    }

    SummaryRequestDto requestDto =
        SummaryRequestDto.builder().showLoading(true).tday(now.format(formatter)).build();

    String payload = JsonHelper.toJsonString(requestDto);

    log.debug("Posting summary request");
    log.trace("Payload: {}", payload);

    Post summaryPost =
        RequestUtils.addPostHeader(
            Http.post(
                    url,
                    payload.getBytes(StandardCharsets.UTF_8),
                    (int) Constants.TIMEOUT,
                    (int) Constants.TIMEOUT)
                .header("Content-Type", Constants.APPLICATION_JSON),
            token);

    if (summaryPost.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error(
          "Unexpected response code while receiving summary data {}: {}",
          summaryPost.responseCode(),
          summaryPost.responseMessage());
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

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSummeryJob().getIntervalInSeconds());
  }
}
