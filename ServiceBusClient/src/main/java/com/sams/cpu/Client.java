package com.sams.cpu;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;


public class Client implements MessageListener {
  private ConnectionStringBuilder csb = null;
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
        
        csb = new ConnectionStringBuilder("sbx-omnichannel-servicebus", "cpu-neworder-details/Subscriptions/getpayload", "cpu-dashboard-listener",
                "YPAcS9eBEOTsdytE3vy1/8WTDhaiqzCUwoRVtG95vZ4=");

        // Configure JNDI environment
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, 
                   "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        env.put(Context.PROVIDER_URL, "src/main/resources/servicebus.properties");
        Context context = new InitialContext(env);

        // Look up ConnectionFactory and Queue
        ConnectionFactory cf = (ConnectionFactory) context.lookup("TESTBUS");
        Destination queue = (Destination) context.lookup("SUBSCRIPTION");

        // Create Connection
        connection = cf.createConnection(csb.getSasKeyName(), csb.getSasKey());
        
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
            String msgBody = ((TextMessage) message).getText();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
            DocumentBuilder builder;  
                            builder = factory.newDocumentBuilder();  
            Document document = builder.parse(new InputSource(new StringReader(msgBody))); 
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
            message.acknowledge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void close() throws JMSException {
        connection.close();
    }
    
   
}

