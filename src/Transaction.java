import java.security.*;
import java.util.ArrayList;

public class Transaction {
    public String transactionId; //Hash of transaction
    public PublicKey sender; //Sender address/public key
    public PublicKey recipient; //Recipient address/public key
    public float value;
    public byte[] signature; //Secure transaction so nobody can spend funds from someone else wallet

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; //count of how many transactions have been generated.

    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    private String calculateHash(){
        sequence++; //increase the sequence to avoid identical transactions having the same hash
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender)
                        + StringUtil.getStringFromKey(recipient)
                        + Float.toString(value)
                        + sequence
        );
    }

    /**
     * Sign data of transaction with sender private key
     * @param privateKey
     */
    public void generateSignature(PrivateKey privateKey){
        String data = StringUtil.getStringFromKey(sender) + value + StringUtil.getStringFromKey(recipient);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    /**
     * Verify that signed data hasn't been tampered with
     * @return
     */
    public boolean verifySignature(){
        String data = StringUtil.getStringFromKey(sender) + value +StringUtil.getStringFromKey(recipient);
        return StringUtil.verifyECDSASig(sender,data,signature);
    }

    /**
     * Return true if new transaction can be created
     * @return
     */
    public boolean processTransaction(){
        if(!verifySignature()){
            System.out.println("Transaction signature failed to verify");
            return false;
        }

        //Gather transaction inputs
        for(TransactionInput i: inputs){
            //this should be done in better way. UTXOs should not exist but we should check
            //the whole blockchain instead !!!!
            i.UTXO = BasicBlockchain.UTXOs.get(i.transactionOutputId);
        }

        //check if transaction is valid
        if(getInputsValue() < BasicBlockchain.minimumTransaction){
            System.out.println("Transaction inputs to small: " + getInputsValue());
            return false;
        }

        //Generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionId)); //send value to recipient
        outputs.add(new TransactionOutput(this.sender,leftOver, transactionId)); //send the left over 'change' back to sender

        //Add outputs to Unspent list
        for(TransactionOutput o : outputs){
            BasicBlockchain.UTXOs.put(o.id, o);
        }

        //Remove transaction inputs from UTXOs list as spent:
        for(TransactionInput i: inputs){
            if(i.UTXO == null) continue; //if transaction can't be found skip it
            BasicBlockchain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    /**
     * Returns sum of input values
     * @return
     */
    public float getInputsValue(){
        float total = 0;
        for(TransactionInput i : inputs){
            if(i.UTXO== null) continue;//if transaction can't be found skip it
            total += i.UTXO.value;
        }

        return total;
    }

    /**
     * Return sum of output values
     * @return
     */
    public float getOutputsValue(){
        float total = 0;
        for(TransactionOutput o : outputs){
            total += o.value;
        }
        return  total;
    }
}
