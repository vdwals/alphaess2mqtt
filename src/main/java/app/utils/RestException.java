
package app.utils;

/**
 * @author Stefano Crespi
 */
public class RestException
	extends Exception
{

	private final ErrorWrapper errorWrapper;

	/**
	 * @param errorWrapper the error wrapper object
	 */
	public RestException(ErrorWrapper errorWrapper) {
		super();
		this.errorWrapper = errorWrapper;
	}

	/**
	 * @return the error wrapper object
	 */
	public ErrorWrapper getErrorWrapper() {
		return errorWrapper;
	}

}
