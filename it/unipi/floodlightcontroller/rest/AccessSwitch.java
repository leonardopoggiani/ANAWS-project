package it.unipi.floodlightcontroller.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Class managing the resource "/access-switches/json".
 */
public class AccessSwitch extends ServerResource {

	/**
	 * Retrieves the list of access switches.
	 * @return  the list of access switches.
	 */
	@Get("json")
    public Set<String> show() {
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
    	return db.getAccessSwitches();
    }

	/**
	 * Adds a switch to the list of access switches.
	 * @param fmJson  the JSON message.
	 * @return        a message carrying information about the success of the operation.
	 */
	@Post("json")
	public Map<String, String> store(String fmJson) {
		Map<String, String> result = new HashMap<>();
		
        // Check if the payload is provided
		if (fmJson == null) {
			result.put("message", "No parameters provided");
			return result;
		}

		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);

			// Get the field dpid.
			DatapathId dpid;
			try {
				dpid = DatapathId.of(root.get("dpid").asText());
			} catch (NumberFormatException e) {
				result.put("message", "Invalid DPID format");
				return result;
			}

			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.addAccessSwitch(dpid));

		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}

		return result;
	}

	/**
	 * Removes a switch from the list of access switches.
	 * @param fmJson  the JSON message.
	 * @return        a message carrying information about the success of the operation.
	 */
	@Delete("json")
	public Map<String, String> remove(String fmJson) {
		Map<String, String> result = new HashMap<>();

		// Check if the payload is provided
		if (fmJson == null) {
			result.put("message", "No parameters provided");
			return result;
		}

		// Parse the JSON input
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(fmJson);

			// Get the field dpid.
			DatapathId dpid;
			try {
				dpid = DatapathId.of(root.get("dpid").asText());
			} catch (NumberFormatException e) {
				result.put("message", "Invalid DPID format");
				return result;
			}

			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.removeAccessSwitch(dpid));

		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}

		return result;
	}
}