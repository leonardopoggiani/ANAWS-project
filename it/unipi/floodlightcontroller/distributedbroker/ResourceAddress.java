package it.unipi.floodlightcontroller.distributedbroker;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import it.unipi.floodlightcontroller.distributedbroker.DistributedMessageBroker;

public class ResourceAddress {
	
	static DistributedMessageBroker broker = new DistributedMessageBroker();
	
	public static boolean isResourceAddress(IPv4Address addressIP) {
		Set<Entry<IPv4Address, HashMap<MacAddress, IPv4Address>>> resourceSubscribers = broker.getResourceSubcribers().entrySet();
		for (Entry<IPv4Address, HashMap<MacAddress, IPv4Address>> resource : resourceSubscribers) {
        	if (addressIP.compareTo(resource.getKey()) == 0) {
        			return true;        		
        	}
        }
		
		return false;
    }

}
