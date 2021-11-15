
package app.controllers;

import static app.utils.JsonHelper.toJson;
import static app.utils.Tokens.APPLICATION_JSON;
import static app.utils.Tokens.CREATED_AT_TOKEN;
import static app.utils.Tokens.ID_TOKEN;
import static app.utils.Tokens.NAME_TOKEN;
import static app.utils.Util.checkErrorsAndRaiseException;

import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.AppController;
import org.javalite.activeweb.annotations.RESTful;
import org.springframework.http.HttpStatus;

import app.models.User;
import app.services.UserService;
import app.utils.Error;
import app.utils.ErrorWrapper;
import app.utils.RestException;

import com.google.inject.Inject;

/**
 * @author Stefano Crespi
 */
@RESTful
public class UsersController
	extends AppController
{

	private static final String NOT_FOUND_DESCRIPTION = "User not found";

	private UserService _userService;

	/**
	 * POST | /users create create a new user
	 *
	 * @throws RestException
	 */
	public void create()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();
		final User user = new User();
		user.fromMap(params1st());

		if (!user.isValid()) {
			for (final String key : user.errors().keySet()) {
				errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), user.errors().get(key)));
			}
		}

		checkErrorsAndRaiseException(errorWrapper);

		user.saveIt();
		respond(user.toJson(false, ID_TOKEN, NAME_TOKEN, CREATED_AT_TOKEN)).status(201);
	}

	/**
	 * DELETE | /users/id destroy delete a specific user
	 *
	 * @throws RestException
	 */
	public void destroy()
		throws RestException
	{
		final User user = (User) User.findById(param(ID_TOKEN));
		if (user == null) {
			handleNotFound();
		} else {
			user.delete(true);
			respond("").status(HttpStatus.OK.value());
		}
	}

	/**
	 * GET /users/:id/edit_form edit_form return an HTML form for editing a
	 * photo
	 *
	 * @throws RestException
	 */
	public void editForm()
		throws RestException
	{
		final User user = (User) User.findById(param(ID_TOKEN));
		if (user == null) {
			handleNotFound();
		} else {
			view("user", user);
		}
	}

	/**
	 * GET | /users index display a list of all users
	 */
	public void index() {
		LazyList<User> users = User.findAll();
		respond(users.toJson(false, ID_TOKEN, NAME_TOKEN, CREATED_AT_TOKEN)).contentType(APPLICATION_JSON);
	}

	/**
	 * GET | /users/new_form new_form return an HTML form for creating a new
	 * user
	 */
	public void newForm() {
		// nothing to do here, just default view delegation
	}

	/**
	 * @param userService the user service to inject
	 */
	@Inject
	public void setUserService(UserService userService) {
		_userService = userService;
	}

	/**
	 * GET | /users/id show display a specific user
	 *
	 * @throws RestException
	 */
	public void show()
		throws RestException
	{
		final User user = (User) User.findById(param(ID_TOKEN));
		if (user == null) {
			handleNotFound();
		} else {
			respond(toJson(_userService.toDTO(user, url()))).contentType(APPLICATION_JSON);
		}
	}

	/**
	 * PUT /users/:id update update a specific user
	 *
	 * @throws RestException
	 */
	public void update()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();
		final User user = new User();
		user.fromMap(params1st());

		if (!user.isValid()) {
			for (final String key : user.errors().keySet()) {
				errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), user.errors().get(key)));
			}
		}

		final User existingUser = (User) User.findById(param(ID_TOKEN));
		if (existingUser == null) {
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), NOT_FOUND_DESCRIPTION));
		}

		checkErrorsAndRaiseException(errorWrapper);

		existingUser.setString(NAME_TOKEN, user.getString(NAME_TOKEN));
		existingUser.saveIt();
		respond(existingUser.toJson(false, ID_TOKEN, NAME_TOKEN, CREATED_AT_TOKEN)).status(201);
	}

	/**
	 * @throws RestException
	 */
	private void handleNotFound()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();
		errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), NOT_FOUND_DESCRIPTION));
		checkErrorsAndRaiseException(errorWrapper);
	}

}
