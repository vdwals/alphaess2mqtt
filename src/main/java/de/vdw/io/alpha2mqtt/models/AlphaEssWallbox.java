package de.vdw.io.alpha2mqtt.models;

import lombok.extern.slf4j.Slf4j;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.IdName;

@Slf4j
@BelongsTo(foreignKeyName = "battery", parent = AlphaEssBattery.class)
@IdName("sn")
public class AlphaEssWallbox extends Model {

  public static final String BATTERY = "battery";
  public static final String SN = "sn";

  public static void create(AlphaEssBattery battery, String sn) {
    findOrCreateIt(BATTERY, battery.getSn(), SN, sn);
  }

  public String getSn() {
    return getString(SN);
  }
}
