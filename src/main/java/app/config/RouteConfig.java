
package app.config;

import org.javalite.activeweb.AbstractRouteConfig;
import org.javalite.activeweb.AppContext;

import app.controllers.TasksController;

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
		route("/users/{user_id}/tasks/{id}").to(TasksController.class).get().action("show");
		route("/users/{user_id}/tasks/{id}").to(TasksController.class).put().action("update");
		route("/users/{user_id}/tasks/{id}").to(TasksController.class).delete().action("destroy");
		route("/users/{user_id}/tasks").to(TasksController.class).get().action("index");
		route("/users/{user_id}/tasks").to(TasksController.class).post().action("create");
	}
}
