
package app.controllers;

import static app.utils.Tokens.APPLICATION_JSON;
import static app.utils.Tokens.DESCRIPTION_TOKEN;
import static app.utils.Tokens.ID_TOKEN;
import static app.utils.Tokens.USER_ID_TOKEN;
import static app.utils.Util.checkErrorsAndRaiseException;

import java.io.IOException;

import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.AppController;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.POST;
import org.javalite.activeweb.annotations.PUT;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import app.models.Task;
import app.models.User;
import app.utils.Error;
import app.utils.ErrorWrapper;
import app.utils.JsonHelper;
import app.utils.RestException;

/**
 * @author Stefano Crespi
 */
public class TasksController
	extends AppController
{

	private static final String NOT_FOUND_DESCRIPTION = " not found for id: ";

	/**
	 * POST | /users/user_id/tasks create create a new book
	 *
	 * @throws RestException
	 */
	@POST
	public void create()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();

		final User user = (User) User.findById(param(USER_ID_TOKEN));
		if (user == null) {
			handleUserNotFound(errorWrapper, param(USER_ID_TOKEN));
		}

		String input = "";
		try {
			input = getRequestString();
		} catch (IOException e) {
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Error retrieving request content"));
		}

		checkErrorsAndRaiseException(errorWrapper);

		if (StringUtils.isEmpty(input)) {
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Error parsing request content"));
		} else {
			Task task = new Task();
			task.fromMap(JsonHelper.toMap(input));
			if (task.isValid()) {
				user.add(task);
				view("task", task);
				render("show").noLayout().contentType(APPLICATION_JSON).status(HttpStatus.CREATED.value());
			} else {
				errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Invalid request content"));
			}
		}
		checkErrorsAndRaiseException(errorWrapper);
	}

	/**
	 * DELETE | /users/user_id/tasks/id destroy delete a specific user
	 *
	 * @throws RestException
	 */
	@DELETE
	public void destroy()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();

		final User user = (User) User.findById(param(USER_ID_TOKEN));
		if (user == null) {
			handleUserNotFound(errorWrapper, param(USER_ID_TOKEN));
		}

		checkErrorsAndRaiseException(errorWrapper);

		Task existingTask = null;
		final LazyList<Task> tasks = user.get(Task.class, ID_TOKEN + " = ?", param(ID_TOKEN));
		if (tasks.isEmpty()) {
			handleTaskNotFound(errorWrapper, param(ID_TOKEN), param(USER_ID_TOKEN));
		} else {
			existingTask = tasks.get(0);
		}

		checkErrorsAndRaiseException(errorWrapper);

		existingTask.delete();
		respond("").status(HttpStatus.OK.value());
	}

	/**
	 * GET | /users/user_id/tasks index display a list of all tasks
	 *
	 * @throws RestException
	 */
	@GET
	public void index()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();
		final User user = (User) User.findById(param(USER_ID_TOKEN));
		if (user == null) {
			handleUserNotFound(errorWrapper, param(USER_ID_TOKEN));
		}

		checkErrorsAndRaiseException(errorWrapper);

		view("tasks", user.getAll(Task.class));
		render().noLayout().contentType(APPLICATION_JSON);
	}

	/**
	 * GET | /users/user_id/tasks/id show display a specific task
	 *
	 * @throws RestException
	 */
	@GET
	public void show()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();
		final User user = (User) User.findById(param(USER_ID_TOKEN));
		if (user == null) {
			handleUserNotFound(errorWrapper, param(USER_ID_TOKEN));
		}

		checkErrorsAndRaiseException(errorWrapper);

		final LazyList<Task> tasks = user.get(Task.class, ID_TOKEN + " = ?", param(ID_TOKEN));
		if (tasks.isEmpty()) {
			handleTaskNotFound(errorWrapper, param(ID_TOKEN), param(USER_ID_TOKEN));
		} else {
			view("task", tasks.get(0));
			render().noLayout().contentType(APPLICATION_JSON);
		}

		checkErrorsAndRaiseException(errorWrapper);
	}

	/**
	 * PUT /users/:id update update a specific user
	 *
	 * @throws RestException
	 */
	@PUT
	public void update()
		throws RestException
	{
		final ErrorWrapper errorWrapper = new ErrorWrapper();

		final User user = (User) User.findById(param(USER_ID_TOKEN));
		if (user == null) {
			handleUserNotFound(errorWrapper, param(USER_ID_TOKEN));
		}

		checkErrorsAndRaiseException(errorWrapper);

		Task existingTask = null;
		final LazyList<Task> tasks = user.get(Task.class, ID_TOKEN + " = ?", param(ID_TOKEN));
		if (tasks.isEmpty()) {
			handleTaskNotFound(errorWrapper, param(ID_TOKEN), param(USER_ID_TOKEN));
		} else {
			existingTask = tasks.get(0);
		}

		String input = "";
		try {
			input = getRequestString();
		} catch (IOException e) {
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Error retrieving request content"));
		}

		checkErrorsAndRaiseException(errorWrapper);

		if (StringUtils.isEmpty(input)) {
			errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Error parsing request content"));
		} else {
			Task task = new Task();
			task.fromMap(JsonHelper.toMap(input));
			if (task.isValid()) {
				existingTask.setString(DESCRIPTION_TOKEN, task.getString(DESCRIPTION_TOKEN));
				existingTask.saveIt();
				view("task", existingTask);
				render("show").noLayout().contentType(APPLICATION_JSON).status(HttpStatus.CREATED.value());
			} else {
				errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), "Invalid request content"));
			}
		}

		checkErrorsAndRaiseException(errorWrapper);
	}

	/**
	 *
	 */
	private void handleNotFound(final ErrorWrapper errorWrapper, final String description) {
		errorWrapper.addError(new Error(HttpStatus.BAD_REQUEST.value(), description));
	}

	private void handleTaskNotFound(final ErrorWrapper errorWrapper, final String id, final String userId) {
		handleNotFound(errorWrapper, "Task" + NOT_FOUND_DESCRIPTION + id + ", user_id: " + userId);
	}

	private void handleUserNotFound(final ErrorWrapper errorWrapper, final String id) {
		handleNotFound(errorWrapper, "User" + NOT_FOUND_DESCRIPTION + id);
	}

}
