/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.nyu.dlib.restfulhs;

import java.util.Properties;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * This is the class that actually implements the restful interface for the
 * Handle Server system.
 *
 * @author Nathan Stevens
 */
class RestfulHS extends Application {
    // The properties object which contains private key and passphrase info
    public static Properties properties = new Properties();

    /**
     * Default constructor
     */
    public RestfulHS() { }

    /**
     * Constructor that takes a properties object
     * @param properties
     */
    public RestfulHS(Properties properties) {
        this.properties = properties;
    }

    /**
     * Method to return the router
     */
    @Override
    public Restlet createInboundRoot() {
        // Create a router
        Router router = new Router(getContext());

        // Attach the resource to the router which takes care of request
        // for handle creation, updating, and deletion
        router.attach("/id/handle/{prefix}/{handle}", HandleResource.class);
        router.attach("/id/handle/{prefix}", HandleResource.class);

        // Attach the resource to the router which takes care of request
        // to audit a handle
        router.attach("/audit/handle/{prefix}/{handle}", AuditHandleResource.class);
        router.attach("/audit/handle/{prefix}", AuditHandleResource.class);
        
        // see whether to restrict client connections only from the localhost
        String allowFrom = properties.getProperty("allow.connection");

        if(!allowFrom.equals("localhost")) {
            return router;
        } else {
            IPAuthorizer authorizer = new IPAuthorizer();
            authorizer.setNext(router);
            return authorizer;
        }
    }
}
