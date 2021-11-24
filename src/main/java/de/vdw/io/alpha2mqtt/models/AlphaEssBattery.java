package de.vdw.io.alpha2mqtt.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;

@Cached
public class AlphaEssBattery extends Model {

  public static final String SN = "sn";

  public String getSn() {
    return getString(SN);
  }
}
