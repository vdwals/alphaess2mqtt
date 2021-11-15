
package app.utils;

/**
 * @author Stefano Crespi
 */
public class Util {

	/**
	 * @param errorWrapper the errorWrapper object
	 * @throws RestException if errors are present
	 */
	public static void checkErrorsAndRaiseException(final ErrorWrapper errorWrapper)
		throws RestException
	{
		if (errorWrapper.hasErrors()) {
			throw new RestException(errorWrapper);
		}
	}
}
