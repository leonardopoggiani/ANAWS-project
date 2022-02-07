package net.floodlightcontroller.unipi.rest;

import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IDistributedBrokerREST extends IFloodlightService {

	Map<String, Object> getSubscribedUsers();

	String createUser(String username, MacAddress MAC);

	Map<String, Object> getSubscribers();

	Map<String, Object> getResources();

	String createResource();

	String publishMessage(String message, IPv4Address resource_address);

	String subscribeResource(IPv4Address resource_address);
}
