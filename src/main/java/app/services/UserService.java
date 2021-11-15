
package app.services;

import app.models.User;
import app.models.UserDTO;

/**
 * @author Stefano Crespi
 */
public interface UserService {

	/**
	 * @param user the user object to transform
	 * @param resourceUrl the url of the resource to transform
	 * @return the transformed DTO
	 */
	UserDTO toDTO(User user, String resourceUrl);
}
