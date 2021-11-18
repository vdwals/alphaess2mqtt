package app.config;

import app.controllers.RunningDataController;
import app.controllers.SummeryController;
import org.javalite.activeweb.AbstractControllerConfig;
import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.controller_filters.DBConnectionFilter;

@SuppressWarnings({"unchecked", "rawtypes"})
public class AppControllerConfig extends AbstractControllerConfig {
    
    public void init(AppContext context) {
        add(new DBConnectionFilter()).to(SummeryController.class);
        add(new DBConnectionFilter()).to(RunningDataController.class);
    }
}
