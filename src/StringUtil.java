import java.security.*;
import java.util.ArrayList;
import java.util.Base64;

public class StringUtil {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String CHARSET = "UTF-8";

    /**
     * Apply SHA-256 to string input and return hex in the form of a String.
     * @param input
     * @return
     */
    public static String applySha256(String input){
        try{
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(CHARSET));
            return convertToHexString(hash);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert byte array to the string representation of a hex
     * @param hash
     * @return
     */
    private static String convertToHexString(byte[] hash){
        StringBuffer hexString = new StringBuffer();
        for(int i = 0; i<hash.length; i++){
            String sHex = Integer.toHexString(0xff & hash[i]);
            if(sHex.length() == 1){
                hexString.append(0);
            }
            hexString.append(sHex);
        }
        return hexString.toString();
    }

    /**
     * Convert Key object to string
     * @param key
     * @return
     */
    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Apply ECDSA and return digital signature
     * @param privateKey
     * @param input
     * @return
     */
    public static byte[] applyECDSASig(PrivateKey privateKey, String input){
        byte[] signature = new byte[]{};
        try{
            Signature dsa = Signature.getInstance("ECDSA","BC");
            dsa.initSign(privateKey);
            dsa.update(input.getBytes());
            signature = dsa.sign();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return signature;
    }

    /**
     * Verify signature
     * @param publicKey
     * @param data
     * @param signature
     * @return
     */
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature){
        try{
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Get Merkle root from transaction hashes
     * @param transactions
     * @return
     */
    public static String getMerkleRoot(ArrayList<Transaction> transactions){
        ArrayList<String> hashList = new ArrayList<String>();
        for(Transaction t: transactions){
            hashList.add(t.transactionId);
        }
        String m = merkle(hashList);
        return m;
    }

    /**
     * Recursive computing of Merkle root
     * @param hashList
     * @return
     */
    private static String merkle(ArrayList<String> hashList){

        int hashNumber = hashList.size();
        if(hashNumber == 0){
            return "";
        }
        if (hashNumber == 1){
            return hashList.get(0);
        }

        ArrayList<String> newHashList = new ArrayList<String>();
        for(int i = 0; i<hashNumber-1; i += 2){
            newHashList.add(applySha256(hashList.get(i))+(hashList.get(i+1)));
        }
        //Odd number of transactions
        //Hash last item twice
        int i = newHashList.size();
        if(hashList.size() % 2 == 1) {
            newHashList.add(applySha256(hashList.get(hashList.size() - 1)));
        }

        return merkle(newHashList);
    }
}
