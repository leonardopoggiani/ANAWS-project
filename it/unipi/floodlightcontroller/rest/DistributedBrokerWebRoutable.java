package it.unipi.floodlightcontroller.rest;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class DistributedBrokerWebRoutable implements RestletRoutable{
	
	@Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);

        router.attach("/subscribers/{resource}/json", Subscriber.class);
        router.attach("/resources/json", Resource.class);
        //router.attach("/access-switches/json", AccessSwitch.class);
        
        return router;
    }

    @Override
    public String basePath() {
        return "/db";
    }
}
