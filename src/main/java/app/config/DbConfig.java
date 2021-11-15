
package app.config;

import org.javalite.activeweb.AbstractDBConfig;
import org.javalite.activeweb.AppContext;

/**
 * @author Stefano Crespi
 */
public class DbConfig
        extends AbstractDBConfig {

    /**
     * {@inheritDoc}
     *
     * @see org.javalite.activeweb.AppConfig#init(org.javalite.activeweb.AppContext)
     */
    @Override
    public void init(AppContext context) {
        environment("development").jdbc("com.mysql.jdbc.Driver", "jdbc:mariadb://192.168.11.50:3308/kettle", "kettle", "ONMW2yYgTJOVopWKnPKj");
    }
}
