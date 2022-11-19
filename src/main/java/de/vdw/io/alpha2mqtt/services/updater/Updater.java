package de.vdw.io.alpha2mqtt.services.updater;

/**
 * Interface for all update services that need to be run at fixed rates.
 *
 * @author Dennis van der Wals
 *
 */
public interface Updater extends Runnable {
  void init();

}
