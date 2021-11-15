
package app.services;

import com.google.inject.AbstractModule;

/**
 * @author Stefano Crespi
 */
public class UserModule
	extends AbstractModule
{

	@Override
	protected void configure() {
		bind(UserService.class).to(UserServiceImpl.class).asEagerSingleton();
	}
}
