/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nyu.dlib.restfulhs;

import java.io.IOException;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AddValueRequest;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.SecretKeyAuthenticationInfo;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is used to handle the creation, modification of handles by using
 * the handle.net client library. The handles created a simple ones which just bound
 * one URL to the handle.
 *
 * @author Nathan Stevens
 */
public class HandleResource extends ServerResource {

    // base address of all handles that are created
    private final String HANDLE_SERVER_URL = "http://hdl.handle.net/";

    public static final int URL_INDEX = 1; // the index where the url is stored
    public static final int DESC_INDEX = 10; // the index where the description is stored

    private String prefix = ""; // the handle prefix
    private String handle = ""; // the handle
    private String adminHandle = ""; // the admin handle. this should probable be passed in with the XML file
    private boolean hasAuthInfo = false; // whether or not authentication info is present
    private String handleXML = ""; // since the xml is so simple just replace the part we need
    private HandleResolver resolver; // used to find handles
    private String completeHandle = null; // This is a combination of the prefix and suffix

    /**
     * The default constructor which initialize the return string
     */
    public HandleResource() {
        // This string stores the template for xml representation of the handle.
        // Normally,XML creation should be done using a Document,object, but
        // it such a simple document it faster to do it this way.
        // ::url:: should be replaced with the actual URL
        // ::handle:: should be replaced by the complet handle variable
        // ::response:: is replace with the handle server response
        handleXML = "<?xml version=\"1.0\" ?> "
                + "<hs:info xmlns:hs=\"info:nyu/dl/v1.0/identifiers/handles\"> "
                + "<hs:binding> "
                + "::url:: "
                + "</hs:binding> "
                + "<hs:location> "
                + "::location::"
                + "</hs:location>"
                + "<hs:response> "
                + "::response:: "
                + "</hs:response> "
                + "</hs:info> ";
    }

    /**
     * This method is called when this object is loaded by the router
     */
    @Override
    public void doInit() {
        this.prefix = (String) getRequestAttributes().get("prefix");
        this.handle = (String) getRequestAttributes().get("handle");

        // Check to see if to get a handle from the noid minter
        // if just the prefix was returned
        if (handle != null) {
            this.completeHandle = prefix.trim() + "/" + handle.trim();
        }

        this.adminHandle = RestfulHS.properties.getProperty(prefix + ".adminHandle");
        
        // check to make sure that the private key file and passphrase were entered
        hasAuthInfo = RestfulHSUtils.hasAuthInfo(prefix);
    }

