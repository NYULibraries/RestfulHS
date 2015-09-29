
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import static org.junit.Assert.*;

/**
 * A test for testing the operation of audit service
 *
 * @author Nathan Stevens
 */
public class TestAuditHandleResource {

    //private final String RESTFULHS_URL = "http://localhost:8082/audit/handle/";
    private final String RESTFULHS_URL = "http://dl-rstardev.home.nyu.edu/audit/handle/";

    private final String BASE_AUDIT_URL = RESTFULHS_URL + "10676";
    //private final String BASE_AUDIT_URL = RESTFULHS_URL + "2333.1";

    // the userid and password to access this resource
    private final String USERID = "dlts-rhs";
    private final String PASSWORD = "pl4typu5";
    private ChallengeResponse authentication;

    public TestAuditHandleResource() {
        // create the authentication object
        authentication = new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERID, PASSWORD);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Method to get the list of handle and run an audit on them
     * TODO 12/20/2010 Must add support for basic authentication since
     * this will only work when the server is running on local host
     */
    @Test
    public void testAuditOfHandles() {
        try {
            URL listHandleUrl = new URL(BASE_AUDIT_URL);

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    listHandleUrl.openStream()));

            String inputLine;

            // get the list of handle
            int count = 1;
            URL auditUrl = null;
            BufferedReader auditReader = null;
            String auditInfo = null;

            // String builders which stores good and broken handles
            StringBuilder goodHandles = new StringBuilder();
            StringBuilder badHandles = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                System.out.println("["+ count + "] Auditing: "  + inputLine);

                try {
                    // now see if the handle is actually valid
                    auditUrl = new URL(RESTFULHS_URL + inputLine);
                    auditReader = new BufferedReader(new InputStreamReader(auditUrl.openStream()));

                    auditInfo = auditReader.readLine();
                    System.out.println(auditInfo + "\n");

                    if(auditInfo.contains("BROKEN")) {
                        badHandles.append(auditInfo).append("\n");
                    } else if(auditInfo.contains("OK")) {
                        goodHandles.append(auditInfo).append("\n");
                    }

                    auditReader.close();
                } catch(Exception e) {
                    System.out.println("Server Error " + e.getMessage());
                }
                
                // increment the count now
                count++;
            }

            in.close();

            // write audit info to a file
            File file = new File("/Users/nathan/handle_audit.csv");
            Writer output = new BufferedWriter(new FileWriter(file));
            try {
                //FileWriter always assumes default encoding is OK!
                output.write("Handle \t Status \t URL \t Timestamp \t Description\n");
                output.write(goodHandles.toString());
                output.write(badHandles.toString());
            }
            finally {
                output.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}
