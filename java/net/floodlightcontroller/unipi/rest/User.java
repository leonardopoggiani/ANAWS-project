package net.floodlightcontroller.unipi.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.MacAddress;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class User extends ServerResource {

	@Get("json")
    public Map<String, Object> show() {	
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
    	return db.getSubscribedUsers();
    }

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
			
			// Get the field username
			String username = root.get("username").asText();

			// Get the field MAC
			MacAddress MAC;
			try {
				MAC = MacAddress.of(root.get("mac").asText());
			} catch (IllegalArgumentException e) {
				result.put("message", "Invalid MAC address format");
				return result;
			}
			
			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.subscribeUser(username, MAC));
			
		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}
		
		result.put("message", "User correctly added");

		return result;
	}
}
