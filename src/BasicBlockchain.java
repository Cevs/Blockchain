import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

public class BasicBlockchain {

    private static ArrayList<Block> blockchain = new ArrayList<Block>();
    //list of all unspent transactions.
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();
    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA;
    public static Wallet walletB;
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //Setup Bouncey castel as a Security Provider

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //Create wallets
        walletA = new Wallet();
        walletB = new Wallet();
        Wallet coinbase = new Wallet();
        //Create genesis transaction, which sends 100 coins to walletA
        genesisTransaction = new Transaction(coinbase.getPublicKey(), walletA.getPublicKey(), 100f, null);
        genesisTransaction.generateSignature(coinbase.getPrivateKey());
        genesisTransaction.transactionId = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        //testing
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (60) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 40f));
        block1.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 20f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.getPublicKey(), 1000f));

        addBlock(block2);

        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (30) to WalletA...");
        block3.addTransaction(walletB.sendFunds( walletA.getPublicKey(), 30f));
        addBlock(block3);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block4 = new Block(block3.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block4.addTransaction(walletB.sendFunds( walletA.getPublicKey(), 20f));
        addBlock(block4);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());
        isChainValid();

    }

    /**
     * Check if blockchain is valid
     * @return
     */
    public static Boolean isChainValid(){
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        //a temporary working list of unspent transactions at a given block state.
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>();
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes;
        for(int position = 1; position<blockchain.size(); position++){
            currentBlock = blockchain.get(position);
            previousBlock = blockchain.get(position-1);
            //Compare written hash with calculated
            if(!currentBlock.hash.equals(currentBlock.calculateHash())){
                System.out.println("Current hashes are not equal");
                return false;
            }
            //Compare previous hash and written previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash)){
                System.out.println("Previous hashes are not equal");
                return false;
            }
            //Check if hash is solved
            if(!currentBlock.hash.substring(0, difficulty).equals(hashTarget)){
                System.out.println("This block hasn't been mined");
                return false;
            }

            //loop through blockchains transactions
            TransactionOutput tempOutput;
            for(Transaction currentTransaction: currentBlock.transactions){
                if(!currentTransaction.verifySignature()){
                    System.out.println("Signature on Transaction("+currentTransaction.transactionId+") not valid!");
                    return false;
                }

                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()){
                    System.out.println("Inputs are note equal to outputs on Transaction(" + currentTransaction.transactionId + ")");
                    return false;
                }

                for(TransactionInput input : currentTransaction.inputs){
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null){
                        System.out.println("Referenced input on Transaction ("+currentTransaction.transactionId+") is missing.");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value){
                        System.out.println("Referenced input transaction ("+currentTransaction.transactionId +") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs){
                    tempUTXOs.put(output.id, output);
                }

                if(currentTransaction.outputs.get(0).recipient != currentTransaction.recipient){
                    System.out.println("Transaction (" + currentTransaction.transactionId + ") output recipient is not who it should be.");
                    return false;
                }

                if(currentTransaction.outputs.get(1).recipient != currentTransaction.sender){
                    System.out.println("Transaction (" +  currentTransaction.transactionId + ") output 'change' is not sender.");
                    return false;
                }
            }
        }
        System.out.println("\nBlockchain is valid");
        return true;
    }

    /**
     * Add new block in chain
     * @param newBlock
     */
    public static void addBlock(Block newBlock){
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
