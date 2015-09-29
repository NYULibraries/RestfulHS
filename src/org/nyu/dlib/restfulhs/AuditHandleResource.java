package org.nyu.dlib.restfulhs;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Date;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ListHandlesRequest;
import net.handle.hdllib.ListHandlesResponse;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.ResponseMessageCallback;
import net.handle.hdllib.SecretKeyAuthenticationInfo;
import net.handle.hdllib.ServerInfo;
import net.handle.hdllib.SiteInfo;
import net.handle.hdllib.Util;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * This class is used to perform an audit on a handle. It only supports
 * The GET method for now. Basically when a hadnlde it pass it it checks
 * to see if the url in the handle resolves, and ofcourse if the handle actually
 * exists.
 *
 * @author Nathan Stevens
 * @date Nov 22, 2010;
 */
public class AuditHandleResource extends ServerResource implements ResponseMessageCallback {

    private final String HANDLE_OK = "OK";
    private final String HANDLE_BROKEN = "BROKEN";
    private final String HANDLE_NOT_FOUND = "NOT FOUND";

    private String prefix = ""; // the handle prefix
    private String handle = ""; // the handle
    private HandleResolver resolver; // used to find handles
    private String completeHandle = null; // This is a combination of the prefix and suffix
    private String adminHandle = ""; // the admin handle. this should probable be passed in with the XML file
    private boolean hasAuthInfo = false; // whether or not authentication info is present

    private StringBuilder sb = null; // Used to store a list of handles

    /**
     * The default constructor. This does nothing for now
     */
    public AuditHandleResource() {
    }

    /**
     * This method is called when this object is loaded by the router
     * it just constructs the handle
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
     * Method to run an audit on a handle. The audit consist of seeing if the
     * handle exists, and the url it's bound to can actually be read from.
     *
     * The text that is returned is a tab seperated line consisting of the
     * following :
     * Handle, Status, Bound URL, Date
     * 10767/test999{tab}OK{tab}http://www.nyu.edu{tab}
     *
     * If the handle can't be found then only handle and status are return
     * 10767/test999{tab}NOT FOUND
     *
     * If the handle link is broken then the status
     * 10767/test99{tab}BROKEN{tab}http://www.brokenlink.org{tab}
     *
     * @return String containing the audit info
     */
    @Get("text")
    public String auditHandle() {
        // looking up the handle now
        resolver = new HandleResolver();

        if (handle == null) {
            return getListOfHandles();
        }

        String auditText = "";

        try {
            ResolutionRequest req = new ResolutionRequest(completeHandle.getBytes("UTF8"), null, null, null);
            AbstractResponse response = resolver.processRequest(req);

            if (response.responseCode == AbstractMessage.RC_SUCCESS) {
                HandleValue values[] = ((ResolutionResponse) response).getHandleValues();

                // now need to find the url
                String url = "";
                Date modifyDate = null;
                String description = "N/A";

                for (int i = 0; values != null && i < values.length; i++) {
                    if (values[i] != null && values[i].getIndex() == HandleResource.URL_INDEX) {
                        url = values[i].getDataAsString();
                        modifyDate = values[i].getTimestampAsDate();
                    } else if(values[i] != null && values[i].getIndex() == HandleResource.DESC_INDEX) {
                        description = values[i].getDataAsString();
                    }
                }

                // now try to read from this url
                if (url.length() == 0) {
                    auditText = completeHandle + " \t " + HANDLE_BROKEN;
                } else {
                    auditText = verifyUrl(url, modifyDate, description);
                }
            } else {
                // handle not found
                getResponse().setStatus(new Status(404), response.toString());

                auditText = completeHandle + " \t " + HANDLE_NOT_FOUND;
            }
        } catch (Throwable t) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Locate Handle Server");
        }

