
package org.nyu.dlib.restfulhs;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Util class to connect to the noid minter and generate a noid which will be used
 * for the handle if no handle was provided. It used the noid minter service
 *
 * @author Nathan Stevens
 * @date Oct 28, 2010;
 */
public class NoidMinterUtil {
    // noid that is returned when there is any kind of exception
    // while trying to get a noid from the server
    public static final String ERROR_NOID = "ErrorNoid";

/**
 * Method to return a handle created from the proper noid minter based on the
 * the prefix
 *
 * @param prefix The handle server prefix
 * @return a noid which is used for the handle
 */
    public static String getHandle(String prefix) {
        String strUrl = getNoidMinterUrl(prefix);

        String noids = ""; // String containing noids from server

        // Prepare HTTP get
        GetMethod get = new GetMethod(strUrl);

        // Get the HTTP client
        HttpClient httpclient = new HttpClient();

        // Execute request
        try {
            int statusCode = httpclient.executeMethod(get);

            // Display status code
            String statusMessage = "Status code: " + statusCode +
                        "\nStatus text: " + get.getStatusText();

            System.out.println(statusMessage);

            // Display response
            noids = get.getResponseBodyAsString().trim();
            System.out.println("Response body: ");
            System.out.println(noids);

            // if status code doesn't equal to success throw exception
            if(statusCode != HttpStatus.SC_OK) {
                get.releaseConnection();
                throw new Exception(statusMessage);
            }
        } catch(Exception e ) {
            get.releaseConnection();
            return ERROR_NOID;
        } finally {
            // Release current connection to the connection pool
            // once you are done
            get.releaseConnection();
        }

        // Get the part of the string that contains the noid
        return noids.substring(4);
    }

    /**
     * Return the noid minter url for the particular prefix
     * @param prefix
     * @return
     */
    public static String getNoidMinterUrl(String prefix) {
        String noidMinterUrl = (String)RestfulHS.properties.get(prefix + ".noidMinter");

        return noidMinterUrl;
    }

}
