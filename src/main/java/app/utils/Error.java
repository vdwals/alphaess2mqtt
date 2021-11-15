
package app.utils;

/**
 * @author Stefano Crespi
 */
public class Error {

	private final int code;

	private final String description;

	/**
	 * @param code the error code
	 * @param description the error description
	 */
	public Error(int code, String description) {
		super();
		this.code = code;
		this.description = description;
	}

	/**
	 * @return the error code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @return the error description
	 */
	public String getDescription() {
		return description;
	}

}
