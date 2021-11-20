package de.vdwals.io.alpha2mqtt.models;

import java.util.Map;
import java.util.stream.Collectors;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Cached;

@Cached
public class AlphaEssSetting extends Model {
    
    public static final String NAME = "name";
    public static final String VALUE = "value";
    
    public static Map<String, String> getSettings() {
        return findAll()
                .stream()
                .map(model -> (AlphaEssSetting) model)
                .collect(
                        Collectors
                                .toMap(
                                        setting -> setting.getString(NAME),
                                        setting -> setting.getString(VALUE)
                                )
                );
    }
}
