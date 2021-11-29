package de.vdw.io.alpha2mqtt.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;
import org.javalite.activejdbc.annotations.IdName;

@Cached
@IdName("sn")
public class AlphaEssBattery extends Model {

  public static final String SN = "sn";
  public static final String SYSTEM_ID = "systemId";

  public String getSn() {
    return getString(SN);
  }

  public String getSystemId() {
    return getString(SYSTEM_ID);
  }

  public Double getUsableCapacity() {
    return getDouble("surpluscobat");
  }
}
