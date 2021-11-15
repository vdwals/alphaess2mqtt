
package app.config;

import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.Bootstrap;

import app.services.UserModule;

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
	public void init(AppContext context) {
		setInjector(Guice.createInjector(new UserModule()));
	}
}
