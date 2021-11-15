
package app.filter;

import static app.utils.JsonHelper.toJson;
import static app.utils.Tokens.APPLICATION_JSON;

import org.javalite.activeweb.controller_filters.HttpSupportFilter;
import org.springframework.http.HttpStatus;

import app.utils.Error;
import app.utils.ErrorWrapper;
import app.utils.RestException;

/**
 * @author Stefano Crespi
 */
public class RestExceptionFilter
	extends HttpSupportFilter
{

	/**
	 * {@inheritDoc}
	 *
	 * @see org.javalite.activeweb.controller_filters.HttpSupportFilter#onException(java.lang.Exception)
	 */
	@Override
	public void onException(Exception e) {
		if (e.getCause() instanceof RestException) {
			// log exception
			respond(toJson(((RestException) e.getCause()).getErrorWrapper())).contentType(APPLICATION_JSON).status(HttpStatus.BAD_REQUEST.value());
		} else {
			final ErrorWrapper errorWrapper = new ErrorWrapper();
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
			respond(toJson(errorWrapper)).contentType(APPLICATION_JSON).status(HttpStatus.BAD_REQUEST.value());
		}
	}
}
