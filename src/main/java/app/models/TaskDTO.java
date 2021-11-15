
package app.models;

import static app.utils.Tokens.DESCRIPTION_TOKEN;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Link;

/**
 * @author Stefano Crespi
 */
public class TaskDTO {

	private int id;

	private String description;

	private final List<Link> links;

	/**
	 * @param user
	 */
	public TaskDTO(User user) {
		super();
		setId((Integer) user.getId());
		setDescription(user.getString(DESCRIPTION_TOKEN));
		links = new ArrayList<Link>();
	}

	/**
	 * @param link the link to add
	 */
	public void addLink(Link link) {
		links.add(link);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the links
	 */
	public List<Link> getlinks() {
		return links;
	}

	/**
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
}
