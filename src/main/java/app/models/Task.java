
package app.models;

import static app.utils.Tokens.DESCRIPTION_TOKEN;

import org.javalite.activejdbc.Model;

/**
 * @author Stefano Crespi
 */
public class Task
	extends Model
{

	static {
		validatePresenceOf(DESCRIPTION_TOKEN).message(DESCRIPTION_TOKEN + " must be provided");
	}
}
