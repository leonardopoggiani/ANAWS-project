package it.unipi.floodlightcontroller.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Subscriber extends ServerResource {
	
	@Get("json")
    public Map<String, String> show(String fmJson) {	
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
        String resources = (String) getRequestAttributes().get("resource");
        
    	return db.getSubscribers(IPv4Address.of(resources));
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

			// Get the field virtual address
	        String resources = (String) getRequestAttributes().get("resource");
			IPv4Address resource_address = IPv4Address.of(resources);
			String user_address;
			String MAC;
			try {
				user_address = root.get("address").asText();
				MAC = root.get("MAC").asText();
			} catch (IllegalArgumentException e) {
				result.put("message", "Invalid format");
				return result;
			}
			
			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.subscribeResource(resource_address, IPv4Address.of(user_address), MacAddress.of(MAC)));
			
		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}

		return result;
	}
	
	/**
	 * Removes a subscriber from a resource.
	 * @param fmJson  the JSON message.
	 * @return        a message carrying information about the success of the operation.
	 */
	@Delete("json")
	public Map<String, String> remove(String fmJson) {
		Map<String, String> result = new HashMap<>();
		
        // Check if the payload is provided
        if(fmJson == null){
			result.put("message", "No parameters provided");
			return result;
        }
     // Parse the JSON input
     		ObjectMapper mapper = new ObjectMapper();

		try {
			JsonNode root = mapper.readTree(fmJson);

			// Get the field virtual address
	        String resource = (String) getRequestAttributes().get("resource");
			IPv4Address resource_address = IPv4Address.of(resource);
			String user_MAC;
			try {
				user_MAC = root.get("MAC").asText();
			} catch (IllegalArgumentException e) {
				result.put("message", "Invalid format");
				return result;
			}
			
			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.removeSubscription(resource_address, MacAddress.of(user_MAC)));
			
		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}

	
		return result;
}

}
