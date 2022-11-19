package de.vdw.io.alpha2mqtt.services.alpha.get;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import org.javalite.http.Get;
import org.javalite.http.Http;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.config.Constants;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.ChargingPileDto;
import de.vdw.io.alpha2mqtt.models.api.ResponseDto;
import de.vdw.io.alpha2mqtt.models.api.SystemDto;
import de.vdw.io.alpha2mqtt.models.api.SystemIdDto;
import de.vdw.io.alpha2mqtt.utils.RequestUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@Value
@EqualsAndHashCode(callSuper = true)
/**
 * Class for API call for available systems
 *
 * @author Dennis van der Wals
 *
 */
public class SystemService extends AlphaService<List<BatteryDto>> {
  public SystemService(ObjectMapper objectMapper, TokenService tokenService) {
    super(objectMapper, tokenService);
  }

  @Override
  public long getRefreshRate() {
    return 0;
  }

  /**
   * Returns a list of system Ids with their matching system sn.
   *
   * @return List of system ids
   */
  public List<SystemIdDto> getSystemIds() {
    log.info("Receive system ids");
    String token = tokenService.getToken();

    Get dataGet = RequestUtils.addHeader(
        Http.get(Constants.systemIdUrl, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    List<SystemIdDto> systemIds = null;

    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving system Ids {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());

      return null;
    }

    String listResponse = dataGet.text();
    try {
      ResponseDto<List<SystemIdDto>> responseDto = getObjectMapper().readValue(listResponse,
          new TypeReference<ResponseDto<List<SystemIdDto>>>() {});

      log.trace("Response: {}", responseDto);

      systemIds = responseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving system Ids:", e);
      return null;
    }

    return systemIds;
  }


  /**
   * Retrieves a list of charging piles connected to the system
   *
   * @param sn System sn
   * @param systemId System id
   * @return List of charging piles
   */
  public List<ChargingPileDto> requestChargingPiles(String sn, String systemId) {
    log.info("Load list of charging piles for sn: {}.", sn);

    String token = tokenService.getToken();
    String url = String.format(Constants.getSettingUrl, systemId);

    Get dataGet = RequestUtils
        .addHeader(Http.get(url, (int) Constants.TIMEOUT, (int) Constants.TIMEOUT), token);

    SystemDto systemDto = null;
    if (dataGet.responseCode() != HttpURLConnection.HTTP_OK) {
      log.error("Unexpected response code while receiving system setting {}: {}",
          dataGet.responseCode(), dataGet.responseMessage());

      return Collections.emptyList();
    }

    String listResponse = dataGet.text();
    try {
      ResponseDto<SystemDto> responseDto =
          getObjectMapper().readValue(listResponse, new TypeReference<>() {});

      log.trace("Response: {}", responseDto);

      systemDto = responseDto.getData();

    } catch (IOException e) {
      log.error("Error receiving system setting:", e);
      log.error("Parsing error for SystemDto: {}", listResponse);
      return Collections.emptyList();
    }

    if (systemDto != null) {
      return systemDto.getCharging_pile_list();
    }
    return Collections.emptyList();
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

}
