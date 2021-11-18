package app.services.injections;

import app.services.SummeryService;
import com.google.inject.AbstractModule;

/**
 * @author Stefano Crespi
 */
public class SummeryModule
        extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ISummeryService.class).to(SummeryService.class).asEagerSingleton();
    }
}
