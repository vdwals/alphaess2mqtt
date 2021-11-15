
package app.config;

import app.services.SummaryModule;
import com.google.inject.Injector;
import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.Bootstrap;

import com.google.inject.Guice;

/**
 * @author Stefano Crespi
 */
public class AppBootstrap
	extends Bootstrap
{

	/**
	 * {@inheritDoc}
	 *
	 * @see org.javalite.activeweb.Bootstrap#init(org.javalite.activeweb.AppContext)
	 */
	@Override
	public void init(AppContext context) {}

	public Injector getInjector(){
		return Guice.createInjector(new SummaryModule());
	}
}
