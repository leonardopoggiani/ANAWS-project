package net.floodlightcontroller.unipi.distributedbroker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.unipi.rest.DistributedBrokerWebRoutable;
import net.floodlightcontroller.unipi.rest.IDistributedBrokerREST;

public class DistributedMessageBroker implements IOFMessageListener, IFloodlightModule, IDistributedBrokerREST {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedMessageBroker.class);
    // private final Logger loggerREST = LoggerFactory.getLogger(IMobilitySupportREST.class);

    // Floodlight services used by the module.
    private IFloodlightProviderService floodlightProvider;
    private IRestApiService restApiService;

    // Default virtual IP and MAC addresses of the service.
    private IPv4Address SERVER_IP = IPv4Address.of("8.8.8.8");
    private MacAddress SERVER_MAC = MacAddress.of("FE:FE:FE:FE:FE:FE");

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

        return dependencies;
	}

	@Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        logger.info("Initializing distributed message broker module.");

        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
	}

	 @Override
	    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
	        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	        restApiService.addRestletRoutable(new DistributedBrokerWebRoutable());
	    }

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getSubscribedUsers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String subscribeUser(String username, MacAddress MAC) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String removeUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getVirtualAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String setVirtualAddress(IPv4Address ipv4, MacAddress MAC) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getPublishers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getSubscribers() {
		// TODO Auto-generated method stub
		return null;
	}

}
