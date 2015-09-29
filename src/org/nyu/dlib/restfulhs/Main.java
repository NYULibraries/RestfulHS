/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nyu.dlib.restfulhs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Test of implementing restful interface for the handle server. It makes use of the
 * restlet framework
 *
 * @author Nathan Stevens
 */
public class Main {
        // The default port this service runs on
        private  static final int DEFAULT_PORT = 8082;

        /**
         * @param args the command line arguments
         */
        public static void main(String[] args) {
                // need to read in the properties file which stores the location of the
                // private key file and the passphrase for the prefixes this HS supports
                if (args.length == 0) {
                        System.err.println("usage: Main <properties file>");
                        System.exit(-1);
                }

                // read in the properties
                Properties properties = new Properties();
                try {
                        properties.load(new FileInputStream(args[0]));
                } catch (IOException e) {
                        System.err.println("Unable to read properties file " + args[0]);
                }

                // get the port number from the properties file
                int port = DEFAULT_PORT;

                try {
                        port = Integer.parseInt(properties.getProperty("server.port"));
                } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                }

                // Now create a component which starts up the build in restlet server
                Component component = new Component();
                component.getServers().add(Protocol.HTTP, port);

                // Create an application
                Application application = new RestfulHS(properties);

                try {
                        // Attach the application to the component and start it
                        component.getDefaultHost().attachDefault(application);
                        component.start();
                } catch (Exception e) {
                        System.err.println("Unable to start RESRul HS server ...");
                        e.printStackTrace();
                }
        }
}
