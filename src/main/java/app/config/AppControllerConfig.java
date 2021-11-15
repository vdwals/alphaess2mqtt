
package app.config;

import app.controllers.SummaryController;
import org.javalite.activeweb.AbstractControllerConfig;
import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.controller_filters.DBConnectionFilter;

import app.filter.RestExceptionFilter;

/**
 * @author Stefano Crespi
 */
public class AppControllerConfig
	extends AbstractControllerConfig
{

	/**
	 * {@inheritDoc}
	 *
	 * @see org.javalite.activeweb.AppConfig#init(org.javalite.activeweb.AppContext)
	 */
	@Override
	public void init(AppContext context) {
		// RestException handling
		add(new RestExceptionFilter());
		// DB connection
		add(new DBConnectionFilter()).to(SummaryController.class);
	}
}
