import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
    private PrivateKey privateKey;
    private PublicKey publicKey;

    //Local list of  UTXOs owned by this wallet.
    public HashMap<String, TransactionOutput> UTXOsWallet = new HashMap<String, TransactionOutput>();

    public Wallet() {
        generateKeyPair();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Generate public and private key
     */
    public void generateKeyPair(){
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Return balance and store the UTXO's owned by this wallet
     * @return
     */
    public float getBalance(){
        float total = 0;
        for(Map.Entry<String, TransactionOutput> item: BasicBlockchain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            //Check if output belongs to this wallet ( if coins belong to this wallet)
            if(UTXO.isMine(publicKey)){
                //Add it to list of unspent transactions;
                UTXOsWallet.put(UTXO.id, UTXO);
                total += UTXO.value;
            }
        }
        return total;
    }

    /**
     * Send funds/coins sto recipient
     * @param recipient
     * @param sendValue
     * @return
     */
    public Transaction sendFunds(PublicKey recipient, float sendValue){
        if(getBalance()<sendValue){
            System.out.println("Insufficient funds. Transaction Discarded");
            return null;
        }

        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        float total = 0;
        for(Map.Entry<String, TransactionOutput> item: UTXOsWallet.entrySet()){ //get all unspent transactions
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total > sendValue) break;
        }

        Transaction newTransaction = new Transaction(publicKey, recipient, sendValue, inputs);
        newTransaction.generateSignature(privateKey);

        //Remove transaction as spent
        for(TransactionInput i: inputs){
            UTXOsWallet.remove(i.transactionOutputId);
        }

        return newTransaction;
    }
}
