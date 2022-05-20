package de.vdw.io.alpha2mqtt.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import de.vdw.io.alpha2mqtt.models.api.BatteryDto;
import de.vdw.io.alpha2mqtt.models.api.WallboxDto;
import lombok.Value;

@Value
@Singleton
public class Cache {
  List<BatteryDto> batteries = Collections.synchronizedList(new LinkedList<>());

  Map<String, List<WallboxDto>> wallboxes = Collections.synchronizedMap(new LinkedHashMap<>());

  Map<String, String> systemIdMap = Collections.synchronizedMap(new LinkedHashMap<>());

  public Cache() {
    System.out.println();
  }

}
