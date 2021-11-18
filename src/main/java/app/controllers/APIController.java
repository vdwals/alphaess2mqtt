package app.controllers;

import app.util.Tokens;
import org.javalite.activeweb.AppController;
import org.javalite.common.JsonHelper;
import org.springframework.http.HttpStatus;

/**
 * @author Igor Polevoy on 10/28/14.
 */
public abstract class APIController extends AppController {
    @Override
    protected String getContentType() {
        return Tokens.APPLICATION_JSON;
    }
    
    @Override
    protected String getLayout() {
        return null;
    }
    
    protected void respondWithJson(Object responseObject) {
        respond(JsonHelper.toJsonString(responseObject)).contentType(getContentType())
                .status(HttpStatus.OK.value());
    }
}
