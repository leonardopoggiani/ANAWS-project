package net.floodlightcontroller.unipi.rest;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class DistributedBrokerWebRoutable implements RestletRoutable{
	
	@Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);

        router.attach("/users/json", User.class);
        router.attach("/publishers/json", Publisher.class);
        router.attach("/subscribers/json", Subscriber.class);
        router.attach("/resources/json", Resource.class);
        
        return router;
    }

    @Override
    public String basePath() {
        return "/db";
    }
}
