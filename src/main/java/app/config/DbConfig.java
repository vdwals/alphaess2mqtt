
package app.config;

import org.javalite.activeweb.AbstractDBConfig;
import org.javalite.activeweb.AppContext;

/**
 * @author Stefano Crespi
 */
public class DbConfig
	extends AbstractDBConfig
{

	/**
	 * {@inheritDoc}
	 *
	 * @see org.javalite.activeweb.AppConfig#init(org.javalite.activeweb.AppContext)
	 */
	@Override
	public void init(AppContext context) {

		environment("development").jdbc("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/activerest_development", "root", "root");

		environment("development").testing().jdbc("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/activerest_test", "root", "root");

		environment("production").jndi("jdbc/activerest_production");
	}
}
