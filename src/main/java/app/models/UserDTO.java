
package app.models;

import static app.utils.Tokens.NAME_TOKEN;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Link;

/**
 * @author Stefano Crespi
 */
public class UserDTO {

	private int id;

	private String name;

	private final List<Link> links;

	/**
	 * @param user
	 */
	public UserDTO(User user) {
		super();
		setId((Integer) user.getId());
		setName(user.getString(NAME_TOKEN));
		links = new ArrayList<Link>();
	}

	/**
	 * @param link the link to add
	 */
	public void addLink(Link link) {
		links.add(link);
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
}
