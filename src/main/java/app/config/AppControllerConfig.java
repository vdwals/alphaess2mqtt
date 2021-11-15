
package app.config;

import org.javalite.activeweb.AbstractControllerConfig;
import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.controller_filters.DBConnectionFilter;
import org.javalite.activeweb.controller_filters.TimingFilter;

import app.controllers.TasksController;
import app.controllers.UsersController;
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
		addGlobalFilters(new RestExceptionFilter());
		addGlobalFilters(new TimingFilter());
		// DB connection
		add(new DBConnectionFilter()).to(UsersController.class);
		add(new DBConnectionFilter()).to(TasksController.class);
	}
}
