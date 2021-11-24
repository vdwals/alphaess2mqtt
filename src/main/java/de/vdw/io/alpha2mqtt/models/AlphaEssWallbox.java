package de.vdw.io.alpha2mqtt.models;

import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;

@Slf4j
public class AlphaEssWallbox extends Model {

  public static final String BATTERY = "battery";
  public static final String SN = "sn";

  public static AlphaEssWallbox create(AlphaEssBattery battery, String sn) {
    log.debug("Created new Wallbox: {}", sn);
    return findOrCreateIt(BATTERY, battery.getSn(), SN, sn);
  }

  public String getSn() {
    return getString(SN);
  }
}
