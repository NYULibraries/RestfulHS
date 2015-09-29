/*
 * This class holds utility method which are used by the RestfulHS program
 */
package org.nyu.dlib.restfulhs;

import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import net.handle.hdllib.Util;

/**
 *
 * @author Nathan Stevens
 * @date May 24, 2010;
 */
public class RestfulHSUtils {

    /**
     * Method to check to see if the private key file and passphrase are
     * available for authentication.
     *
     * @param prefix The handle prefix
     * @return boolean saying whether the authentication information is available
     */
    public static boolean hasAuthInfo(String prefix) {
        // now read in the privatekey to access the Handles. If we can't read in the
        // private key then we should alert the client accessing this resource
        String authentication = RestfulHS.properties.getProperty(prefix + ".authentication", "SECKEY");
        String keyFileName = RestfulHS.properties.getProperty(prefix + ".keyfile");
        String passphrase = RestfulHS.properties.getProperty(prefix + ".passphrase");

        // If we doing SECKEY authentication then just check to make
        // sure passphrase is present. If doing PUBKEY authentication then
        // check passphrase and key file are present
        if (authentication.equals("SECKEY") && passphrase != null) {
            if (passphrase != null) {
                return true;
            }
        } else if (authentication.equals("PUBKEY") && keyFileName != null
                && passphrase != null) {

            // now check to see if that file actually exists
            File keyFile = new File(keyFileName);
            if (keyFile.exists()) {
                return true;
            }
        }

        // return false since could not find the private key file
        return false;
    }

    /**
     * Method to check to see if the authentication is SECKEY
     *
     * @param prefix The handle prefix
     * @return true if sec key authentication is used, false otherwise
     */
    public static boolean isSecKeyAuthentication(String prefix) {
        String authentication = RestfulHS.properties.getProperty(prefix + ".authentication");
        if (authentication != null && authentication.equals("SECKEY")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method to check to see if the authentication is PUBKEY
     *
     * @param prefix The handle prefix
     * @return true if pub key authentication is used, false otherwise
     */
    public static boolean isPubKeyAuthentication(String prefix) {
        String authentication = RestfulHS.properties.getProperty(prefix + ".authentication");
        if (authentication != null && authentication.equals("PUBKEY")) {
            return true;
        } else {
            return false;
        }
    }

    public static PrivateKey getPrivateKey(String prefix) throws Exception {
        String keyFileName = RestfulHS.properties.getProperty(prefix + ".keyfile");
        String passphrase = RestfulHS.properties.getProperty(prefix + ".passphrase");

        // First we need to read the private key in from disk
        byte[] key = null;

        File keyfile = new File(keyFileName);
        FileInputStream fs = new FileInputStream(keyfile);
        key = new byte[(int) keyfile.length()];
        int n = 0;
        while (n < key.length) {
            key[n++] = (byte) fs.read();
        }
        fs.read(key);

        // Check to see if the private key is encrypted.  If so, read in the
        // user's passphrase and decrypt.  Finally, convert the byte[]
        // representation of the private key into a PrivateKey object.
        PrivateKey privkey = null;
        byte secKey[] = null;

        if (Util.requiresSecretKey(key)) {
            secKey = passphrase.getBytes("UTF8");
        }

        key = Util.decrypt(key, secKey);
        privkey = Util.getPrivateKeyFromBytes(key, 0);

        return privkey;
    }

    /**
     * Method to return the passphase as a byte array
     *
     * @param prefix The handle prefix
     * @return a byte array containing the secret key
     * @throws Exception
     */
    public static byte[] getSecretKey(String prefix) throws Exception {
        String passphrase = RestfulHS.properties.getProperty(prefix + ".passphrase");
        byte[] secKey = passphrase.getBytes("UTF8");

        return secKey;
    }
}