    /**
     * This methods returns the XML representation of the Handle in the
     * format below
     *
     * <?xml version="1.0" ?>
     * <hs:info xmlns:hs="info:nyu/dl/v1.0/identifiers/handles">
     * <hs:binding>
     *   URL
     * </hs:binding>
     * <hs:response>
     *   content of handle server response
     * </hs:response>
     * </hs:info>
     *
     * @return Return xml represention of handle, including response from server
     */
    @Get("xml")
    public String getHandle() {
        // looking up the handle now
        resolver = new HandleResolver();

        // TODO 11/2/2010 Handle the case where no handle is provided, just the prefix.
        // In that case a list of all handles on server should returned

        try {
            ResolutionRequest req = new ResolutionRequest(completeHandle.getBytes("UTF8"), null, null, null);
            AbstractResponse response = resolver.processRequest(req);

            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                HandleValue values[] = ((ResolutionResponse) response).getHandleValues();

                // now need to find the url
                String url = "";
                for (int i = 0; values != null && i < values.length; i++) {
                    if (values[i] != null && values[i].getTypeAsString().equals("URL")) {
                        url = values[i].getDataAsString();
                        break; // break out of loop since we found the url
                    }
                }

                // update the template xml by just replacing the text
                updateHandleXML(url, response.toString());

                return handleXML;
            } else {
                getResponse().setStatus(new Status(404), response.toString());
                return "";
            }
        } catch (Throwable t) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Locate Handle Server");
            return "";
        }
    }

    /**
     * This either updates an existing Handle, or create a new Handle
     *
     * @param entity The request entity
     * @return an xml string including the response of the Handle Server
     */
    @Put("xml")
    public String createOrUpdateHandle(Representation entity) {
        // check to make sure the authentication information was found before
        // continuing
        if (!hasAuthInfo) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Find Authentication Information");

            return ""; // return blank string
        }

        // check to make sure that the PUT method is not being used to creat
        // a noid handle. only post should do that.
        if(handle == null) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Please Use the POST method to create a NOID handle");

            return ""; // return blank string
        }

        // get the xml representation of the request
        try {
            String[] handleInfo = getHandleInformation(entity);
            String url = handleInfo[0];
            String description = handleInfo[1];

            try {// now check to see if the handle exist. If it doesn't then create it,
                if (!handleExists()) {
                    createHandle(url, description);
                } else { // updating the handle
                    updateHandle(url, description);
                }

                // return the handle information in xml format
                return handleXML;
            } catch (Exception e) {
                return "";
            }
        } catch (IOException ioe) { // unable to validate xml
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid XML");
            return "";
        }
    }

    /**
     * This or create a new Handle using a noid from eihter the development
     * or production noid minter depending on the prefix. This could have
     * been done in the method above, but for future flexibility and code
     * clarity it's done in seperate method
     *
     * @param entity The request entity
     * @return an xml string including the response of the Handle Server
     */
    @Post("xml")
    public String createNoidHandle(Representation entity) {
        // check to make sure the authentication information was found before
        // continuing
        if (!hasAuthInfo) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Find Authentication Information");

            return ""; // return blank string
        }

        // get the noid handle and set the complete handle
        handle = NoidMinterUtil.getHandle(prefix);
        completeHandle = prefix.trim() + "/" + handle.trim();

        // check to see if a valid noid was returned. If not let the user know
        // that this not a valid noid and no handle was created
        if(handle.equals(NoidMinterUtil.ERROR_NOID)) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to get a valid noid from NOID Minter");

            return ""; // return blank string
        }

        // get the xml representation of the request
        try {
            String[] handleInfo = getHandleInformation(entity);
            String url = handleInfo[0];
            String description = handleInfo[1];

            try {// now check to see if the handle exist. If it doesn't then create it,
                if(!handleExists()) {
                    createHandle(url, description);
                }
                
                // return the handle information in xml format
                getResponse().setStatus(Status.SUCCESS_CREATED, 
                        "Created NOID Handle (" + HANDLE_SERVER_URL  + completeHandle  + ")");

                return handleXML;
            } catch (Exception e) {
                return "";
            }
        } catch (IOException ioe) { // unable to validate xml
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid XML");
            return "";
        }
    }

    /**
     * Method to check to see if a handle exists before updating it, or
     * creating a new handle
     *
     * @return boolean saying if the handle already exists
     */
    private boolean handleExists() throws Exception {
        try {
            // looking up the handle now
            resolver = new HandleResolver();

            ResolutionRequest req = new ResolutionRequest(completeHandle.getBytes("UTF8"), null, null, null);
            AbstractResponse response = resolver.processRequest(req);

            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Locate Handle Server");
            throw new Exception();
        }
    }

    /**
     * Method to actually create a new handle and set the URL
     *
     * @param url The URL of this handle
     * @param description  This is the description of the handle
     */
    private void createHandle(String url, String description) throws Exception {
        AbstractResponse response = null;

        try {
            // First create the authentication object
            AuthenticationInfo authInfo = getAuthenticationInfo();

            // Creat the admin record for the handle.
            AdminRecord admin = new AdminRecord(adminHandle.getBytes("UTF8"), 300,
                    true, true, true, true, true, true,
                    true, true, true, true, true, true);

            // now create the actual handle, but first get the current time stamp since
            // all handles need this
            int timestamp = (int) (System.currentTimeMillis() / 1000);

            // add handle value which just consist of ADMIN handle and URL data, and if the
            // description is provided add that to
            HandleValue[] values;
            
            if(description.length() == 0) {
                values = new HandleValue[] {
                    new HandleValue(100, "HS_ADMIN".getBytes("UTF8"),
                    Encoder.encodeAdminRecord(admin),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false),
                    new HandleValue(URL_INDEX, "URL".getBytes("UTF8"), url.getBytes("UTF8"))};
            } else {
                values = new HandleValue[] {
                    new HandleValue(100, "HS_ADMIN".getBytes("UTF8"),
                    Encoder.encodeAdminRecord(admin),
                    HandleValue.TTL_TYPE_RELATIVE, 86400,
                    timestamp, null, true, true, true, false),
                    new HandleValue(URL_INDEX, "URL".getBytes("UTF8"), url.getBytes("UTF8")),
                    new HandleValue(DESC_INDEX, "DESC".getBytes("UTF8"), description.getBytes("UTF8"))};
            }

            // Now we can build our CreateHandleRequest object.
            CreateHandleRequest req =
                    new CreateHandleRequest(completeHandle.getBytes("UTF8"), values, authInfo);

            // Finally, send the message by calling the processRequest method of the resolver
            // object with the request
            response = resolver.processRequest(req);

            // The responseCode value for a response indicates the status of
            // the request.
            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                // update the template xml by just replacing the text
                updateHandleXML(url, response.toString());
            } else {
                System.out.println("\nGot Error: \n" + response);
                throw new Exception();
            }
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to create handle >> " + response.toString());
            throw new Exception();
        }
    }

    /**
     * Method to update the url of a specific handle
     *
     * @param url The update Url
     * @param description if one was provided
     * @throws Exception Thrown if the handle doesn't already exists
     */
    private void updateHandle(String url, String description) throws Exception {
        AbstractResponse modresp = null;
        AbstractResponse addresp = null;

        try {
            // First create the authentication object
            AuthenticationInfo authInfo = getAuthenticationInfo();

            //Get the resolution request object
            ResolutionRequest rreq = new ResolutionRequest(completeHandle.getBytes("UTF8"),
                    null, null, null);
            rreq.authoritative = true;

            AbstractResponse rresp = resolver.processRequest(rreq);

            HandleValue[] values = null;
            HandleValue[] updateValues = null;

            if (rresp != null && rresp instanceof ResolutionResponse) {
                // get the current time stamp
                int timestamp = (int) (System.currentTimeMillis() / 1000);

                values = ((ResolutionResponse) rresp).getHandleValues();

                // see how many values need to be updated.
                if(description.length() != 0 && values.length > 2) {
                    updateValues = new HandleValue[2];
                } else {
                    updateValues = new HandleValue[1];
                }

                // find the handle value with the URL index then update it
                for (int i = 0; values != null && i < values.length; i++) {
                    // update the url value which is at index 1, and the timestamp
                    if (values[i] != null && values[i].getIndex() == URL_INDEX) {
                        values[i].setData(url.getBytes("UTF8"));
                        values[i].setTimestamp(timestamp);

                        updateValues[0] = values[i]; // store this value
                    }

                    // update the description values if it is present
                    if (values[i] != null && values[i].getIndex() == DESC_INDEX
                            && updateValues.length == 2) {
                        values[i].setData(description.getBytes("UTF8"));
                        values[i].setTimestamp(timestamp);

                        updateValues[1] = values[i]; // store this value
                    }
                }
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                        "Error response for hdl '" + completeHandle + "': " + rresp);

                handleXML = "";
            }

            // see whether to add a description value
            if(description.length() != 0 && updateValues.length == 1) {
                HandleValue descValue = new HandleValue(DESC_INDEX, "DESC".getBytes("UTF8"), description.getBytes("UTF8"));
                AddValueRequest addreq = new AddValueRequest(rreq.handle, descValue, authInfo);
                addresp = resolver.processRequest(addreq);
            }

            // now update the handle value
            ModifyValueRequest modreq = new ModifyValueRequest(rreq.handle, updateValues, authInfo);       
            modresp = resolver.processRequest(modreq);

            // succesfully modified the handle so return the updated xml
            if (modresp != null && modresp.responseCode == AbstractMessage.RC_SUCCESS) {
                updateHandleXML(url, modresp.toString());
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                        "Error modifying handle '" + completeHandle + "': " + modresp);
                
                String em = modresp.toString();
                System.out.println("Error Message: " + em);

                throw new Exception();
            }

        } catch (Exception e) {
            String errorMessage = "";

            if(addresp != null) {
                errorMessage = modresp.toString() + ", " + modresp.toString();
            } else {
                errorMessage = modresp.toString();
            }

            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to update handle >> " + errorMessage);

            throw new Exception();
        }
    }

    /**
     * Method to delete a handle. This is used for testing purposes only since in a
     * production environment, handles should be persistent from the time they are
     * created till end of handle system fails.
     */
    @Delete
    public String deleteHandle() {
        try {
            // First create the authentication object
            AuthenticationInfo authInfo = getAuthenticationInfo();

            // check to see if handle exists
            if (handleExists()) {
                DeleteHandleRequest req =
                        new DeleteHandleRequest(completeHandle.getBytes("UTF8"), authInfo);

                AbstractResponse response = resolver.processRequest(req);

                if (response == null || response.responseCode != AbstractMessage.RC_SUCCESS) {
                    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, response.toString());

                    System.out.println("error deleting " + response);

                    return response.toString();
                } else {
                    getResponse().setStatus(Status.SUCCESS_OK, response.toString());

                    System.out.println("deleted ");

                    return response.toString();
                }
            } else {
                getResponse().setStatus(new Status(404), "Unable to locate handle");
                return "Handle not found";
            }
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to delete handle");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Method to return the authentication information when creating or editing
     * a handle
     *
     * @return The authentication info object
     * @throws Exception If anything goes wrong trying to create the authentication object
     */
    private AuthenticationInfo getAuthenticationInfo() throws Exception {
        try {
            AuthenticationInfo authInfo = null;

            if (RestfulHSUtils.isPubKeyAuthentication(prefix)) {
                authInfo = new PublicKeyAuthenticationInfo(adminHandle.trim().getBytes("UTF8"),
                        300, RestfulHSUtils.getPrivateKey(prefix));
            } else {
                authInfo = new SecretKeyAuthenticationInfo(adminHandle.trim().getBytes("UTF8"),
                        300, RestfulHSUtils.getSecretKey(prefix));
            }

            return authInfo;
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Problem loading security keys");
            throw new Exception();
        }
    }

    /**
     * Method to update the xml that is returned to the requesting client.
     * We simple do string replacements to create the corrent xml
     *
     * @param url The current, new, or update url of the handle
     * @param responseText The response text from the Handle server
     */
    private void updateHandleXML(String url, String responseText) {
        // update the template xml by just replacing the text
        handleXML = handleXML.replaceFirst("::url::", url);
        handleXML = handleXML.replaceFirst("::location::", HANDLE_SERVER_URL  + completeHandle);
        handleXML = handleXML.replaceFirst("::response::", responseText);
    }

    /**
     * Method to get the url and description from the XML document submitted.
     * Since description is optional, if there is any kind of error getting it
     * then blank string is returned
     * 
     * @param entity
     * @return The url extracted from xml
     * @throws Exception An exception if anything goes wrong
     */
    private String[] getHandleInformation(Representation entity) throws IOException {
        DomRepresentation representation = new DomRepresentation(entity);
        Document doc = representation.getDocument();
        
        // The url and description string    
        String url = "";
        String description = "";
        
        // get the root element then the url this handle should be bound to
        Element root = doc.getDocumentElement();
        Element urlElement = (Element) root.getElementsByTagName("hs:binding").item(0);
        url = urlElement.getTextContent();

        // try to get the description, but since it's not required it may not have
        // been provided in the xml so catch any errors and do nothing
        try {
            Element descriptionElement = (Element) root.getElementsByTagName("hs:description").item(0);
            description = descriptionElement.getTextContent();
        } catch (Exception e) { }

        return new String[]{url, description};
    }
}
