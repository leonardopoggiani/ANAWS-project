package net.floodlightcontroller.unipi.rest;

import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IDistributedBrokerREST extends IFloodlightService {

	/**
	 * Retrieves the list of subscribed users.
	 * @return  the list of subscribed users, identified by their MAC addresses and usernames.
	 */
	Map<String, Object> getSubscribedUsers();

	/**
	 * Adds a user to the list of subscribed ones.
	 * @param username  the username of the user.
	 * @param MAC       the MAC address of the user.
	 * @return          a message carrying information about the success of the operation.
	 */
	String subscribeUser(String username, MacAddress MAC);

	/**
	 * Removes a user from the list of subscribed ones.
	 * @param username  the username of the user.
	 * @return          a message carrying information about the success of the operation.
	 */
	String removeUser(String username);

	/**
	 * Retrieves the virtual MAC and IP addresses of the service.
	 * @return  the virtual MAC and IP addresses of the service.
	 */
	Map<String, Object> getVirtualAddress();

	/**
	 * Changes the virtual MAC and IP addresses of the service.
	 * @param ipv4  the new IPv4 address of the service.
	 * @param MAC   the new MAC address of the service.
	 * @return      a message carrying information about the success of the operation.
	 */
	String setVirtualAddress(IPv4Address ipv4, MacAddress MAC);

	Map<String, Object> getPublishers();

	Map<String, Object> getSubscribers();
}
