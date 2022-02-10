package it.unipi.floodlightcontroller.distributedbroker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.floodlightcontroller.rest.DistributedBrokerWebRoutable;
import it.unipi.floodlightcontroller.rest.IDistributedBrokerREST;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
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
import net.floodlightcontroller.util.FlowModUtils;

public class DistributedMessageBroker implements IOFMessageListener, IFloodlightModule, IDistributedBrokerREST {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedMessageBroker.class);
    private final Logger loggerREST = LoggerFactory.getLogger(IDistributedBrokerREST.class);

    // Floodlight services used by the module.
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
    private IDeviceService deviceService;

    // Default virtual IP and MAC addresses of the server
	private final static MacAddress SERVER_MAC =  MacAddress.of("00:00:00:00:00:FE");
	private static final int IDLE_TIMEOUT = 0;
	private static final int HARD_TIMEOUT = 0;
    
    private final Map<IPv4Address, Integer>  resources = new HashMap<>();
    int howManyResources = 1;
   
    // Resources and subscribers for each resource
    // <virtual resource address, <real mac address, real ip>>
  	private final Map<IPv4Address, HashMap<MacAddress, IPv4Address>>  resourceSubscribers = new HashMap<>();
    
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
        dependencies.add(IDeviceService.class);
        dependencies.add(IRestApiService.class);

        return dependencies;
	}

	@Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger.info("Initializing distributed message broker module.");

        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        deviceService = context.getServiceImpl(IDeviceService.class);
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

	private Command handleIpPacket(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
        MacAddress sourceMAC = ethernetFrame.getSourceMACAddress();
        MacAddress destinationMAC = ethernetFrame.getDestinationMACAddress();
        IPv4Address sourceIP = ipPacket.getSourceAddress();
        IPv4Address destinationIP = ipPacket.getDestinationAddress();

        logger.info("Processing an IP packet.");
        logger.info("Switch: {}", sw.getId());
        logger.info("Source: {}, {}", sourceMAC, sourceIP);
        logger.info("Destination: {}, {}", destinationMAC, destinationIP);

        // The packet is a request to the service from a user.
        if (isResourceAddress(destinationMAC, destinationIP)) {
            logger.info("The packet is a message to a resource.");
            logger.info("Handling the translation of the destination address.");
            handleRequestToResource(sw, packetIn, ethernetFrame, ipPacket);
            return Command.STOP;
        }

        // The packet is a response from the service to a user.
        // if (isServerCompleteAddress(sourceMAC, sourceIP)) {
        //    logger.info("The packet is a response from the service transiting through an access switch.");
        //    logger.info("Handling the translation of the source address.");
        //    handleResponseFromServer(sw, packetIn, ethernetFrame, ipPacket);
        //    return Command.STOP;
        //}

        // The packet is transiting through the network.
        logger.info("The packet is transiting through the network.");
        logger.info("Leaving the processing to the Forwarding module.");
        return Command.CONTINUE;
	}
	
	private Set<SwitchPort> getSwitchesAttachedToDevice(MacAddress deviceMAC) {
		// Iterator<? extends IDevice> devices = deviceService.queryDevices(deviceMAC, null, null, null, null);
        Set<SwitchPort> attachedSwitches = new HashSet<>();
        int numberOfDevices = 0;

        // while(devices.hasNext()) {
        //    attachedSwitches.addAll(Arrays.asList(devices.next().getAttachmentPoints()));
        //    numberOfDevices++;

            if (numberOfDevices > 1) {
                logger.error("Multiple devices with the same MAC address were found in the network." +
                        "Returning no attachment points.");
            //    break;
            }
        // }

        /*
         *  Conditions causing the return of no switches:
         *  1) the device is not in the network anymore;
         *  2) multiple devices with the same MAC address are found (it should not be possible);
         *  3) the device is still a tracked device, but it is disconnected (no attachment points).
         */
        if (numberOfDevices == 0 || numberOfDevices > 1 || attachedSwitches.isEmpty())
            return null;

        return attachedSwitches;
    }
	
	private void handleRequestToResource(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
		// the request to the resource must retrieve the list of subscribers of the resource
		Map<String, String> list = null;
		
		for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
        	if (ipPacket.getDestinationAddress().compareTo(resource.getKey()) == 0) {
                list = getSubscribers(resource.getKey());
                break;
        	}
        }
		
		if(list == null) {
			// no subscribers for this resource
			return;
		}
		
		for(Map.Entry<String, String> subscriber : list.entrySet()) {
			
	        OFPort outputPort = packetIn.getMatch().get(MatchField.IN_PORT);
			logger.info("outputPort: {}", outputPort);

            if (outputPort == null) {
                logger.info("The user is not connected anymore to this access switch. Dropping the packet.");
                return;
            }
            logger.info("Output port towards the user: " + outputPort);

            
			instructSwitchWhenRequestToService(sw, packetIn, ethernetFrame, ipPacket, MacAddress.of(subscriber.getKey()), IPv4Address.of(subscriber.getValue()), outputPort);
		}
		
		logger.info("Packet-out and flow mod correctly sent to the switch.");
	}
	
	private Match createMatchWhenRequestToService(IOFSwitch sw, Ethernet ethernetFrame, IPv4 ipPacket, IPv4Address resource_address) {
        MacAddress userMAC = ethernetFrame.getSourceMACAddress();
        IPv4Address userIP = ipPacket.getSourceAddress();
        Match.Builder matchBuilder = sw.getOFFactory().buildMatch();

        matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, userMAC)
                .setExact(MatchField.IPV4_SRC, userIP)
                .setExact(MatchField.ETH_DST, SERVER_MAC)
                .setExact(MatchField.IPV4_DST, resource_address);

        return matchBuilder.build();
    }
	
	private ArrayList<OFAction> translateDestinationAddressIntoReal(IOFSwitch sw, MacAddress serverMAC,
            IPv4Address serverIP, OFPort outputPort) {
		
		OFOxms oxmsBuilder = sw.getOFFactory().oxms();
		OFActions actionBuilder = sw.getOFFactory().actions();
		ArrayList<OFAction> actionList = new ArrayList<>();
		
		OFActionSetField setMACDestination = actionBuilder.buildSetField()
		.setField(oxmsBuilder.buildEthDst().setValue(serverMAC).build())
		.build();
		
		OFActionSetField setIPDestination = actionBuilder.buildSetField()
		.setField(oxmsBuilder.buildIpv4Dst().setValue(serverIP).build())
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
	
	private void instructSwitchWhenRequestToService(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame,
        IPv4 ipPacket, MacAddress serverMAC, IPv4Address serverIP,
        OFPort outputPort) {

		OFFlowAdd.Builder flowModBuilder = sw.getOFFactory().buildFlowAdd();
		Match match = createMatchWhenRequestToService(sw, ethernetFrame, ipPacket, serverIP);
		ArrayList<OFAction> actionList = translateDestinationAddressIntoReal(sw, serverMAC, serverIP, outputPort);
		
		flowModBuilder.setIdleTimeout(IDLE_TIMEOUT);
		flowModBuilder.setHardTimeout(HARD_TIMEOUT);
		flowModBuilder.setBufferId(OFBufferId.NO_BUFFER);
		flowModBuilder.setOutPort(OFPort.ANY);
		flowModBuilder.setCookie(U64.of(0));
		flowModBuilder.setPriority(FlowModUtils.PRIORITY_MAX);
		flowModBuilder.setMatch(match);
		flowModBuilder.setActions(actionList);
		
		sw.write(flowModBuilder.build());
		
		/*
		*  Create a packet-out doing the same actions specified in the flow mod, so that
		*  the packet sent to the controller is delivered correctly and not dropped.
		*/
		OFPacketOut.Builder packetOutBuilder = sw.getOFFactory().buildPacketOut();
		packetOutBuilder.setBufferId(packetIn.getBufferId());
		packetOutBuilder.setInPort(OFPort.ANY);
		packetOutBuilder.setActions(actionList);
		
		// If the packet-in encapsulates the original packet, the packet is sent back.
		if (packetIn.getBufferId() == OFBufferId.NO_BUFFER)
		packetOutBuilder.setData(packetIn.getData());
		
		sw.write(packetOutBuilder.build());
	}

	private IPacket createArpReplyForService(Ethernet ethernetFrame, ARP arpRequest, IPv4Address resource_virtual_address) {
		logger.info("arp reply");
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

        boolean belongToResource = false;
        
        for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
        	if (arpRequest.getTargetProtocolAddress().compareTo(resource.getKey()) == 0) {
                logger.info("the arp request belong to a virtual address of a resource, so build a reply");
                arpReply = createArpReplyForService(ethernetFrame, arpRequest, resource.getKey());
                belongToResource = true;
                break;
        	}
        }
        
        if(belongToResource == false) {
            logger.info("The IP address in the ARP request does not belong to a device in the network. " +
                                "Dropping the ARP reply.");
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

	/*private boolean filterPacket(IOFSwitch sw, Ethernet ethernetFrame) {
        MacAddress sourceMAC = ethernetFrame.getSourceMACAddress();
        MacAddress destinationMAC = ethernetFrame.getDestinationMACAddress();
        logger.info("Received a packet from {} with destination {}", sourceMAC, destinationMAC);

        // If the packet comes from a server, it is always accepted.
        if (isServerMacAddress(sourceMAC)) {
            logger.info("The packet comes from a server. Accepting the packet.");
            return false;
        }

        // The packet is coming from a subscribed user and it is transiting through an access switch.
        IPacket packet = ethernetFrame.getPayload();

        // If the packet is an ARP request, it is allowed only if it targets the virtual IP.
        if ((ethernetFrame.isBroadcast() || ethernetFrame.isMulticast()) && packet instanceof ARP) {
            ARP arpRequest = (ARP) packet;

            if (arpRequest.getTargetProtocolAddress().compareTo(SERVER_IP) != 0) {
                logger.info("The packet is an ARP request coming from the client and not addressed " +
                                    "to the service. Dropping the packet.");
                return true;
            }

            logger.info("The packet is an ARP request coming from the client and addressed " +
                                "to the service. Accepting the packet.");
            return false;
        }

        // If the packet is an IP request, check if IP or MAC destination addresses are virtual.
        if (packet instanceof IPv4) {
            IPv4 ipPacket = (IPv4) packet;

            if (!isServerAddress(destinationMAC, ipPacket.getDestinationAddress())) {
                logger.info("The packet is an IP request coming from the client and not addressed " +
                                    "to the service. Dropping the packet.");
                return true;
            }

            logger.info("The packet is an IP request coming from the client and addressed " +
                                "to the service. Accepting the packet.");
            return false;
        }

        logger.info("The packet is neither an ARP request nor an IP packet. Dropping the packet.");
        return true;
    }*/
	 
	@Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        OFPacketIn packetIn = (OFPacketIn) msg;
        Ethernet ethernetFrame = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        IPacket packet = ethernetFrame.getPayload(); 
        
        logger.info("processing received packet");

        //if (filterPacket(sw, ethernetFrame))
        //    return Command.STOP;

        if (packet instanceof ARP) {
            ARP arpRequest = (ARP) packet;
            logger.info("arp request");
            return handleArpRequest(sw, packetIn, ethernetFrame, arpRequest);        
        }

        if (packet instanceof IPv4) {
            IPv4 ipPacket = (IPv4) packet;
            logger.info("ip packet");
            return handleIpPacket(sw, packetIn, ethernetFrame, ipPacket);
        }

        return Command.STOP;
    }
	 
	@Override
	public Map<String, Object> getSubscribedUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createUser(String username, MacAddress MAC) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<String, String> getSubscribers(IPv4Address resource_address) {
		//List of subscribers of resource_address
		Map<String, String> list = new HashMap<>();
		for (Map.Entry<MacAddress, IPv4Address> subscriber : resourceSubscribers.get(resource_address).entrySet()) {
            list.put(subscriber.getKey().toString(), subscriber.getValue().toString());
        }
		
        loggerREST.info("The list of subscribers for {} has been provided.", resource_address);

		return list;
	}

	@Override
	public Map<String, Object> getResources() {
		Map<String, Object> list = new HashMap<>();
		
        for (Map.Entry<IPv4Address, Integer> resource : resources.entrySet()) {
            list.put(resource.getKey().toString(), resource.getValue().toString());
        }

        loggerREST.info("The list of resources has been provided.");
        return list;
    }

	@Override
	public String createResource() {
		IPv4Address address = null;
		int address_int = 0;
		
		// retrieve last virtual ip used and create the new one
		IPv4Address lastVirtualIpUsed = null;
		for (Entry<IPv4Address, Integer> entry : resources.entrySet()) {
			lastVirtualIpUsed = entry.getKey();
        }
		loggerREST.info("lastVirtualIpUsed: " + lastVirtualIpUsed);
		
		if(resources.isEmpty()) {
			address = IPv4Address.of("1.1.1.1");
			logger.info("first resource, default address: " + address);
		} else {
			address_int = lastVirtualIpUsed.getInt() + 1;
			address = IPv4Address.of(address_int);
			loggerREST.info("new address:" + address);
			logger.info("new resource, address: " + address);

		}
		
		resources.put(address, howManyResources);
		resourceSubscribers.put(address, new HashMap<MacAddress, IPv4Address>());
		howManyResources++;
        return "Resource created, address: " + address;
	}

	@Override
	public String publishMessage(String message, IPv4Address resource_address) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String subscribeResource(IPv4Address resource_address, IPv4Address USER_IP, MacAddress MAC) {
		
		loggerREST.info("Received request for the subscription of {}, with ip \"{}\".",
                MAC, USER_IP);

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
		
		resourceSubscribers.get(resource_address).put(MAC, USER_IP);
		 // Add user to the list of subscribed users related to this resource.
		//resourceSubscribers.get(resource_address).put(MAC, username);

        loggerREST.info("Registered user_MAC {} with IP \"{}\".", MAC, USER_IP);
        return "Subscription successful";
	}
	
	@Override
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

}
