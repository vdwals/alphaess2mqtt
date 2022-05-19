package de.vdw.io.alpha2mqtt;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.javalite.activejdbc.connection_config.ConnectionJdbcConfig;
import org.javalite.activejdbc.connection_config.DBConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vdw.io.alpha2mqtt.services.RunningDataUpdateService;
import de.vdw.io.alpha2mqtt.services.SettingsUpdateService;
import de.vdw.io.alpha2mqtt.services.SummeryDataUpdateService;
import de.vdw.io.alpha2mqtt.services.alpha.ChargingService;
import de.vdw.io.alpha2mqtt.services.alpha.SettingService;
import de.vdw.io.alpha2mqtt.services.ha.BatteryDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.InverterDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.SolarModuleDeviceService;
import de.vdw.io.alpha2mqtt.services.ha.WallBoxDeviceService;
import de.vdw.it.hamqtt.HomeAssistantMQTTService;
import de.vdw.it.hamqtt.utils.ServiceFactory;
import eu.lestard.easydi.EasyDI;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Value
public class App {

  public static void main(String[] args) throws MqttException {
    Map<String, String> environmentVariables = System.getenv();

    log.info("Init database connection");
    ConnectionJdbcConfig connectionConfig =
        new ConnectionJdbcConfig(
            environmentVariables.get("ACTIVEJDBC.DRIVER"),
            environmentVariables.get("ACTIVEJDBC.URL"),
            environmentVariables.get("ACTIVEJDBC.USER"),
            environmentVariables.get("ACTIVEJDBC.PASSWORD"));
    connectionConfig.setEnvironment("development");
    DBConfiguration.addConnectionConfig(connectionConfig);

    EasyDI ed = new EasyDI();
    ed.bindInstance(EasyDI.class, ed);

    ed.bindInstance(ObjectMapper.class, new ObjectMapper());

    log.info("Connect to MQTT-Broker");
    HomeAssistantMQTTService homeAssistantMQTTService =
        ServiceFactory.createHomeAssistantMQTTService(
            environmentVariables.get("MQTT_HOST"),
            environmentVariables.get("MQTT_PORT"),
            environmentVariables.get("MQTT_USERNAME"),
            environmentVariables.get("MQTT_PASSWORD").toCharArray(),
            "alpha_energy",
            environmentVariables.getOrDefault("MQTT_DISCOVERY_TOPIC", "homeassistant"),
            "Alpha ESS Proxy", environmentVariables.getOrDefault("MQTT_PROTOCOLL", "tcp"));

    ed.bindInstance(HomeAssistantMQTTService.class, homeAssistantMQTTService);

    ed.bindInstance(ScheduledExecutorService.class, Executors.newSingleThreadScheduledExecutor());

    App app = ed.getInstance(App.class);

    app.init();
    app.start();
  }

  BatteryDeviceService batteryDeviceService;

  SolarModuleDeviceService solarModuleDeviceService;

  InverterDeviceService inverterDeviceService;

  WallBoxDeviceService wallboxDeviceService;

  ChargingService chargingService;

  HomeAssistantMQTTService mqttService;

  ScheduledExecutorService scheduledExecutorService;

  EasyDI easyDI;

  public void init() {
    this.mqttService.connect();

    this.mqttService.addDevice(this.batteryDeviceService.getDevice());
    this.mqttService.addDevice(this.solarModuleDeviceService.getDevice());
    this.mqttService.addDevice(this.inverterDeviceService.getDevice());
    this.mqttService.addDevice(this.wallboxDeviceService.getDevice());

    this.mqttService.addCommandListener(this.chargingService);

    this.mqttService.publishConfigs();
  }

  public void start() {
    this.easyDI.getInstance(SettingService.class).getData();

    RunningDataUpdateService runningDataUpdateService =
        this.easyDI.getInstance(RunningDataUpdateService.class);
    runningDataUpdateService.init();

    SummeryDataUpdateService summeryDataUpdateService =
        this.easyDI.getInstance(SummeryDataUpdateService.class);
    summeryDataUpdateService.init();

    SettingsUpdateService settingUpdateService =
        this.easyDI.getInstance(SettingsUpdateService.class);
    settingUpdateService.init();
  }
}
