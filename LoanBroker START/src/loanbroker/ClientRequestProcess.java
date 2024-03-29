package loanbroker;

import bank.BankQuoteReply;
import bank.BankQuoteRequest;
import client.ClientReply;
import client.ClientRequest;
import creditbureau.CreditReply;
import creditbureau.CreditRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import messaging.requestreply.IReplyListener;

/**
 * This class is responsible to processing a single ClientRequest.
 * @author Maja Pesic
 */
abstract class ClientRequestProcess {

    private ClientRequest clientRequest = null;
    private CreditRequest creditRequest = null;
    private CreditReply creditReply = null;
    private BankQuoteRequest bankQuoteRequest = null;
    private BankQuoteReply bankQuoteReply = null;
    private ClientReply clientReply = null;
    private CreditGateway creditGateway;
    private ClientGateway clientGateway;
    private BankGateway bankGateway;
    private IReplyListener<CreditRequest, CreditReply> creditReplyListener;
    private IReplyListener<BankQuoteRequest, BankQuoteReply> bankReplyListener;

    /**
     * initializes the clientRequest and 3 gateways, and send the CreditRequest
     * @param clientRequest
     * @param creditGateway
     * @param clientGateway
     * @param bankGatewayNew
     */
    public ClientRequestProcess(ClientRequest clientRequest, CreditGateway creditGateway, ClientGateway clientGateway, BankGateway bankGateway) {
        super();
        this.clientRequest = clientRequest;
        this.creditGateway = creditGateway;
        this.clientGateway = clientGateway;
        this.bankGateway = bankGateway;
        this.creditReplyListener = new IReplyListener<CreditRequest, CreditReply>() {

            public void onReply(CreditRequest request, CreditReply reply) {
                try {
                    onCreditReply(reply);
                } catch (JMSException ex) {
                    Logger.getLogger(ClientRequestProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        this.bankReplyListener = new IReplyListener<BankQuoteRequest, BankQuoteReply>() {

            public void onReply(BankQuoteRequest request, BankQuoteReply reply) {
                try {
                    onBankQuoteReply(reply);
                } catch (JMSException ex) {
                    Logger.getLogger(ClientRequestProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        requestCreditHistory();
    }

    public ClientRequest getClientRequest() {
        return clientRequest;
    }

    /**
     * Sends the CreditRequest
     * @todo Implement the following
     * 1. create the creditRequest from the clientRequest (use method createCreditRequest)
     * 2. send the creditRequest and register the method onCreditReply as the listener for the reply
     */
    private void requestCreditHistory() {
        try {
            System.out.println("Loanbroaker sent request to Creditbureau");
            CreditRequest credit = createCreditRequest(clientRequest);
            creditGateway.getCreditHistory(credit, creditReplyListener);
        } catch (Exception ex) {
            Logger.getLogger(ClientRequestProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Receives the creditReply
     * @todo Implement the following
     * 1. call method notifyReceivedCreditReply to add the CreditReply to the GUI
     * 2. create the bankRequest from the creditRequest (use method createBankRequest)
     * 3. send the bankRequest and register the method onBankQuoteReply as the listener for the reply
     */
    public void onCreditReply(CreditReply reply) throws JMSException {
        System.out.println("Loanbroaker received reply from Creditbureau");
        creditReply = reply;
        notifyReceivedCreditReply(clientRequest, reply);
        BankQuoteRequest bankRequest = createBankRequest(clientRequest, creditReply);
        bankGateway.sendRequest(bankRequest, bankReplyListener);
        //onBankQuoteReply(bankQuoteReply);
    }

    abstract void notifyReceivedCreditReply(ClientRequest clientRequest, CreditReply reply);

    /**
     * Receives the bankQuoteReply
     * @todo Implement the following
     * 1. call method notifyReceivedBankReply to add the BankQuoteReply to the GUI
     * 2. create the clientReply from the bankQuoteReply (use method createClientReply)
     * 3. send the clientReply and notify the LoanBroker that you have sent the clientReply
     * 4. call method notifySentClientReply to notify the LoanBroker that this process has finished
     */
    public void onBankQuoteReply(BankQuoteReply reply) throws JMSException {
        System.out.println("Loanbroaker received reply from Bank");
        notifyReceivedBankReply(clientRequest, reply);
        ClientReply cReply = createClientReply(reply);
        clientGateway.offerLoan(clientRequest, cReply);
        notifySentClientReply(this);
    }

    abstract void notifyReceivedBankReply(ClientRequest clientRequest, BankQuoteReply reply);

    abstract void notifySentClientReply(ClientRequestProcess process);

    /**
     * Generates a credit request based on the given client request.
     * @param clientRequest
     * @return
     */
    private CreditRequest createCreditRequest(ClientRequest clientRequest) {
        return new CreditRequest(clientRequest.getSSN());
    }

    /**
     * Generates a bank quote reguest based on the given client request and credit reply.
     * @param creditReply
     * @return
     */
    private BankQuoteRequest createBankRequest(ClientRequest clientRequest, CreditReply creditReply) {
        int ssn = creditReply.getSSN();
        int score = creditReply.getCreditScore();
        int history = creditReply.getHistory();
        int amount = clientRequest.getAmount();
        int time = clientRequest.getTime();
        return new BankQuoteRequest(ssn, score, history, amount, time);
    }

    /**
     * Generates a client reply based on the given bank quote reply.
     * @param creditReply
     * @return
     */
    private ClientReply createClientReply(BankQuoteReply reply) {
        return new ClientReply(reply.getInterest(), reply.getQuoteId());
    }
}
