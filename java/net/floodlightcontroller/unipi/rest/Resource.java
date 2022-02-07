package net.floodlightcontroller.unipi.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Resource extends ServerResource {

    @Get("json")
    public Map<String, Object> show() {	
    	IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
    	return db.getResources();
    }

	@Post("json")
	public Map<String, String> store(String fmJson) {
		Map<String, String> result = new HashMap<>();
        
        IDistributedBrokerREST db = (IDistributedBrokerREST) getContext().getAttributes().get(IDistributedBrokerREST.class.getCanonicalName());
		result.put("message", db.createResource());
		
		return result;
	}

}
