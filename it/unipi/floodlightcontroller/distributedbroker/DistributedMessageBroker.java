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
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
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
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.util.FlowModUtils;

public class DistributedMessageBroker implements IOFMessageListener, IFloodlightModule, IDistributedBrokerREST {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedMessageBroker.class);
    private final Logger loggerREST = LoggerFactory.getLogger(IDistributedBrokerREST.class);

    // Floodlight services used by the module.
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;
	private IDeviceService deviceManagerService; //Reference to the device manager
	private IRoutingService routingService;

    // Default virtual IP and MAC addresses of the server
	private final static MacAddress SERVER_MAC =  MacAddress.of("00:00:00:00:00:FE");
	private final int IDLE_TIMEOUT = 5;
    private final int HARD_TIMEOUT = 10;
    
	// <ip virtuale, int> -> <1.1.1.1, 1>, <1.1.1.2, 2> ...
    private final Map<IPv4Address, Integer>  resources = new HashMap<>();
    int howManyResources = 1; 
    IPv4Address lastAddressUsed = null;
    
    // Resources and subscribers for each resource
    // <virtual resource address, <real mac address, real ip>>
    // <1.1.1.1, < <00:00:00:00:00:01, 10.0.0.1>,<00:00:00:00:00:02, 10.0.0.2> ..>
    // <1.1.1.2, < <00:00:00:00:00:03, 10.0.0.3>, ..>
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
        dependencies.add(IRestApiService.class);
        dependencies.add(IDeviceService.class);
        dependencies.add(IRoutingService.class);
        return dependencies;
	}

	@Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger.info("Initializing distributed message broker module.");

        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        deviceManagerService = context.getServiceImpl(IDeviceService.class);
        routingService = context.getServiceImpl(IRoutingService.class);
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
	
	private boolean isServerCompleteAddress(MacAddress addressMAC, IPv4Address addressIP) {
		
		logger.info("Source MAC: {} ,source IP: {}", addressMAC, addressIP);
		
		for (Entry<IPv4Address, HashMap<MacAddress, IPv4Address>> resource : resourceSubscribers.entrySet()) {
			
			HashMap<MacAddress, IPv4Address> list = resource.getValue();
			
			for (Entry<MacAddress, IPv4Address> subscriber : list.entrySet() ) {
				logger.info("subscriber MAC: {} ,subscriber IP: {}", subscriber.getKey(), subscriber.getValue());
				
				if( (subscriber.getKey().compareTo(addressMAC) == 0) && (subscriber.getValue().compareTo(addressIP) == 0) ) {
					logger.info("Subscriber found");
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
            handleRequestToResource(sw, packetIn, ethernetFrame, ipPacket);
            return Command.STOP;
        }

        // The packet is a response from the service to a user.
        if (isServerCompleteAddress(sourceMAC, sourceIP)) {
            logger.info("The packet is a response from the server transiting through an access switch.");
            handleResponseFromServer(sw, packetIn, ethernetFrame, ipPacket);
            return Command.STOP;
        }

        // The packet is transiting through the network.
        logger.info("The packet is transiting through the network.");
        return Command.CONTINUE;
	}
	
	private void instructSwitchWhenResponseFromServer(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame,
            IPv4 ipPacket, OFPort outputPort) {
		/*
		OFFlowAdd.Builder flowModBuilder = sw.getOFFactory().buildFlowAdd();
		Match match = createMatchWhenResponseFromServer(sw, ethernetFrame, ipPacket);
		ArrayList<OFAction> actionList = translateSourceAddressIntoVirtual(sw, outputPort);
		
		flowModBuilder.setIdleTimeout(IDLE_TIMEOUT);
		flowModBuilder.setHardTimeout(HARD_TIMEOUT);
		flowModBuilder.setBufferId(OFBufferId.NO_BUFFER);
		flowModBuilder.setOutPort(OFPort.ANY);
		flowModBuilder.setCookie(U64.of(0));
		flowModBuilder.setPriority(FlowModUtils.PRIORITY_MAX);
		flowModBuilder.setMatch(match);
		flowModBuilder.setActions(actionList);
		
		sw.write(flowModBuilder.build());
		

		OFPacketOut.Builder packetOutBuilder = sw.getOFFactory().buildPacketOut();
		packetOutBuilder.setBufferId(packetIn.getBufferId());
		packetOutBuilder.setInPort(OFPort.ANY);
		packetOutBuilder.setActions(actionList);
		
		// If the packet-in encapsulates the original packet, the packet is sent back.
		if (packetIn.getBufferId() == OFBufferId.NO_BUFFER)
		packetOutBuilder.setData(packetIn.getData());
		
		sw.write(packetOutBuilder.build());
		*/
	}
	
	private void handleResponseFromServer(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
        MacAddress userMAC = ethernetFrame.getDestinationMACAddress();
        logger.info("INFO: handleResponseFromServer ");
        /*
        
        */
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
    private Path getShortestPath(DatapathId startSwitch, SwitchPort[] endSwitches) {
        Path shortestPath = null;

        for (SwitchPort endSwitch : endSwitches) {
            Path candidateShortestPath = routingService.getPath(startSwitch, OFPort.of(1),
                                                                  endSwitch.getNodeId(), endSwitch.getPortId());
                                                                
            if (candidateShortestPath == null)
                continue;

            if (shortestPath == null) {
                shortestPath = candidateShortestPath;
                continue;
            }

            if (candidateShortestPath.compareTo(shortestPath) < 0)
                shortestPath = candidateShortestPath;
        }

        return shortestPath;
    }
	
	private void handleRequestToResource(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame, IPv4 ipPacket) {
		
		IPv4Address resource_address = ipPacket.getDestinationAddress();
		logger.info("handleRequestToResource");
		
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
		} else {
			logger.info("Subscribers list found for: " + resource_address.toString());
		}
		
		// Create a flow table modification message to add a rule
		/*
		OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
		
        fmb.setIdleTimeout(IDLE_TIMEOUT);
        fmb.setHardTimeout(HARD_TIMEOUT);
        fmb.setBufferId(OFBufferId.NO_BUFFER);
        fmb.setOutPort(OFPort.ANY);
        fmb.setCookie(U64.of(0));
        fmb.setPriority(FlowModUtils.PRIORITY_MAX);

        // Create the match structure  
        Match.Builder mb = sw.getOFFactory().buildMatch();
        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
        .setExact(MatchField.IPV4_DST, resource_address)
        .setExact(MatchField.ETH_DST, LOAD_BALANCER_MAC);
        */
        
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
                if (shortestPath == null)
                    continue;
                
                // The output port of the current switch is specified by the second element of the path.
                OFPort outputPort = shortestPath.getPath().get(1).getPortId();
                logger.info(outputPort.toString());
                
	        	/*for(int i = 0; i < ports.length; i++) {
	        		
	        		logger.info("Port: " + ports[i].getPortId());
	        		
	        		outputPort = ports[i].getPortId();
	        		
	        		if (outputPort == null) {
	                    logger.info("The user is not connected anymore to this access switch. Dropping the packet.");
	                    return;
	                }*/
	        			        		
	    			// Generate ARP reply
	    			IPacket tcpPacket = new Ethernet()
	    				.setSourceMACAddress(SERVER_MAC)
	    				.setDestinationMACAddress(ethernetFrame.getSourceMACAddress())
	    				.setEtherType(EthType.IPv4)
	    				.setPriorityCode(ethernetFrame.getPriorityCode())
	    				.setPayload(
	    					new TCP()
	    						.setDestinationPort(TransportPort.of(outputPort.getPortNumber()))
	    						.setSourcePort(TransportPort.of(packetIn.getMatch().get(MatchField.IN_PORT).getPortNumber())));
	    			
	    			// Set the ICMP reply as packet data 
	    			byte[] packetData = tcpPacket.serialize();

	    			OFPacketOut po = sw.getOFFactory().buildPacketOut()
	    				    .setData(packetData)
	    				    .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(outputPort, 0xffFFffFF)))
	    				    .setInPort(OFPort.ANY)
	    				    .build();
	    				  
	    			sw.write(po);
	    			
                    logger.info("Sent packet-out on the outport specified.");

	        	//}
			}		
			
			/*// Create the Packet-Out and set basic data for it (buffer id and in port)
			OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
			pob.setBufferId(OFBufferId.NO_BUFFER);
			pob.setInPort(OFPort.CONTROLLER);
			
			// Create action -> send the packet back from the source port
			OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
			// The method to retrieve the InPort depends on the protocol version 
			OFPort inPort = packetIn.getMatch().get(MatchField.IN_PORT);
			actionBuilder.setPort(inPort); 
			
			// Assign the action
			pob.setActions(Collections.singletonList((OFAction) actionBuilder.build()));
			*/
			
			
				
			//pob.setData(packetData);
			
			//sw.write(pob.build());

			/*
			 OFActions actions = sw.getOFFactory().actions();
	        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
	        OFOxms oxms = sw.getOFFactory().oxms();
	        // Set dstIp as the destination address for the incoming packet
	        OFActionSetField setDstIp = actions.buildSetField()
	        	    .setField(
	        	        oxms.buildIpv4Dst()
	        	        .setValue(IPv4Address.of(subscriber.getValue()))
	        	        .build()
	        	    ).build();
	        actionList.add(setDstIp);
			
	        logger.info("Subscriber: " + subscriber.getValue());
			Iterator<? extends IDevice> dstDev = deviceManagerService.queryDevices(MacAddress.NONE, VlanVid.ZERO, IPv4Address.NONE, IPv6Address.NONE, DatapathId.NONE, OFPort.ZERO);
        	
        	while(dstDev.hasNext()) {
 
        		IDevice device =  dstDev.next();
        		logger.info("Dev: " + device.toString());
        		
	        	OFActionSetField setDestMAC = actions.buildSetField()
	        			.setField(
	        					oxms.buildEthDst()
	        					.setValue(device.getMACAddress())
	        					.build()
	        					).build();
	        	actionList.add(setDestMAC);
	        	
	        	SwitchPort[] ports = device.getAttachmentPoints();
	        	
	        	for(int i = 0; i < ports.length; i++) {
	        		// logger.info("Port: " + ports[i].getPort());
	        		// OFPort outputPort = ports[i].getPort();
	        		OFPort outputPort = OFPort.of(i);
	        		
	        		if (outputPort == null) {
	                    logger.info("The user is not connected anymore to this access switch. Dropping the packet.");
	                    return;
	                }
	        		
	                instructSwitchWhenRequestToServer(sw, packetIn, ethernetFrame, ipPacket, MacAddress.of(subscriber.getKey()), IPv4Address.of(subscriber.getValue()), outputPort, resource_address);

	        	}
	      	}        
	      	*/      
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
	
	

	
	private void instructSwitchWhenRequestToServer(IOFSwitch sw, OFPacketIn packetIn, Ethernet ethernetFrame,
        IPv4 ipPacket, MacAddress serverMAC, IPv4Address serverIP,
        OFPort outputPort, IPv4Address resource_address) {
		
		logger.info("Server MAC: " + serverMAC.toString() + ", server IP: " + serverIP.toString() + ", outputPort: " + outputPort.toString());

		/*
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
		
		OFPacketOut.Builder packetOutBuilder = sw.getOFFactory().buildPacketOut();
		packetOutBuilder.setBufferId(packetIn.getBufferId());
		packetOutBuilder.setInPort(OFPort.ANY);
		packetOutBuilder.setActions(actionList);
		
		// If the packet-in encapsulates the original packet, the packet is sent back.
		if (packetIn.getBufferId() == OFBufferId.NO_BUFFER)
		packetOutBuilder.setData(packetIn.getData());
		
		sw.write(packetOutBuilder.build());
		*/
		
		// Create a flow table modification message to add a rule
		OFFlowAdd.Builder fmb = sw.getOFFactory().buildFlowAdd();
		
        fmb.setIdleTimeout(IDLE_TIMEOUT);
        fmb.setHardTimeout(HARD_TIMEOUT);
        fmb.setBufferId(OFBufferId.NO_BUFFER);
        fmb.setOutPort(OFPort.ANY);
        fmb.setCookie(U64.of(0));
        fmb.setPriority(FlowModUtils.PRIORITY_MAX);

        // Create the match structure  
        MacAddress userMAC = ethernetFrame.getSourceMACAddress();
        IPv4Address userIP = ipPacket.getSourceAddress();
        Match.Builder matchBuilder = sw.getOFFactory().buildMatch();

        matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
                .setExact(MatchField.ETH_SRC, userMAC)
                .setExact(MatchField.IPV4_SRC, userIP)
                .setExact(MatchField.ETH_DST, SERVER_MAC)
                .setExact(MatchField.IPV4_DST, resource_address);
        
        OFActions actions = sw.getOFFactory().actions();
        // Create the actions (Change DST mac and IP addresses and set the out-port)
        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        
        OFOxms oxms = sw.getOFFactory().oxms();

        OFActionSetField setDlDst = actions.buildSetField()
        	    .setField(
        	        oxms.buildEthDst()
        	        .setValue(serverMAC)
        	        .build()
        	    )
        	    .build();
        actionList.add(setDlDst);

        OFActionSetField setNwDst = actions.buildSetField()
        	    .setField(
        	        oxms.buildIpv4Dst()
        	        .setValue(serverIP)
        	        .build()
        	    ).build();
        actionList.add(setNwDst);
        
        OFActionOutput output = actions.buildOutput()
        	    .setMaxLen(0xFFffFFff)
        	    .setPort(outputPort)
        	    .build();
        actionList.add(output);
        
        
        fmb.setActions(actionList);
        fmb.setMatch(matchBuilder.build());

        sw.write(fmb.build());
        
        // Reverse Rule to change the source address and mask the action of the controller
        
		// Create a flow table modification message to add a rule
		OFFlowAdd.Builder fmbRev = sw.getOFFactory().buildFlowAdd();
		
		fmbRev.setIdleTimeout(IDLE_TIMEOUT);
		fmbRev.setHardTimeout(HARD_TIMEOUT);
		fmbRev.setBufferId(OFBufferId.NO_BUFFER);
		fmbRev.setOutPort(OFPort.CONTROLLER);
		fmbRev.setCookie(U64.of(0));
		fmbRev.setPriority(FlowModUtils.PRIORITY_MAX);

        Match.Builder mbRev = sw.getOFFactory().buildMatch();
        mbRev.setExact(MatchField.ETH_TYPE, EthType.IPv4)
        .setExact(MatchField.IPV4_SRC, serverIP)
        .setExact(MatchField.ETH_SRC, serverMAC);
        
        ArrayList<OFAction> actionListRev = new ArrayList<OFAction>();
        
        OFActionSetField setDlDstRev = actions.buildSetField()
        	    .setField(
        	        oxms.buildEthSrc()
        	        .setValue(SERVER_MAC)
        	        .build()
        	    )
        	    .build();
        actionListRev.add(setDlDstRev);

        OFActionSetField setNwDstRev = actions.buildSetField()
        	    .setField(
        	        oxms.buildIpv4Src()
        	        .setValue(serverIP)
        	        .build()
        	    ).build();
        actionListRev.add(setNwDstRev);
        
        OFActionOutput outputRev = actions.buildOutput()
        	    .setMaxLen(0xFFffFFff)
        	    .setPort(OFPort.of(1))
        	    .build();
        actionListRev.add(outputRev);
        
        fmbRev.setActions(actionListRev);
        fmbRev.setMatch(mbRev.build());
        
        sw.write(fmbRev.build());

        // If we do not apply the same action to the packet we have received and we send it back the first packet will be lost
        
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
	}

	private IPacket createArpReplyForService(Ethernet ethernetFrame, ARP arpRequest, IPv4Address resource_virtual_address) {
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
                logger.info("The ARP request belong to a virtual address of a resource, so build a reply");
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
	        	if (!isServerCompleteAddress(sourceMAC, ((IPv4) packet).getSourceAddress())) {
	        		logger.info("The packet is an IP request coming from an user and not addressed " +
                            "to a resource. Dropping the packet.");
	        		return true;
	        	}
	            
	        }
	        
	        logger.info("The packet is an IP request or a reply. Accepting the packet.");
	        
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
