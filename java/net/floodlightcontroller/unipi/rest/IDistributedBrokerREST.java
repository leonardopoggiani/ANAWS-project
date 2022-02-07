package net.floodlightcontroller.unipi.rest;

import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IDistributedBrokerREST extends IFloodlightService {

	Map<String, Object> getSubscribedUsers();

	String subscribeUser(String username, MacAddress MAC);

	Map<String, Object> getSubscribers();

	Map<String, Object> getResources();

	String createResource();
}
