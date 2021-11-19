package de.vdwals.io.alpha2mqtt.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdwals.io.alpha2mqtt.config.Constants;
import de.vdwals.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdwals.io.alpha2mqtt.models.api.ResponseDto;
import de.vdwals.io.alpha2mqtt.models.api.SummaryRequestDto;
import de.vdwals.io.alpha2mqtt.models.api.SummeryDto;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.inject.Singleton;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

@Singleton
public class SummeryService extends AlphaService<SummeryDto> {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private LocalDateTime lastRequest;

  public SummeryService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  public SummeryDto requestNewData(String token, LocalDateTime now) {
    AlphaEssLoadJob summaryJob = AlphaEssLoadJob.getSummeryJob();

    SummaryRequestDto requestDto = SummaryRequestDto.builder()
        .showLoading(true)
        .tday(now.format(formatter))
        .build();

    Post summaryPost = Http.post(summaryJob.getUrl(), JsonHelper.toJsonString(requestDto))
        .header("Accept", Constants.APPLICATION_JSON)
        .header("Content-Type", Constants.APPLICATION_JSON)
        .header("authorization", "Bearer " + token);

    String summaryResponse = summaryPost.text();
    try {
      ResponseDto<SummeryDto> summaryResponseDto = getObjectMapper().readValue(
          summaryResponse,
          new TypeReference<ResponseDto<SummeryDto>>() {
          });

      lastRequest = LocalDateTime.now();

      return summaryResponseDto.getData();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public long getNextRefreshInSeconds() {
    Integer seconds = Base.withDb(
        () -> AlphaEssLoadJob.getSummeryJob().getIntervalInSeconds());

    long diff = Long.MAX_VALUE;
    if (lastRequest != null) {

      long now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
      long updateAvailable = lastRequest.atZone(ZoneId.systemDefault()).toEpochSecond();
      diff = Math.abs(updateAvailable - now);
    }

    return Math.min(seconds, diff);
  }
}
