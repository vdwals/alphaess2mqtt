package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SummaryRequestDto;
import de.vdw.io.alpha2mqtt.models.api.SummeryDto;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import javax.inject.Singleton;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Singleton
public class SummeryService extends AlphaService<SummeryDto> {

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private LocalDateTime lastRequest;

  public SummeryService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  public SummeryDto requestNewData(String token, LocalDateTime now) {
    String url = Base.withDb(() -> AlphaEssLoadJob.getSummeryJob().getUrl());

    SummaryRequestDto requestDto =
        SummaryRequestDto.builder().showLoading(true).tday(now.format(formatter)).build();

    Post summaryPost =
        Http.post(url, JsonHelper.toJsonString(requestDto))
            .header("Accept", Constants.APPLICATION_JSON)
            .header("Content-Type", Constants.APPLICATION_JSON)
            .header("authorization", "Bearer " + token);

    String summaryResponse = summaryPost.text();
    try {
      ResponseDto<SummeryDto> summaryResponseDto =
          getObjectMapper().readValue(summaryResponse, new TypeReference<>() {});

      lastRequest = LocalDateTime.now();

      return summaryResponseDto.getData();

    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSummeryJob().getIntervalInSeconds());
  }
}
