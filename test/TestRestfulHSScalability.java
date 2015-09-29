/*
 * This is a simple class that test the scalability of the handle service running
 * on dl-rstardev.home.nyu.edu. It attempts to create 10,000 handles then deletes
 * them in one shoot
 */

import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.junit.Assert.*;

/**
 *
 * @author Nathan Stevens
 */
public class TestRestfulHSScalability {
    private static String RESTFULHS_URL = "https://dlibprod.home.nyu.edu/id/handle/";
    //private static String RESTFULHS_URL = "http://dl-rstardev.home.nyu.edu/id/handle/";
    //private static String RESTFULHS_URL = "http://localhost:8082/id/handle/";
    private static String PREFIX = "10676"; // The prefix of the development handle server
    private String handleXML = "";

    // the userid and password to access this resource
    private final String USERID = "dlts-rhs";
    private final String PASSWORD = "pl4typu5";
    private ChallengeResponse authentication;

    public TestRestfulHSScalability() {
        // set this to trust all ssl certificates otherwise an exception gets thrown
        NaiveTrustProvider.setAlwaysTrust(true);
        
        // create the authentication object
        authentication = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERID, PASSWORD);

        handleXML = "<?xml version=\"1.0\" ?> "
                + "<hs:info xmlns:hs=\"info:nyu/dl/v1.0/identifiers/handles\"> "
                + "<hs:binding> "
                + "::url:: "
                + "</hs:binding> "
                + "<hs:description> "
                + "Unit Test -- Test Handle"
                + "</hs:description>"
                + "</hs:info> ";
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Method to test scalability of the restful handle service by
     * creation and deletion of 10,000 handles.
     * delete them all.
     */
    @Test
    public void testScalability() {
        try {

            ClientResource handleResource = null;
            Representation representation = null;

            int min = 10200;
            int max = 10300;
            //int min = 10200; // The minimum number of the handle
            //int max = 20200; // The maximum number of the handle
            //int min = 20300;
            //int max = 30300;
            //int min = 30400;
            //int max = 40400;

            // create ten handles now
            for (int i = min; i <= max; i++) {
                String url = "http://localhost/handel_" + i + "_" + System.currentTimeMillis();

                String xmlData = handleXML.replaceFirst("::url::", url);
                StringRepresentation xmlRepresentation = new StringRepresentation(xmlData, MediaType.TEXT_XML);

                String handle = PREFIX + "/TESTNS" + i;
                handleResource = new ClientResource(RESTFULHS_URL + handle);
                handleResource.setProtocol(Protocol.HTTPS);

                // add the authentication information
                handleResource.setChallengeResponse(authentication);

                representation = handleResource.put(xmlRepresentation);

                System.out.println("[ " + i + " ] Returned Text : " + representation.getText() + "\n");
                
                // close  the underlying connection
                handleResource.release();
                xmlRepresentation.release();
                representation.release();
            }

            System.out.println("Finished creating handles");

            // Sleep for 5 seconds before starting to delete
            Thread.sleep(5000);

            System.out.println("Now let us delete them");

            // now delete them
            for (int i = min; i <= max; i++) {
                try {
                    String handle = PREFIX + "/TESTNS" + i;

                    handleResource = new ClientResource(RESTFULHS_URL + handle);

                    // add the authentication information
                    handleResource.setChallengeResponse(authentication);

                    representation = handleResource.delete();

                    System.out.println("[ " + i + " ] Delete Message : " + representation.getText() + "\n");

                    // close  the underlying connection
                    handleResource.release();
                    representation.release();
                } catch (ResourceException e) { // catch any errors so we can continue to delete
                    // catch any errors so we can continue to delete even if
                    // handle doesn't exist.
                    Status status = e.getStatus();
                    System.out.println("Handle Server Response : " + status + "\n");
                }
            }
        } catch (ResourceException re) {
            Status status = re.getStatus();
            System.out.println("Invalid Response : " + status + "\n");
            assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Method to test the performance of the service when using the noid minter
     * to create handles
     */
    //@Test
    public void testNoidHandleScalability() {
        try {
            // initialize array list to store the handles that where create
            ArrayList<String> createdHandles = new ArrayList<String>();
            
            ClientResource handleResource = null;
            Representation representation = null;

            int min = 0; // The minimum number of the handle
            int max = 100; // The maximum number of the handle

            // create ten handles now
            for (int i = min; i <= max; i++) {
                String url = "http://localhost/handel_" + i + "_" + System.currentTimeMillis();

                String xmlData = handleXML.replaceFirst("::url::", url);
                StringRepresentation xmlRepresentation = new StringRepresentation(xmlData, MediaType.TEXT_XML);

                String handle = PREFIX;  // used when creating handle from noid
                handleResource = new ClientResource(RESTFULHS_URL + handle);

                // add the authentication information
                handleResource.setChallengeResponse(authentication);

                representation = handleResource.post(xmlRepresentation);
                
                String returnedXml = representation.getText();

                System.out.println("[ " + i + " ] Returned Text : " + returnedXml + "\n");

                // add the handle to the arraylist so they can be deleted later
                createdHandles.add(getHandleFromXML(returnedXml));

                // close  the underlying connection
                handleResource.release();
                xmlRepresentation.release();
                representation.release();
            }

            System.out.println("Finished creating NOID handles");

            // Sleep for 5 seconds before starting to delete
            Thread.sleep(5000);

            System.out.println("Now let us delete them");

            // now delete them
            for (int i = 0; i < createdHandles.size(); i++) {
                try {
                    String handle = createdHandles.get(i);
                    handleResource = new ClientResource(RESTFULHS_URL + handle);

                    // add the authentication information
                    handleResource.setChallengeResponse(authentication);

                    representation = handleResource.delete();

                    System.out.println("[ " + i + " ] Delete Message : " + representation.getText() + "\n");

                    // close  the underlying connection
                    handleResource.release();
                    representation.release();
                } catch (ResourceException e) { // catch any errors so we can continue to delete
                    // catch any errors so we can continue to delete even if
                    // handle doesn't exist.
                    Status status = e.getStatus();
                    System.out.println("Handle Server Response : " + status + "\n");
                }
            }
        } catch (ResourceException re) {
            Status status = re.getStatus();
            System.out.println("Invalid Response : " + status + "\n");
            assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Method to extract out the created handle from the return xml
     *
     * @param returnedXML The xml returned from the handle service
     * @return the handle extract from the returned xml
     */
    private String getHandleFromXML(String returnedXml) throws Exception {
        StringRepresentation xmlRepresentation = new StringRepresentation(returnedXml, MediaType.TEXT_XML);
        DomRepresentation representation = new DomRepresentation(xmlRepresentation);
        
        Document doc = representation.getDocument();

        // get the root element then the url this handle should be bound to
        Element root = doc.getDocumentElement();
        Element urlElement = (Element) root.getElementsByTagName("hs:location").item(0);

        // get the full handle url so that we can extract the
        String[] handleURLParts = urlElement.getTextContent().split("net/");
        
        return handleURLParts[1];
    }
}