
package app.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Stefano Crespi
 */
public class JsonHelper {

	/**
	 * Deserializes object from JSON
	 *
	 * @param json
	 * @return the created map
	 * @throws IOException
	 */
	public static Map<String, Object> fromJson(String json)
		throws IOException
	{
		return new ObjectMapper().readValue(json, HashMap.class);
	}

	/**
	 * Builds a list form JSON
	 *
	 * @param json
	 * @return the created list
	 * @throws IOException
	 */
	public static List<Map<String, Object>> fromJsonToList(String json)
		throws IOException
	{
		return new ObjectMapper().readValue(json, List.class);

	}

	/**
	 * Serializes object from JSON
	 *
	 * @param object
	 * @return the json representation
	 */
	public static String toJson(Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (IOException ex) {
			// Log exception
			return null;
		}
	}

	/**
	 * @param json
	 * @return the created map
	 */
	public static Map toMap(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param json
	 * @return the array of created maps
	 */
	public static Map[] toMaps(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, Map[].class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
