package de.vdw.io.alpha2mqtt.config;

import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class Constants {
  public static final String APPLICATION_JSON = "application/json";
  
  public static final long EXPIRE = TimeUnit.SECONDS.toSeconds(30);

  public static final long TIMEOUT = TimeUnit.SECONDS.toMillis(EXPIRE);
}
