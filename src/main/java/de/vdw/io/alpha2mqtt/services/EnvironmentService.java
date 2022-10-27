package de.vdw.io.alpha2mqtt.services;

import java.security.InvalidParameterException;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import de.vdw.io.alpha2mqtt.models.Credentials;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnvironmentService {
  static String DEFAULT_PROTOCOLL = "tcp";
  static String DEFAULT_DISCOVERY_TOPIC = "homeassistant";
  static String DEFAULT_TOPIC = "alpha_energy";
  static String ALPHA_PASSWORD = "ALPHA.PASSWORD";
  static String ALPHA_USERNAME = "ALPHA.USERNAME";
  static String MQTT_PROTOCOLL = "MQTT.PROTOCOLL";
  static String MQTT_DISCOVERY_TOPIC = "MQTT.DISCOVERY_TOPIC";
  static String MQTT_TOPIC = "MQTT.TOPIC";
  static String MQTT_PASSWORD = "MQTT.PASSWORD";
  static String MQTT_USERNAME = "MQTT.USERNAME";
  static String MQTT_HOST = "MQTT.HOST";
  static String MQTT_PORT = "MQTT.PORT";
  static String ALPHA_MIN_WAIT = "ALPHA.MIN_WAIT_S";

  @Getter
  private Credentials credentials;

  @Getter
  long intervall;

  Map<String, String> environmentVariables;

  public EnvironmentService() {
    environmentVariables = System.getenv();

    if (environmentVariables.containsKey("LOG_LEVEL")) {
      switch (environmentVariables.get("LOG_LEVEL")) {
        case "TRACE":
          Configurator.setRootLevel(Level.TRACE);
          log.trace("Trace level logging activated");
          break;
        case "DEBUG":
          Configurator.setRootLevel(Level.DEBUG);
          log.trace("Debug level logging activated");
          break;
        default:
          log.warn("Unexpected value: " + environmentVariables.get("LOG_LEVEL"));
      }
    }

    if ((!environmentVariables.containsKey(ALPHA_USERNAME)
        || !environmentVariables.containsKey(ALPHA_PASSWORD)))
      throw new InvalidParameterException("Alpha Cloud credentials missing");

    credentials = new Credentials(environmentVariables.get(ALPHA_USERNAME),
        environmentVariables.get(ALPHA_PASSWORD));

    String envIntervalLength = System.getenv(ALPHA_MIN_WAIT);
    long i = 0;
    if (envIntervalLength != null) {
      try {
        i = Long.parseLong(envIntervalLength);
      } catch (Exception e) {
        log.error("Could not parse environment variable " + ALPHA_MIN_WAIT + " with value "
            + envIntervalLength, e);
      }
    }

    intervall = i;
  }

  public String mqttDiscoveryTopic() {
    return environmentVariables.getOrDefault(MQTT_DISCOVERY_TOPIC, DEFAULT_DISCOVERY_TOPIC);
  }

  public String mqttHost() {
    return environmentVariables.get(MQTT_HOST);
  }

  public char[] mqttPassword() {
    String pw = environmentVariables.get(MQTT_PASSWORD);
    if (pw == null)
      return new char[0];
    return pw.toCharArray();
  }

  public String mqttPort() {
    return environmentVariables.get(MQTT_PORT);
  }

  public String mqttProtocoll() {
    return environmentVariables.getOrDefault(MQTT_PROTOCOLL, DEFAULT_PROTOCOLL);
  }

  public String mqttTopic() {
    return environmentVariables.getOrDefault(MQTT_TOPIC, DEFAULT_TOPIC);
  }

  public String mqttUsername() {
    return environmentVariables.get(MQTT_USERNAME);
  }

}
