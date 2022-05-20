package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Singleton;
import org.javalite.http.Get;
import org.javalite.http.Http;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.Cache;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.SystemIdDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
public class SystemService extends AlphaService<List<BatteryDto>> {
  Cache cache;

  public SystemService(ObjectMapper objectMapper, TokenService tokenService, Cache cache) {
    super(objectMapper, tokenService);
    this.cache = cache;

    List<BatteryDto> batteries = getData();

    if (batteries != null) {
      cache.getBatteries().addAll(batteries);
    }

    boolean systemIdsLoadded = getSystemIds();

    if (systemIdsLoadded) {
      cache.getBatteries().forEach(battery -> requestWallboxes(battery.getSys_sn()));
    }
  }

  @Override
  public long getRefreshRate() {
    return 0;
  }

  private boolean getSystemIds() {
    log.info("Receive system ids");
    String token = tokenService.getToken();

    Get dataGet = RequestUtils.addHeader(
        Http.get(Constants.systemIdUrl, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    List<SystemIdDto> systemIds = null;

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving system Ids {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());

      return false;
    }

    String listResponse = dataGet.text();
    try {
      ResponseDto<List<SystemIdDto>> responseDto = getObjectMapper().readValue(listResponse,
          new TypeReference<ResponseDto<List<SystemIdDto>>>() {});

      log.trace("Response: {}", responseDto);

      systemIds = responseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving system Ids:", e);
      return false;
    }

    if (systemIds == null) {
      return false;
    }

    systemIds.forEach(systemIdDto -> cache.getSystemIdMap().put(systemIdDto.getSys_sn(),
        systemIdDto.getSystem_id()));

    return true;
  }


  @Override
  protected List<BatteryDto> requestNewData(String token, LocalDateTime now) {
    log.info("Load list of Alpha devices.");

    Get dataGet = RequestUtils.addHeader(
        Http.get(Constants.batteriesUrl, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving system Ids {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());

      return null;
    }

    String listResponse = dataGet.text();
    try {
      ResponseDto<List<BatteryDto>> responseDto = getObjectMapper().readValue(listResponse,
          new TypeReference<ResponseDto<List<BatteryDto>>>() {});

      log.trace("Response: {}", responseDto);

      return responseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving battery list:", e);
      return null;
    }
  }

  private void requestWallboxes(String sn) {
    log.info("Load list of Wallboxes for sn: {}.", sn);

    String token = tokenService.getToken();
    String url = String.format(Constants.getSettingUrl, cache.getSystemIdMap().get(sn));

    Get dataGet = RequestUtils
        .addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    SystemDto systemDto = null;
    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving system setting {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());

      return;
    }

    String listResponse = dataGet.text();
    try {
      ResponseDto<SystemDto> responseDto =
          getObjectMapper().readValue(listResponse, new TypeReference<>() {});

      log.trace("Response: {}", responseDto);

      systemDto = responseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving system setting:", e);
      return;
    }

    if (systemDto != null) {
      List<WallboxDto> wallboxes = new LinkedList<>(systemDto.getCharging_pile_list());
      cache.getWallboxes().merge(sn, wallboxes, (existingList, newList) -> Stream
          .concat(existingList.stream(), newList.stream()).collect(Collectors.toList()));
    }
  }

}
