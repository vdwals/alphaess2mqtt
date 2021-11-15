
package app.models;

import static app.utils.Tokens.NAME_TOKEN;

import org.javalite.activejdbc.Model;

/**
 * @author Stefano Crespi
 */
public class User
	extends Model
{

	static {
		validatePresenceOf(NAME_TOKEN).message(NAME_TOKEN + " must be provided");
		validateRegexpOf(NAME_TOKEN, "^[A-Z]*$");
	}
}