        return auditText;
    }

    /**
     * Method to check to see if url can be actually resolved correctly
     * 
     * @param strUrl The url to check
     * @param modifyDate The date the url was created or last updated
     * @param description A brief describe of the object the handle points to
     * @return String containing the status for the whole handle
     */
    private String verifyUrl(String strUrl, Date modifyDate, String description) {
        String auditText = "";

        // Prepare HTTP get
        GetMethod get = new GetMethod(strUrl);

        // Get the HTTP client
        HttpClient httpclient = new HttpClient();

        // Execute request
        try {
            int statusCode = httpclient.executeMethod(get);

            // if status code doesn't equal to success throw exception
            if (statusCode == HttpStatus.SC_OK) {
                auditText = completeHandle + " \t " + HANDLE_OK + " \t " + strUrl + " \t "  + modifyDate + " \t " + description;
            } else { // something wrong with url link, so just stat it broken
                auditText = completeHandle + " \t " + HANDLE_BROKEN + " \t " + strUrl + " \t " + modifyDate + " \t " + description;
            }

            get.releaseConnection();
        } catch (UnknownHostException uhe) {
            // can't find the host, so set this link has broken
            auditText = completeHandle + " \t " + HANDLE_BROKEN + " \t " + strUrl + " \t " + modifyDate + " \t " + description;
        } catch (ConnectException ce) {
            // connection refused to the host, so set this link has broken
            auditText = completeHandle + " \t " + HANDLE_BROKEN + " \t " + strUrl + " \t " + modifyDate + " \t " + description;
        } catch (Exception e) {
            e.printStackTrace();
            get.releaseConnection();
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Audit Handle");
        } finally {
            // Release current connection to the connection pool
            // once you are done
            get.releaseConnection();
        }

        return auditText;
    }

    /**
     * Method to list all handles for the followng prefix
     * @return The list of handles
     */
    private String getListOfHandles() {
        // check to make sure the authentication information was found before
        // continuing
        if (!hasAuthInfo) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to Find Authentication Information");

            return ""; // return blank string
        }

        // initialize the string builder which hold the handle
        sb = new StringBuilder();

        // now get the list of handles
        AbstractResponse response = null;

        try {
            // First create the authentication object
            AuthenticationInfo authInfo = getAuthenticationInfo();

            // Creat the admin record to access the handle server.
            AdminRecord admin = new AdminRecord(adminHandle.getBytes("UTF8"), 300,
                    true, true, true, true, true, true,
                    true, true, true, true, true, true);

            // need to find local sites for resolving the handles
            ResolutionRequest resReq =
                    new ResolutionRequest(Util.encodeString(prefix + "/test"), null, null, null);
            SiteInfo[] sites = resolver.findLocalSites(resReq);

            // create a list handle request object to actually list the handles
            ListHandlesRequest req = new ListHandlesRequest(Util.encodeString("0.NA/" + prefix), authInfo);

            for (int i = 0; i < sites[0].servers.length; i++) {
                ServerInfo server = sites[0].servers[i];
                response = resolver.sendRequestToServer(req, server, this);
            }

            return sb.toString();
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unable to get list of handles >> " + response.toString());
            return "";
        }
    }

    /**
     * This method needs to be implements to get the complete list of handles
     * apparently?
     *
     * @param abstractResponse
     * @throws HandleException
     */
    public void handleResponse(AbstractResponse abstractResponse) {
        if (abstractResponse instanceof ListHandlesResponse) {
            try {
                ListHandlesResponse lhResp = (ListHandlesResponse) abstractResponse;
                byte handles[][] = lhResp.handles;

                for (int i = 0; i < handles.length; i++) {
                    String currentHandle = Util.decodeString(handles[i]);
                    sb.append(currentHandle).append("\n");

                    //System.out.println(currentHandle);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }
        } else if (abstractResponse.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
            System.err.println(abstractResponse);
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
                authInfo = new PublicKeyAuthenticationInfo(adminHandle.getBytes("UTF8"),
                        300, RestfulHSUtils.getPrivateKey(prefix));
            } else {
                authInfo = new SecretKeyAuthenticationInfo(adminHandle.getBytes("UTF8"), 300, RestfulHSUtils.getSecretKey(prefix));
            }

            return authInfo;
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Problem loading security keys");
            throw new Exception();
        }
    }
}
