package app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.webapp.WebAppContext;

public class Main {
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        WebAppContext webapp = new WebAppContext("src/main/webapp", "/");
        webapp.addAliasCheck(new AllowSymLinkAliasChecker());
        server.setHandler(webapp);
        server.start();
        server.dumpStdErr();
        server.join();
    }
}
