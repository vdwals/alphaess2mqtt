
package app.config;

import app.controllers.SummaryController;
import org.javalite.activeweb.AbstractRouteConfig;
import org.javalite.activeweb.AppContext;

/**
 * @author Stefano Crespi
 */
public class RouteConfig
	extends AbstractRouteConfig
{

	/**
	 * {@inheritDoc}
	 *
	 * @see org.javalite.activeweb.AppConfig#init(org.javalite.activeweb.AppContext)
	 */
	@Override
	public void init(AppContext appContext) {
		route("/test").to(SummaryController.class);
	}
}
