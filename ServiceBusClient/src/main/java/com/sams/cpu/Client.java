package com.sams.cpu;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

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
  
//  public static void displayMessages1 () {
//       MessagingFactory factory = MessagingFactory.Create(uri, tokenProvider);
//       MessageReceiver receiver = factory.CreateMessageReceiver($"{subscription}");// topicname/subscriptions/nameoffilter
//       BrokeredMessage receivedMessage = receiver.Receive();
//           Stream msgStream = receivedMessage.GetBody<Stream>();
//           StreamReader sr = new StreamReader(msgStream);
//           string topicBodyStream = sr.ReadToEnd();
//           var topicBody = JsonConvert.DeserializeObject<dynamic>(topicBodyStream);MessagingFactory factory = MessagingFactory.Create(uri, tokenProvider);
//           MessageReceiver receiver = factory.CreateMessageReceiver($"{subscription}");// topicname/subscriptions/nameoffilter
//           BrokeredMessage receivedMessage = receiver.Receive();
//               var msgStream = receivedMessage.GetBody<Stream>();
//              StreamReader sr = new StreamReader(msgStream);
//               string topicBodyStream = sr.ReadToEnd();
//               var topicBody = JsonConvert.DeserializeObject<dynamic>(topicBodyStream);
//  }
  public String ConnectionString = "Endpoint=sb://sbx-omnichannel-servicebus.servicebus.windows.net/;SharedAccessKeyName=cpu-dashboard-listener;SharedAccessKey=NV8FH0FVY/DcZ1GDdwvpdp9LOvGisdFaFElbi/AX7FI=;EntityPath=cpu-neworder-details";
  public String TopicName = "cpu-neworder-details";
  static final String[] Subscriptions = {"getpayload"};
  static final String[] Store = {"Store1","Store2","Store3","Store4","Store5","Store6","Store7","Store8","Store9","Store10"};
  static final String SysField = "sys.To";
  static final String CustomField = "StoreId";    
  int NrOfMessagesPerStore = 1; // Send at least 1.
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
                       if ( receivedMessage.getProperties() != null ) {                                                                                
                           System.out.printf("StoreId=%s\n", receivedMessage.getProperties().get("StoreId"));                                                                                          

                           // Show the label modified by the rule action
                           if(receivedMessage.getLabel() != null)
                               System.out.printf("Label=%s\n", receivedMessage.getLabel());   
                       }

                       byte[] body = receivedMessage.getBody();
                       //Item theItem = GSON.fromJson(new String(body, UTF_8), Item.class);
                       System.out.println(body);
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

