package it.unipi.floodlightcontroller.distributedbroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.floodlightcontroller.rest.DistributedBrokerWebRoutable;
import it.unipi.floodlightcontroller.rest.IDistributedBrokerREST;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchBackend;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;


public class DistributedMessageBroker implements IOFMessageListener, IFloodlightModule, IDistributedBrokerREST {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedMessageBroker.class);
    private final Logger loggerREST = LoggerFactory.getLogger(IDistributedBrokerREST.class);

    // Floodlight services used by the module.
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
	private IDeviceService deviceManagerService; //Reference to the device manager
	private static IRoutingService routingService;
    private IOFSwitchService switchService;

    // Default virtual IP and MAC addresses of the server
	private final static MacAddress SERVER_MAC =  MacAddress.of("00:00:00:00:00:FE");
	
	//private final int IDLE_TIMEOUT = 5;
    //private final int HARD_TIMEOUT = 10;
    
	// <ip virtuale, int> -> <1.1.1.1, 1>, <1.1.1.2, 2> ...
    private final Map<IPv4Address, Integer>  resources = new HashMap<>();
    int howManyResources = 1; 
    IPv4Address lastAddressUsed = null;
        
    // Resources and subscribers for each resource
    // <virtual resource address, <real mac address, real ip>>
    // <1.1.1.1, < <00:00:00:00:00:01, 10.0.0.1>,<00:00:00:00:00:02, 10.0.0.2> ..>
    // <1.1.1.2, < <00:00:00:00:00:03, 10.0.0.3>, ..>
  	private final Map<IPv4Address, HashMap<MacAddress, IPv4Address>>  resourceSubscribers = new HashMap<>();
  	
	// Access switches.
    private final Set<String> accessSwitches = new HashSet<>();
   

    
	@Override
	public String getName() {
		return IDistributedBrokerREST.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	    Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IDistributedBrokerREST.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	    m.put(IDistributedBrokerREST.class, this);
	    return m;
	}

	@Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<>();

        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(IRestApiService.class);
        dependencies.add(IDeviceService.class);
        dependencies.add(IRoutingService.class);
        dependencies.add(IOFSwitchService .class);

        return dependencies;
	}

	@Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger.info("Initializing distributed message broker module.");

        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        deviceManagerService = context.getServiceImpl(IDeviceService.class);
        routingService = context.getServiceImpl(IRoutingService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);

	}

	 @Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new DistributedBrokerWebRoutable());
	}
	 
	private boolean isResourceAddress(MacAddress addressMAC, IPv4Address addressIP) {
		for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
        	if (addressIP.compareTo(resource.getKey()) == 0) {
        		if(addressMAC.compareTo(SERVER_MAC) == 0) {
        			return true;
        		}
        	}
        }
		
		return false;
    }
	
	
	
	
	
	 /**
     * Checks if the publisher is trying to publish a message on a resource in which it is also a subscriber.
     * @param publisherAddressMAC
     * @param resourceIP
     * @return    true if the publisher is also a subscriber for this resource, false otherwise.
     */
		private boolean isValidPublisher(MacAddress publisherAddressMAC, IPv4Address resourceIP) {
		
			return !resourceSubscribers.get(resourceIP).containsKey(publisherAddressMAC);
			
    }
	
	 /**
     * Checks if the switch identified by the given DPID is registered as an access switch.
     * @param sw  the DPID of the switch.
     * @return    true if the DPID identifies an access switch, false otherwise.
     */
	private boolean isAccessSwitch(String sw) {
        return accessSwitches.contains(sw);
    }
	
	/**
     * Checks if the switch identified by the given DPID is registered as an access switch.
     * @param sw  the DPID of the switch.
     * @return    true if the DPID identifies an access switch, false otherwise.
     */
	private boolean isSubscriber(IPv4Address resourceIpAddress, MacAddress subscriberMac) {
        if(!resources.containsKey(resourceIpAddress)) {
        	logger.info("Resource {} doesn't exist", resourceIpAddress.toString());
        	return false;
        }
        if(resourceSubscribers.get(resourceIpAddress).containsKey(subscriberMac)){
        	logger.info("Subscriber {} doesn't exist", subscriberMac.toString());
        	return false;
        }
        return true;		
		
    }
	
	 

	private Command handleIpPacket(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
        MacAddress sourceMAC = ethernetFrame.getSourceMACAddress();
        MacAddress destinationMAC = ethernetFrame.getDestinationMACAddress();
        IPv4Address sourceIP = ipPacket.getSourceAddress();
        IPv4Address destinationIP = ipPacket.getDestinationAddress();

        logger.info("Processing an IP packet.");
        logger.info("Switch: {}", sw.getId());
        logger.info("Source: {}, {}", sourceMAC, sourceIP);
        logger.info("Destination: {}, {}", destinationMAC, destinationIP);
        loggerREST.info("AccessSwitches: " + accessSwitches.toString());

        // The packet is a request to a resource from a user.
        if (isResourceAddress(destinationMAC, destinationIP) ) {
            logger.info("The packet is a message to a resource.");
            handleRequestToResource(sw, packetIn, ethernetFrame, ipPacket);
            return Command.STOP;
        }
        
        return Command.CONTINUE;
	}
	
	/**
     * Returns the shortest path in terms of hops between a starting switch and
     * a set of ending switches.
     * @param startSwitch  the DPID of the switch representing the starting point of the path.
     * @param endSwitches  the list of switches and ports each one representing
     *                     an end point of the path.
     * @return             the shortest path, if at least one path exists between the endpoints,
     *                     null otherwise.
     */
     public static Path getShortestPath(DatapathId startSwitch, SwitchPort[] endSwitches) {
        Path shortestPath = null;

        for (SwitchPort endSwitch : endSwitches) {
            
            Path candidateShortestPath = routingService.getPath(
            		startSwitch, 
            		OFPort.of(1),
                    endSwitch.getNodeId(), 
                    endSwitch.getPortId()
            	);
                                                                
            if (candidateShortestPath == null)
                continue;

            if (shortestPath == null) {
                shortestPath = candidateShortestPath;
                continue;
            }

            if (candidateShortestPath.getHopCount() < shortestPath.getHopCount())
                shortestPath = candidateShortestPath;
        }

        return shortestPath;
    }
    
    private boolean isFirstSwitch(IOFSwitch sw, MacAddress source_mac_address) {
    	IDevice dstDevice = deviceManagerService.findDevice(
				source_mac_address, 
				VlanVid.ZERO, 
				IPv4Address.NONE, 
				IPv6Address.NONE, 
				DatapathId.NONE, 
				OFPort.ZERO
			);
		
		SwitchPort[] switches_host = dstDevice.getAttachmentPoints();
		
		for(SwitchPort swport : switches_host) {
			logger.info("SwitchPort: {}", swport.toString());
			if(swport.getNodeId().equals(sw.getId())) {
				logger.info("First switch!!");
				return true;
			}
		}
	
		return false;
    }
	
	private void handleRequestToResource(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
				
		IPv4Address resource_address = ipPacket.getDestinationAddress();
		MacAddress sourceMac = ethernetFrame.getSourceMACAddress();
		
		if(!isFirstSwitch(sw, sourceMac)) {
			return;	
		}
		
		// the request to the resource must retrieve the list of subscribers of the resource
		Map<String, String> subscriber_list = null;
		
		for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
        	if (resource_address.compareTo(resource.getKey()) == 0) {
        		subscriber_list = getSubscribers(resource.getKey());
                break;
        	}
        }
		
		if(subscriber_list == null) {
			// no subscribers for this resource
			logger.info("No subscribers found for this resource: " + resource_address.toString());
			return;
		}

		// i want to send a packet to every subscriber on the list
		for(Map.Entry<String, String> subscriber : subscriber_list.entrySet()) {
			
			//OFPort outputPort = OFPort.ANY;
			logger.info("Subscriber: {}", subscriber.getKey());
			
			Iterator<? extends IDevice> dstDev = deviceManagerService.queryDevices(
						MacAddress.of(subscriber.getKey()), 
						VlanVid.ZERO, 
						IPv4Address.NONE, 
						IPv6Address.NONE, 
						DatapathId.NONE, 
						OFPort.ZERO
					);
			
			if(dstDev.hasNext()) {
				 
				IDevice device =  dstDev.next();
				
				logger.info("Dev: " + device.toString());
				 
				SwitchPort[] switches = device.getAttachmentPoints(); 
				
				
				// Compute the shortest path from the switch that sent the packet-in to the candidate server.
				Path shortestPath = getShortestPath(sw.getId(), switches);
				if (shortestPath == null) {
					logger.info("No path found!");
				    continue;
				}
								
				// The output port of the current switch is specified by the second element of the path.
				OFPort outputPort = shortestPath.getPath().get(1).getPortId();
		        // Create the actions (Change DST mac and IP addresses and set the out-port)
		        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		        
		        actionList = translateDestinationAddressIntoReal(sw, subscriber.getKey(), subscriber.getValue(), outputPort);
				// Create the Packet-Out and set basic data for it (buffer id and in port)
				OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
				pob.setBufferId(packetIn.getBufferId());
				pob.setInPort(OFPort.ANY);
				// Assign the action
				pob.setActions(actionList);
				
				// Packet might be buffered in the switch or encapsulated in Packet-In 
				// If the packet is encapsulated in Packet-In sent it back
				if (packetIn.getBufferId() == OFBufferId.NO_BUFFER) {
					// Packet-In buffer-id is none, the packet is encapsulated -> send it back
		            byte[] packetData = packetIn.getData();
		            pob.setData(packetData);
		            
				} 
						
				sw.write(pob.build());
				logger.info("Sent packet-out on the outport specified.");
					
			}		
						
		}
	}
	
	private ArrayList<OFAction> translateDestinationAddressIntoReal(IOFSwitch sw, String subscriberMAC,
            String subscriberIP, OFPort outputPort) {
		
		OFOxms oxmsBuilder = sw.getOFFactory().oxms();
		OFActions actionBuilder = sw.getOFFactory().actions();
		ArrayList<OFAction> actionList = new ArrayList<>();
		
		OFActionSetField setMACDestination = actionBuilder.buildSetField()
		.setField(oxmsBuilder.buildEthDst().setValue(MacAddress.of(subscriberMAC)).build())
		.build();
		
		OFActionSetField setIPDestination = actionBuilder.buildSetField()
		.setField(oxmsBuilder.buildIpv4Dst().setValue(IPv4Address.of(subscriberIP)).build())
		.build();
		
		OFActionOutput output = actionBuilder.buildOutput()
		.setMaxLen(0xFFffFFff)
		.setPort(outputPort)
		.build();
		
		actionList.add(setMACDestination);
		actionList.add(setIPDestination);
		actionList.add(output);
		
		return actionList;
	}
	
	
	private IPacket createArpReplyForServer(Ethernet ethernetFrame, ARP arpRequest, IPv4Address resource_virtual_address) {
		logger.info("Sender MAC: {}",arpRequest.getSenderHardwareAddress());
		logger.info("Sender IP: {}",arpRequest.getSenderProtocolAddress());
		logger.info("destination MAC: {}",ethernetFrame.getSourceMACAddress());

        return new Ethernet()
                .setSourceMACAddress(SERVER_MAC)
                .setDestinationMACAddress(ethernetFrame.getSourceMACAddress())
                .setEtherType(EthType.ARP)
                .setPriorityCode(ethernetFrame.getPriorityCode())
                .setPayload(
                        new ARP()
                                .setHardwareType(ARP.HW_TYPE_ETHERNET)
                                .setProtocolType(ARP.PROTO_TYPE_IP)
                                .setHardwareAddressLength((byte) 6)
                                .setProtocolAddressLength((byte) 4)
                                .setOpCode(ARP.OP_REPLY)
                                .setSenderHardwareAddress(SERVER_MAC)
                                .setSenderProtocolAddress(resource_virtual_address)
                                .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                                .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress()));
        
        
    }
	
	private Command handleArpRequest(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, ARP arpRequest) {
        IPacket arpReply = null;

        logger.info("Processing an ARP request.");
        logger.info("Switch: {}", sw.getId());
        logger.info("Source: {}", ethernetFrame.getSourceMACAddress());
        logger.info("Destination: {}", ethernetFrame.getDestinationMACAddress());
        logger.info(" Destination IP: {}", arpRequest.getTargetProtocolAddress());            

        boolean belongToResource = false;
        
        for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
        	if (arpRequest.getTargetProtocolAddress().compareTo(resource.getKey()) == 0) {
                logger.info("The ARP request belong to a virtual address of a resource, so build a reply");
                arpReply = createArpReplyForServer(ethernetFrame, arpRequest, resource.getKey());
                belongToResource = true;
                break;
        	}
        }
        
        if(belongToResource == false) {
            logger.info("Not belonging to any resource, dropping the request!");            
            return Command.STOP;
        }
        
        // Create the packet-out.
        OFPacketOut.Builder packetOutBuilder = sw.getOFFactory().buildPacketOut();
        packetOutBuilder.setBufferId(OFBufferId.NO_BUFFER);
        packetOutBuilder.setInPort(OFPort.ANY);

        // Create action: send the packet back from the source port.
        OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
        OFPort inPort = packetIn.getMatch().get(MatchField.IN_PORT);
        actionBuilder.setPort(inPort);

        // Assign the action.
        packetOutBuilder.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
        // Set the ARP reply as packet data.
        packetOutBuilder.setData(arpReply.serialize());

        logger.info("Sending out the ARP reply");
        sw.write(packetOutBuilder.build());

        return Command.STOP;
    }
	
	/**
     * Checks if the given IP address couple identifies a resource.
     * @param addressIP   the IPv4 address of the resource.
     * @return            true if the couple identifies a resource, false otherwise.
     */
    private boolean isResourceIPAddress(IPv4Address addressIP) {
        return resourceSubscribers.containsKey(addressIP);
    }
    
    /**
     * Drops a packet that comes from an unsubscribed user or that comes from a subscribed user,
     * but is not addressed to the service. Only ARP requests and IP packets are allowed.
     * @param sw             the switch that sent the packet-in.
     * @param ethernetFrame  the Ethernet frame encapsulated in the packet-in.
     * @return               true if the packet must be dropped, false otherwise.
     */
    private boolean filterPacket(IOFSwitch sw, Ethernet ethernetFrame) {
        MacAddress sourceMAC = ethernetFrame.getSourceMACAddress();
        MacAddress destinationMAC = ethernetFrame.getDestinationMACAddress();
        logger.info("Received a packet from {} with destination {}", sourceMAC, destinationMAC);
        
        IPacket packet = ethernetFrame.getPayload();
        
        // If the packet is an ARP request, it is allowed only if it targets a resource's virtual IP.
        if ((ethernetFrame.isBroadcast() || ethernetFrame.isMulticast()) && packet instanceof ARP) {
            ARP arpRequest = (ARP) packet;

            if (!isResourceIPAddress(arpRequest.getTargetProtocolAddress())) {
                logger.info("The packet is an ARP request coming from an user and not addressed " +
                        "to a resource. Dropping the packet.");
                return true;
            }
            
            logger.info("The packet is an ARP request coming from an user and addressed " +
                                "to a resource. Accepting the packet.");
            return false;
        }

        // If the packet is an IP request, check if IP destination address is virtual.
        if (packet instanceof IPv4) {
            IPv4 ipPacket = (IPv4) packet;
	        if (!isResourceIPAddress(ipPacket.getDestinationAddress())) {
	        	logger.info("The packet is an IP request not addressed to a resource");
	        	return true;      
	        }
	        
	        // If ip destination address is a valid virtual resource address check if the publisher is valid
	        if (!isValidPublisher(sourceMAC, ipPacket.getDestinationAddress())) {
        		logger.info("The packet is an IP request addressed to a resource but coming from a not valid user");
        		return true;
        	}
	       
	        logger.info("The packet is a valid IP request or a reply. Accepting the packet.");
	        
	        return false;
        }
        
        logger.info("The packet is neither an ARP request nor an IP packet. Dropping the packet.");
        return true;
    }
	 
	@Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        OFPacketIn packetIn = (OFPacketIn) msg;
        Ethernet ethernetFrame = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        IPacket packet = ethernetFrame.getPayload(); 
        
       if (filterPacket(sw, ethernetFrame)) {
            return Command.STOP;
        }

        if (packet instanceof ARP) {
            ARP arpRequest = (ARP) packet;
            return handleArpRequest(sw, packetIn, ethernetFrame, arpRequest);        
        }

        if (packet instanceof IPv4) {
            IPv4 ipPacket = (IPv4) packet;
            return handleIpPacket(sw, packetIn, ethernetFrame, ipPacket);
        }

        return Command.STOP;
    }

	@Override
	public Map<String, String> getSubscribers(IPv4Address resource_address) {
		// List of subscribers of resource_address
		Map<String, String> list = new HashMap<>();
		for (Map.Entry<MacAddress, IPv4Address> subscriber : resourceSubscribers.get(resource_address).entrySet()) {
            list.put(subscriber.getKey().toString(), subscriber.getValue().toString());
        }
		
		return list;
	}

	@Override
	public Map<String, Object> getResources() {
		Map<String, Object> list = new HashMap<>();
		
        for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
            list.put(resource.getKey().toString(), resource.getValue().toString());
        }

        return list;
    }

	@Override
	public String createResource() {
		IPv4Address address = null;
		int address_int = 0;
				
		// retrieve last virtual ip used and create the new one
		IPv4Address lastVirtualIpUsed = lastAddressUsed;
		
		if(resources.isEmpty()) {
			address = IPv4Address.of("1.1.1.1");
			lastAddressUsed = address;
		} else {
			address_int = lastVirtualIpUsed.getInt() + 1;
			address = IPv4Address.of(address_int);
		}
		
		resources.put(address, howManyResources);
		resourceSubscribers.put(address, new HashMap<MacAddress, IPv4Address>());
		howManyResources++;
		lastAddressUsed = address;
        return "Resource created, address: " + address;
	}

	@Override
	public String subscribeResource(IPv4Address resource_address, IPv4Address USER_IP, MacAddress MAC) {

		// Check if MAC address is already subscribed.
		if (resourceSubscribers.get(resource_address).containsKey(MAC)) {
		    loggerREST.info("The MAC address {} is already subscribed.", MAC);
		    return "MAC address already subscribed";
		}
		
		// Check if the USER_IP is already present.
        if (resourceSubscribers.get(resource_address).containsValue(USER_IP)) {
            loggerREST.info("The USER_IP \"{}\" is already in use.", USER_IP);
            return "Username already in use";
        }
        
		// Add user to the list of subscribed users related to this resource.
		resourceSubscribers.get(resource_address).put(MAC, USER_IP);

        loggerREST.info("Registered user_MAC {} with IP \"{}\".", MAC, USER_IP);
        return "Subscription successful";
	}
	
	
	// to delete or review
	/*
	public String removeSubscription(IPv4Address resource_address, IPv4Address USER_IP) {
		
		loggerREST.info("Received request for delete the subscription of {}, with IP \"{}\".",
				USER_IP);

		// Check if the username is present.
        for(Map.Entry<MacAddress, IPv4Address> resourceSub : resourceSubscribers.get(resource_address).entrySet()) {
        	if(resourceSub.getValue().equals(USER_IP))
        		resourceSubscribers.get(resource_address).remove(resourceSub);
				return "User removed from the resource";
        }
		
        return "User removed successfully";
	}
	*/

	@Override
	public Set<String> getAccessSwitches() {
	        return accessSwitches;
	}
	
	@Override
	public String addAccessSwitch(DatapathId dpid) {
        loggerREST.info("Received request for the insertion of the access switch {}", dpid);
        
        // Check if the switch is already present.
        if (isAccessSwitch(dpid.toString())) {
            loggerREST.info("The switch {} is already an access switch.", dpid);
            return "Already an access switch";
        }
        //boolean success = changePriorityOfDefaultRule(dpid, ACCESS_SWITCH_DEFAULT_RULE_PRIORITY);
        // if (success) {
        accessSwitches.add(dpid.toString());
        //}
        
        loggerREST.info("The switch {} is now an access switch. List: \n", dpid);
        
        return "Access switch added";
 }

	@Override
	public String removeSubscription(IPv4Address resource_address, MacAddress userMac) {
		if(isSubscriber(resource_address, userMac)) {
			 loggerREST.info("Removed subscriber {}", userMac);
	            resourceSubscribers.get(resource_address).remove(userMac);
	            return "Subscriber removed";
		}
		loggerREST.info("The subscriber or the resource don't exist");
    	return "Subscriber or resource not found";
	}

	@Override
	public String removeAccessSwitch(DatapathId dpid) {
		 loggerREST.info("Received request for the cancellation of the access switch {}", dpid);

	        if (isAccessSwitch(dpid.toString())) {
	        	
	            loggerREST.info("Removed access switch {}", dpid);
	            accessSwitches.remove(dpid.toString());
	            return "Access switch removed";
	        }
	        loggerREST.info("The switch {} is not an access switch", dpid);
	    	return "Access switch not found";
	    }
	}

