package it.unipi.floodlightcontroller.rest;

import java.util.Map;
import java.util.Map.Entry;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IDistributedBrokerREST extends IFloodlightService {

	Map<String, String> getSubscribers(IPv4Address resource_address);

	Map<String, Object> getResources();

	String createResource();

	String subscribeResource(IPv4Address resource_address, IPv4Address USER_IP, MacAddress MAC);
	
	String removeSubscription(IPv4Address resource_address, IPv4Address USER_IP);
}
