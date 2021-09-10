package com.algorand.FirstTransactionSTM;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;

public class FirstTransactionSTM {

    
    public AlgodClient client = null;
    // Connect to a node
    private AlgodClient connectToNetwork(){

        // Initialize an algod client
        final String ALGOD_API_ADDR = "http://192.168.1.72";
        final int ALGOD_PORT = 6001;
        final String ALGOD_API_TOKEN = "62569290418bdd347a5df5fe9bb191a79aa89124fdda23541a4c1dc36bf3d335";

        return new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
    }
    // Function to wait on a transaction to be confirmed
    public void waitForConfirmation( String txID ) throws Exception{
        if( client == null ) this.client = connectToNetwork();
        Long lastRound = client.GetStatus().execute().body().lastRound;
        while(true) {
            //Check the pending transactions
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

    public void firstTransaction() throws Exception {

        if( client == null ) this.client = connectToNetwork();

        // Import private key mnemonic and address
        final String PASSPHRASE = "provide stay fetch exist wild night okay adjust upgrade put abandon foil matrix ignore robust hurry mind velvet again inform field east acid abstract play";
        com.algorand.algosdk.account.Account myAccount = new Account(PASSPHRASE);
        System.out.println("My Address: " + myAccount.getAddress());

        String myAddress = myAccount.getAddress().toString();

        com.algorand.algosdk.v2.client.model.Account accountInfo = client.AccountInformation(myAccount.getAddress()).execute().body();

        System.out.printf("Account Balance: %d microAlgos%n", accountInfo.amount);

        try {
            // Construct the transaction
            final String RECEIVER = "7ZEEEYMRYL2HIXQBRIBG6TDSJPOAJXL5JONCY7RC5WXVZL7RUDRW4H3WJ4";
            String note = "First STM32MP157A-DK1 Transaction";
            TransactionParametersResponse params = client.TransactionParams().execute().body();
            Transaction txn = Transaction.PaymentTransactionBuilder()
                    .sender(myAddress)
                    .note(note.getBytes())
                    .amount(10000000)
                    .receiver(new Address(RECEIVER))
                    .suggestedParams(params)
                    .build();


            // Sign the transaction
            SignedTransaction signedTxn = myAccount.signTransaction(txn);
            System.out.println("Signed transaction with txid: " + signedTxn.transactionID);

            // Submit the transaction to the network
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);
            String id = client.RawTransaction().rawtxn(encodedTxBytes).execute().body().txId;
            System.out.println("Successfully sent tx with ID: " + id);

            // Wait for transaction confirmation
            waitForConfirmation(id);

            // Read the transaction
            PendingTransactionResponse pTrx = client.PendingTransactionInformation(id).execute().body();
            System.out.println("Transaction information (with notes): " + pTrx.toString());
            System.out.println("Decoded note: " + new String(pTrx.txn.tx.note));


        } catch (Exception e) {
            System.err.println("Exception when calling algod#transactionInformation: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        FirstTransactionSTM t = new FirstTransactionSTM();
        t.firstTransaction();
    }
}
