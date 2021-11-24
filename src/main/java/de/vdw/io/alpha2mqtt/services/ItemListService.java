package de.vdw.io.alpha2mqtt.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.models.AlphaEssBattery;
import de.vdw.io.alpha2mqtt.models.AlphaEssLoadJob;
import de.vdw.io.alpha2mqtt.models.AlphaEssWallbox;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.services.alpha.TokenService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Base;
import org.javalite.http.Get;
import org.javalite.http.Http;

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

    Map<AlphaEssBattery, List<WallboxDto>> batteryWallboxMap =
        settingsUrl.entrySet().stream()
            .map(
                entry -> {
                  Get dataGet = addHeader(Http.get(entry.getKey()), token);

                  String dataResponse = dataGet.text();

                  try {
                    ResponseDto<SystemDto> systemResponseDto =
                        getObjectMapper().readValue(dataResponse, new TypeReference<>() {});

                    return new AbstractMap.SimpleEntry<>(
                        entry.getValue(), systemResponseDto.getData().getCharging_pile_list());

                  } catch (IOException e) {
                    e.printStackTrace();
                  }

                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Base.withDb(
        () -> {
          for (Map.Entry<AlphaEssBattery, List<WallboxDto>> entry : batteryWallboxMap.entrySet()) {
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
