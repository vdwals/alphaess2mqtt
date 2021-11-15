
package app.services;

import org.springframework.hateoas.Link;

import app.models.User;
import app.models.UserDTO;

/**
 * @author Stefano Crespi
 */
public class UserServiceImpl
	implements UserService
{

	/**
	 * {@inheritDoc}
	 *
	 * @see app.services.UserService#toDTO(app.models.User, java.lang.String)
	 */
	public UserDTO toDTO(User user, String resourceUrl) {
		UserDTO retVal = new UserDTO(user);
		retVal.addLink(new Link(resourceUrl));
		retVal.addLink(new Link("rel", resourceUrl + "/tasks"));
		return retVal;
	}

	// private Map<Integer, User> _users = new HashMap<Integer, User>();
	//
	// public void addUser(User user) {
	// _users.put(user.getInteger(ID_TOKEN), user);
	// }
	//
	// public User getUserById(int id) {
	// return _users.get(id);
	// }
	//
	// public void editUser(User user) {
	// final User toEdit = _users.get(user.getInteger(ID_TOKEN));
	// if (toEdit != null) {
	// toEdit.set(NAME_TOKEN, user.get(NAME_TOKEN));
	// }
	// }
	//
	// public void deleteUser(int id) {
	// if (_users.containsKey(id)) {
	// _users.remove(id);
	// }
	// }
}
