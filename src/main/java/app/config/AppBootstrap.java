package app.config;

import app.services.injections.RunningDataModule;
import app.services.injections.SummeryModule;
import app.services.injections.TokenModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.Bootstrap;

public class AppBootstrap extends Bootstrap {
    public void init(AppContext context) {        
    }
    
    public Injector getInjector() {
        return Guice.createInjector(new SummeryModule(),
                                    new TokenModule(),
                                    new RunningDataModule());
    }
}
