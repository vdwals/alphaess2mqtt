
package app.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefano Crespi
 */
public class ErrorWrapper {

	private final List<Error> errors;

	/**
	 *
	 */
	public ErrorWrapper() {
		super();
		errors = new ArrayList<Error>();
	}

	/**
	 * @param error the error to add
	 */
	public void addError(final Error error) {
		errors.add(error);
	}

	/**
	 * @return the list of errors
	 */
	public List<Error> getErrors() {
		return errors;
	}

	/**
	 * @return whether errors are present
	 */
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

}
