package de.vdw.io.alpha2mqtt.services.updater;

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
    boolean retry = false;
    do {
      try {
        doUpdate();
        retry = false;
      } catch (Exception e) { // timeout, network failure exceptions
        log.error("Exception in running thread: " + this.getClass().getCanonicalName()
            + ", restarting job");
        retry = true;
      }
    } while (retry);
  }
}
