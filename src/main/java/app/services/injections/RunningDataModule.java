package app.services.injections;

import app.services.RunningDataService;
import com.google.inject.AbstractModule;

/**
 * @author Stefano Crespi
 */
public class RunningDataModule
		extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(IRunningDataService.class).to(RunningDataService.class).asEagerSingleton();
	}
}
