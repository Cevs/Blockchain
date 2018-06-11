import java.util.ArrayList;
import java.util.Date;

public class Block {

    public String hash;
    public String previousHash;
    private long timeStamp;
    private int nonce;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>();  //our data will be a simple message.
    public String merkleRoot;

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    /**
     * Calculate hash of a previous and current block.
     * @return
     */
    public String calculateHash(){
        String calculatedHash = StringUtil.applySha256(
                    previousHash
                            + Long.toString(timeStamp)
                            + Integer.toString(nonce)
                            + merkleRoot
        );
        return calculatedHash;
    }

    /**
     * Mine block which will be added in chain
     * @param difficultyLevel
     */
    public void mineBlock(int difficultyLevel){
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = new String(new char[difficultyLevel]).replace('\0', '0');
        while(!hash.substring(0, difficultyLevel).equals(target)){
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block Mined !!! : "+hash);
    }

    /**
     * Add transaction in block
     * @param transaction
     * @return
     */
    public boolean addTransaction(Transaction transaction){
        //process transaction and check if valid, unless block is genesis then ignore
        if(transaction == null) return false;
        if((previousHash != "0")){
            if((transaction.processTransaction() != true)){
                System.out.println("Transaction failed to process. Discarded");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction successfully added to block");
        return true;
    }
}
