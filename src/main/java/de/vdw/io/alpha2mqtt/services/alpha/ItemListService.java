package de.vdw.io.alpha2mqtt.services.alpha;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.http.Get;
import org.javalite.http.Http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ItemListService extends AlphaService<SystemDto> {

  public ItemListService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  protected SystemDto requestNewData(String token, LocalDateTime now) {
    log.info("Load wallbox information.");
    Map<String, AlphaEssBattery> settingsUrl =
        Base.withDb(
            () -> {
              String url = AlphaEssLoadJob.getSettingsJob().getUrl();
              return AlphaEssBattery.findAll().stream()
                  .map(model -> (AlphaEssBattery) model)
                  .collect(
                      Collectors.toMap(
                          battery -> String.format(url, battery.getSystemId()),
                          battery -> battery));
            });

    if (settingsUrl.isEmpty()) return null;

    Map<AlphaEssBattery, List<WallboxDto>> batteryWallBoxMap =
        settingsUrl.entrySet().stream()
            .map(
                entry -> {
                  Get dataGet = RequestUtils.addHeader(Http.get(entry.getKey()), token);

                  if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
                    log.error(
                        "Unexpected response code while receiving items {}: {}",
                        dataGet.responseCode(),
                        dataGet.responseMessage());
                    return null;
                  }
                  String dataResponse = dataGet.text();

                  try {
                    ResponseDto<SystemDto> systemResponseDto =
                        getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

                    return new AbstractMap.SimpleEntry<>(
                        entry.getValue(), systemResponseDto.getData().getCharging_pile_list());

                  } catch (IOException e) {
                    log.error("Could not parse response.", e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Base.withDb(
        () -> {
          for (Map.Entry<AlphaEssBattery, List<WallboxDto>> entry : batteryWallBoxMap.entrySet()) {
            AlphaEssBattery battery = entry.getKey();

            for (WallboxDto wallboxDto : entry.getValue()) {
              AlphaEssWallbox.create(battery, wallboxDto.getChargingpile_sn());
            }
          }
          return null;
        });

    return null;
  }

  @Override
  public long getRefreshRate() {
    return Base.withDb(() -> AlphaEssLoadJob.getSettingsJob().getIntervalInSeconds());
  }
}
