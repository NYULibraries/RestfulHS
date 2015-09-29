package org.nyu.dlib.restfulhs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;

/**
 * Class used to authorize who can connect to the service based on the clients
 * IP address.
 *
 * @author Nathan Stevens
 * @date Nov 16, 2010;
 */
public class IPAuthorizer extends Authorizer {
    /**
     * Based on the client IP restrict access to the resources. Right now this
     * only allows connection from clients running on the localhost
     *
     * @param rqst
     * @param rspns
     * @return
     */
    @Override
    protected boolean authorize(Request rqst, Response rspns) {
        try {
            InetAddress addr = InetAddress.getLocalHost(); 
            String serverIP = addr.getHostAddress();

            String clientIP = rqst.getClientInfo().getAddress();

            if(clientIP.equals(serverIP) || clientIP.equals("127.0.0.1")
                    || clientIP.startsWith("0:0")) {
                return true;
            } else {
                return false;
            }
        } catch(UnknownHostException e) {
            return false;
        }
    }
}
