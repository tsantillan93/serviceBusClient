package com.sams.cpu;
import java.io.StringReader;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;


public class Client implements MessageListener {
  static String uri = "https://management.core.windows.net/";
  static String subscriptionId = "2595a959-2760-494f-9a85-112c76c7f112";
  static String keyStoreLocation = "c:\\certificates\\AzureJavaDemo.jks";
  static String keyStorePassword = "my-cert-password";
 
  public String ConnectionString = "Endpoint=sb://sbx-omnichannel-servicebus.servicebus.windows.net/;SharedAccessKeyName=cpu-dashboard-listener;SharedAccessKey=NV8FH0FVY/DcZ1GDdwvpdp9LOvGisdFaFElbi/AX7FI=;EntityPath=cpu-neworder-details";
  public String TopicName = "cpu-neworder-details";
  static final String[] Subscriptions = {"cpu-getpayload-ethanconner"};   
  
  private static boolean runReceiver = true;
  private Connection connection;
  private Session receiveSession;
  private MessageConsumer receiver;
  
  public static void main(String[] args) throws Exception {
//       new Client().receiveAllMessages();
      try {
          Client simpleReceiver = new Client();
          System.out.println("Press [enter] to send a message. Type 'exit' + [enter] to quit.");
          BufferedReader commandLine = new java.io.BufferedReader(new InputStreamReader(System.in));
    
          while (true) {
              String s = commandLine.readLine();
              if (s.equalsIgnoreCase("exit")) {
                  simpleReceiver.close();
                  System.exit(0);
              } else {
                  //simpleSenderReceiver.sendMessage();
              }
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
       

 
    public Client() throws Exception {
        // Configure JNDI environment
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, 
                   "org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory");
        env.put(Context.PROVIDER_URL, "servicebus.properties");
        Context context = new InitialContext(env);

        // Look up ConnectionFactory and Queue
        ConnectionFactory cf = (ConnectionFactory) context.lookup("TESTBUS");
        Destination queue = (Destination) context.lookup("TOPIC");

        // Create Connection
        connection = cf.createConnection();

        if (runReceiver) {
            // Create receiver-side Session, MessageConsumer,and MessageListener
            receiveSession = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            receiver = receiveSession.createConsumer(queue);
            receiver.setMessageListener(this);
            connection.start();
        }
    }
    
    public void onMessage(Message message) {
        try {
            System.out.println("Received message with JMSMessageID = " + message.getJMSMessageID());
            message.acknowledge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void close() throws JMSException {
        connection.close();
    }
    
    public void receiveAllMessages() throws Exception {     
        System.out.printf("\nStart Receiving Messages.\n");

        CompletableFuture.allOf(
                receiveAllMessageFromSubscription(Subscriptions[0]) 
                ).join();
 }
    public CompletableFuture<Void> receiveAllMessageFromSubscription(String subscription) throws Exception {

        int receivedMessages = 0;

        // Create subscription client.
        IMessageReceiver subscriptionClient = ClientFactory.createMessageReceiverFromConnectionStringBuilder
                    (new ConnectionStringBuilder(ConnectionString, TopicName+"/subscriptions/"+ subscription), 
                                  ReceiveMode.PEEKLOCK);

        // Create a receiver from the subscription client and receive all messages.
        System.out.printf("\nReceiving messages from subscription %s.\n\n", subscription);

        while (true)
        {
            // This will make the connection wait for N seconds if new messages are available. 
            // If no additional messages come we close the connection. This can also be used to realize long polling.
            // In case of long polling you would obviously set it more to e.g. 60 seconds.
           IMessage receivedMessage = subscriptionClient.receive(Duration.ofSeconds(1));
            if (receivedMessage != null)
            {
//                if ( receivedMessage.getProperties() != null ) {                                                                                
//                    System.out.printf("Date Created=%s\n", receivedMessage.getProperties().get("CreateTSUTC"));                                                                                          
//
//                    // Show the label modified by the rule action
//                    if(receivedMessage.getLabel() != null)
//                        System.out.printf("Label=%s\n", receivedMessage.getLabel());   
//                }

                byte[] body = receivedMessage.getBody();

                
                
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
                DocumentBuilder builder;  
                try {  
                    builder = factory.newDocumentBuilder();  
                    Document document = builder.parse(new InputSource(new StringReader(new String(body)))); 
                    String countryCode, ebuNumber, orderNumber, scheduledTime, fulfillmentDate;
                    
                    Element headerInfo = (Element) document.getElementsByTagName("NS1:HeaderInfo").item(0);
                    countryCode = headerInfo.getAttribute("CountryCode");
                    ebuNumber = headerInfo.getAttribute("StoreNo");
                    orderNumber = ((Element)document.getElementsByTagName("NS1:OrderNumber").item(0)).getTextContent();
                    scheduledTime = ((Element)document.getElementsByTagName("NS1:ScheduleTimeSlot").item(0)).getTextContent();
                    fulfillmentDate = ((Element)document.getElementsByTagName("NS1:FulfillmentDate").item(0)).getTextContent();
                    
                    
                    System.out.println("countryCode : "+countryCode+
                                          "\nebuNbr : "+ebuNumber+
                                        "\norderNbr : "+orderNumber+
                               "\nscheduledTimeSlot : "+scheduledTime+
                                 "\nfulfillmentDate : "+fulfillmentDate+"\n" );
                } catch (Exception e) {  
                    e.printStackTrace();  
                } 
             
                subscriptionClient.complete(receivedMessage.getLockToken());
                receivedMessages++;
            }
            else
            {
                // No more messages to receive.
                subscriptionClient.close();
                break;
            }
        }
        System.out.printf("\nReceived %s messages from subscription %s.\n", receivedMessages, subscription);

        return new CompletableFuture().completedFuture(null);
}
}

