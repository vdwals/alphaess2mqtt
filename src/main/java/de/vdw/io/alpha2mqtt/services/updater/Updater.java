package de.vdw.io.alpha2mqtt.services.updater;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Interface for all update services that need to be run at fixed rates.
 *
 * @author Dennis van der Wals
 *
 */
@Slf4j
public abstract class Updater implements Runnable {
  abstract void doUpdate();

  public abstract void init();

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        doUpdate();
        return;
      } catch (Exception e) {
        log.error("Exception in running thread: {}, restarting in 30s",
            this.getClass().getCanonicalName(), e);
        try {
          TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }
}
