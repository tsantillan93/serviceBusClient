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

public class Client {
  static String uri = "https://management.core.windows.net/";
  static String subscriptionId = "2595a959-2760-494f-9a85-112c76c7f112";
  static String keyStoreLocation = "c:\\certificates\\AzureJavaDemo.jks";
  static String keyStorePassword = "my-cert-password";

public static void main(String[] args) throws Exception {
       new Client().receiveAllMessages();
       
        //    Configuration config = ManagementConfiguration.configure(
//      new URI(uri), 
//      subscriptionId,
//      keyStoreLocation, // the file path to the JKS
//      keyStorePassword, // the password for the JKS
//      KeyStoreType.jks // flags that I'm using a JKS keystore
//    );
//
//    // create a management client to call the API
//    ManagementClient client = ManagementService.create(config);
//
//    // get the list of regions
//    LocationsListResponse response = null;
//     try {
//            response = client.getLocationsOperations().list();
//     } catch (com.microsoft.windowsazure.exception.ServiceException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//     }
//    ArrayList<Location> locations = response.getLocations();
//    // write them out
//    for( int i=0; i<locations.size(); i++){
//      System.out.println((locations.get(i)).getDisplayName());
//    }
  }
  
  public String ConnectionString = "Endpoint=sb://sbx-omnichannel-servicebus.servicebus.windows.net/;SharedAccessKeyName=cpu-dashboard-listener;SharedAccessKey=NV8FH0FVY/DcZ1GDdwvpdp9LOvGisdFaFElbi/AX7FI=;EntityPath=cpu-neworder-details";
  public String TopicName = "cpu-neworder-details";
  static final String[] Subscriptions = {"cpu-getpayload-ethanconner"};    

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
//                       if ( receivedMessage.getProperties() != null ) {                                                                                
//                           System.out.printf("Date Created=%s\n", receivedMessage.getProperties().get("CreateTSUTC"));                                                                                          
//
//                           // Show the label modified by the rule action
//                           if(receivedMessage.getLabel() != null)
//                               System.out.printf("Label=%s\n", receivedMessage.getLabel());   
//                       }

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

