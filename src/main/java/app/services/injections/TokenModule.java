
package app.services.injections;

import app.services.TokenService;
import com.google.inject.AbstractModule;

/**
 * @author Stefano Crespi
 */
public class TokenModule
	extends AbstractModule
{

	@Override
	protected void configure() {
		bind(ITokenService.class).to(TokenService.class).asEagerSingleton();
	}
}
