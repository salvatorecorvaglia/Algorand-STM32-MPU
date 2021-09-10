package com.algorand.AtomicTransactionSTM;

import java.io.ByteArrayOutputStream;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;

public class AtomicTransactionSTM {
    public AlgodClient client = null;
    // Connect to a node
    private AlgodClient connectToNetwork(){

        // Initialize an algod client
        final String ALGOD_API_ADDR = "http://192.168.1.72";
        final int ALGOD_PORT = 6001;
        final String ALGOD_API_TOKEN = "62569290418bdd347a5df5fe9bb191a79aa89124fdda23541a4c1dc36bf3d335";

        return new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
    }

    public void waitForConfirmation( String txID ) throws Exception{
        if( client == null ) this.client = connectToNetwork();
        Long lastRound = client.GetStatus().execute().body().lastRound;
        while(true) {
            //Check the pending transaction
            Response<PendingTransactionResponse> pendingInfo = client.PendingTransactionInformation(txID).execute();
            if (pendingInfo.body().confirmedRound != null && pendingInfo.body().confirmedRound > 0) {
                //Got the completed Transaction
                System.out.println("Transaction " + txID + " confirmed in round " + pendingInfo.body().confirmedRound);
                break;
            }
            lastRound++;
            client.WaitForBlock(lastRound).execute();
        }
    }

    public void AtomicTransfer() throws Exception {

        if( client == null ) this.client = connectToNetwork();

        final String account1_mnemonic = "provide stay fetch exist wild night okay adjust upgrade put abandon foil matrix ignore robust hurry mind velvet again inform field east acid abstract play";
        final String account2_mnemonic = "gravity iron fuel ring frown napkin follow cave cushion abandon endless legend okay ivory asthma chaos rain arch love filter gain atom hen absent ramp";
        final String account3_mnemonic = "intact horse original solve clutch subject win surround near stem mimic fresh salon olympic electric nose foil ticket slam absorb pupil also deliver about flight";

        // Recover account A, B, C
        Account accountA  = new Account(account1_mnemonic);
        Account accountB  = new Account(account2_mnemonic);
        Account accountC  = new Account(account3_mnemonic);

        // Get node suggested parameters
        String note1 = "First Atomic Transfer with STM32MP157A-DK1";
        String note2 = "First Atomic Transfer with STM32MP157A-DK1";
        TransactionParametersResponse params = client.TransactionParams().execute().body();        

        // Create the first transaction
        Transaction tx1 = Transaction.PaymentTransactionBuilder()
        .sender(accountA.getAddress())
        .note(note1.getBytes())
        .amount(30000000)
        .receiver(accountC.getAddress())
        .suggestedParams(params)
        .build();

        // Create the second transaction
        Transaction tx2 = Transaction.PaymentTransactionBuilder()
        .sender(accountB.getAddress())
        .note(note2.getBytes())
        .amount(30000000)
        .receiver(accountA.getAddress())
        .suggestedParams(params)
        .build();
        // Group transactions an assign ids
        Digest gid = TxGroup.computeGroupID(tx1, tx2);
        tx1.assignGroupID(gid);
        tx2.assignGroupID(gid);

        // Sign individual transactions
        SignedTransaction signedTx1 = accountA.signTransaction(tx1);
        SignedTransaction signedTx2 = accountB.signTransaction(tx2);

        try {
            // Put both transaction in a byte array
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream( );
            byte[] encodedTxBytes1 = Encoder.encodeToMsgPack(signedTx1);
            byte[] encodedTxBytes2 = Encoder.encodeToMsgPack(signedTx2);
            byteOutputStream.write(encodedTxBytes1);
            byteOutputStream.write(encodedTxBytes2);
            byte[] groupTransactionBytes = byteOutputStream.toByteArray();

            // Send transaction group
            String id = client.RawTransaction().rawtxn(groupTransactionBytes).execute().body().txId;
            System.out.println("Successfully sent tx with ID: " + id);

            // Wait for confirmation
            waitForConfirmation(id);

        } catch (Exception e) {
            System.out.println("Submit Exception: " + e); 
        }
    }
    public static void main(String[] args) throws Exception {
        AtomicTransactionSTM mn = new AtomicTransactionSTM();
        mn.AtomicTransfer();
    }
}