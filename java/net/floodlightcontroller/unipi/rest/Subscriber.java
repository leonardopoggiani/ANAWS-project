package net.floodlightcontroller.unipi.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Subscriber extends ServerResource {
	
	@Get("json")
    public Map<String, Object> show() {	
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
    	return db.getSubscribers();
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
			IPv4Address resource_address;
			try {
				resource_address = IPv4Address.of(root.get("resource").asText());
			} catch (IllegalArgumentException e) {
				result.put("message", "Invalid resource address address format");
				return result;
			}
			
			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", db.subscribeResource(resource_address));
			
		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}
		
		result.put("message", "Resource correctly subscribed");

		return result;
	}

}
