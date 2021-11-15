
package app.services;

import com.google.inject.AbstractModule;

/**
 * @author Stefano Crespi
 */
public class SummaryModule
	extends AbstractModule
{

	@Override
	protected void configure() {
		bind(ISummaryService.class).to(SummaryService.class).asEagerSingleton();
	}
}
