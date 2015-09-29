/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import static org.junit.Assert.*;

/**
 *
 * @author nathan
 */
public class TestHandleResource {

    private static String RESTFULHS_URL = "http://localhost:8082/id/handle/";
    private static String PREFIX = "10676"; // The prefix of the development handle server
    private static String SUCCESS = "HTTP/1.1 200";
    private String handleXML = "";

    public TestHandleResource() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    /**
     * Test to resolve a good handle and bad handle.
     */
    @Test
    public void testGetValidHandle() {
        try {
            String goodHandle = "10.1130/B25510.1";

            // Define our Restlet client resources.
            ClientResource handleResource = new ClientResource(RESTFULHS_URL + goodHandle);
            Response response = handleResource.getResponse();
            Representation representation = handleResource.get();

            System.out.println("Success Response : " + response.toString());
            System.out.println("Returned Text : " + representation.getText() + "\n");

            boolean resolved = response.getStatus().equals(Status.SUCCESS_OK);
            assertTrue(resolved);
        } catch (Exception e) { // exception so test failed for some reason
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Method to test response to trying to get invalid handle
     */
    @Test
    public void testGetInvalidHandle() {
        try {
            String invalidHandle = "10.1130/bad565";

            ClientResource handleResource = new ClientResource(RESTFULHS_URL + invalidHandle);
            Representation representation = handleResource.get();
        } catch (ResourceException re) {
            Status status = re.getStatus();
            String description = status.getDescription();
            int code = status.getCode();

            System.out.println("Invalid Response : " + status + "\n");

            assertEquals(404, code);
        }
    }

    /**
     * Method to test creation of handles
     */
    @Test
    public void testCreateOrUpdateHandle() {
        try {

            ClientResource handleResource = null;
            Representation representation = null;

            // create ten handles now
            for (int i = 1; i <= 10; i++) {
                String url = "http://localhost/handel_" + i + "_" + System.currentTimeMillis();

                String xmlData = handleXML.replaceFirst("::url::", url);
                StringRepresentation xmlRepresentation = new StringRepresentation(xmlData, MediaType.TEXT_XML);

                String handle = PREFIX + "/TESTNS" + i;
                handleResource = new ClientResource(RESTFULHS_URL + handle);
                representation = handleResource.put(xmlRepresentation);

                System.out.println("[ " + i + " ] Returned Text : " + representation.getText() + "\n");

                // close the connection now
                handleResource.release();
                xmlRepresentation.release();
                representation.release();
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
     * Method to test the creation of handles by using the NOId minter service
     * to first get a noid then use that to create the handle
     */
    @Test
    public void createNoidHandle() {
        try {

            ClientResource handleResource = null;
            Representation representation = null;

            // create ten handles now
            for (int i = 1; i <= 2; i++) {
                String url = "http://localhost/handel_" + i + "_" + System.currentTimeMillis();

                String xmlData = handleXML.replaceFirst("::url::", url);
                StringRepresentation xmlRepresentation = new StringRepresentation(xmlData, MediaType.TEXT_XML);

                String handle = PREFIX;
                handleResource = new ClientResource(RESTFULHS_URL + handle);
                representation = handleResource.post(xmlRepresentation);
                
                System.out.println("[ " + i + " ] Returned Text : " + representation.getText() + "\n");

                // close the connection now
                handleResource.release();
                xmlRepresentation.release();
                representation.release();
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
     * Method to test deletion of handles. We will create a bunch of handles and
     * delete them all.
     *
     */
    @Test
    public void testDeleteHandle() {
        try {

            ClientResource handleResource = null;
            Representation representation = null;

            int min = 100; // The minimum number of the handle
            int max = 110; // The maximum number of the handle

            // create ten handles now
            for (int i = min; i <= max; i++) {
                String url = "http://localhost/handel_" + i + "_" + System.currentTimeMillis();

                String xmlData = handleXML.replaceFirst("::url::", url);
                StringRepresentation xmlRepresentation = new StringRepresentation(xmlData, MediaType.TEXT_XML);

                String handle = PREFIX + "/TESTNS" + i;
                handleResource = new ClientResource(RESTFULHS_URL + handle);
                representation = handleResource.put(xmlRepresentation);

                System.out.println("[ " + i + " ] Returned Text : " + representation.getText() + "\n");

                // close the connection now
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
                    representation = handleResource.delete();

                    System.out.println("[ " + i + " ] Delete Message : " + representation.getText() + "\n");

                    // close the connection now
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
}
