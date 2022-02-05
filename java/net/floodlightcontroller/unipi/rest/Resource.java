package net.floodlightcontroller.unipi.rest;

import org.restlet.resource.ServerResource;

public class Resource extends ServerResource {

    @Get("json")
    public Map<String, Object> show() {	
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
    	return db.getResources();
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
			String username = root.get("name").asText();

			// Get the field address
            IPv4Address address;
			try {
				address = IPv4Address.of(root.get("address").asText());
			} catch (IllegalArgumentException e) {
				result.put("message", "Invalid virtual address format");
				return result;
			}
			
			IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
			result.put("message", dn.createResource(username, MAaddressC));
			
		} catch (IOException e) {
			e.printStackTrace();
			result.put("message", "An exception occurred while parsing the parameters");
		}
		
		result.put("message", "Resource correctly created");

		return result;
	}

}
