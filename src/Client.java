import com.rabbitmq.client.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by Stanley on 10/10/2015.
 */
public class Client {
    private static boolean isExit = false;
    private static String CurrentUser = null;
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println("Please login by typing '/NICK' or '/NICK[space][nickname]'");
        //Login
        while(CurrentUser==null){
            Scanner client_input = new Scanner(System.in);
            String input = client_input.nextLine();
            if(!input.contains(" ") && input.substring(0,5).equals("/NICK")){
                CurrentUser = new StringGenerator(8).nextString();
                channel.queueDeclare(CurrentUser, false, false, false, null);
                channel.exchangeDeclare(CurrentUser+"-broadcast", "fanout", true);
                System.out.println("You are logged in as " + CurrentUser);
            }
            else if(input.contains(" ") && input.substring(0,5).equals("/NICK")){
                CurrentUser = input.substring(input.indexOf(' ') + 1, input.length());
                channel.queueDeclare(CurrentUser, false, false, false, null);
                channel.exchangeDeclare(CurrentUser+"-broadcast", "fanout", true);
                System.out.println("You are logged in as " + CurrentUser);
            }
            else{
                System.out.println("Please login by typing '/NICK' or '/NICK[space][nickname]'");
            }
        }
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                String format = "["+ envelope.getExchange() +"] " + message;
                System.out.println(format);
            }
        };
        channel.basicConsume(CurrentUser, consumer);
        Thread inputHandler = inputHandler(channel,connection);
        inputHandler.start();

    }


    private static Thread inputHandler(Channel channel,Connection connection){
        return new Thread(){
            public void run() {
                try {
                    while(!isExit) {
                        //Terima input
                        String buffer, content;
                        Scanner client_input = new Scanner(System.in);
                        String input = client_input.nextLine();
                        if (!input.isEmpty()) {
                            if (input.contains(" ")) {
                                buffer = input.substring(0, input.indexOf(' '));
                                content = input.substring(input.indexOf(' ') + 1, input.length());
                            } else {
                                buffer = input;
                                content = input;
                            }
                            if (buffer.length() > 0) {
                                switch (buffer) {
                                    case "/NICK":
                                        System.out.println("You are logged in as " + CurrentUser);
                                        break;
                                    case "/JOIN":
                                        if (content != buffer) {
                                            channel.exchangeDeclare(content, "fanout", true);
                                            channel.queueBind(CurrentUser, content, "");
                                            channel.exchangeBind(content, CurrentUser+"-broadcast", "");
                                            System.out.println("You have joined channel " + content);
                                        } else {
                                            System.out.println("Please add channel name");
                                        }
                                        break;
                                    case "/LEAVE":
                                        if (content != buffer) {
                                            channel.queueUnbind(CurrentUser, content, "");
                                            channel.exchangeUnbind(content, CurrentUser, "");
                                            System.out.println("You have leaved channel " + content);
                                        } else {
                                            System.out.println("Please add channel name");
                                        }
                                        break;
                                    case "/EXIT":
                                        channel.queueDelete(CurrentUser);
                                        isExit = true;
                                        connection.close();
                                        break;

                                    default:
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                                        Date resultdate = new Date();
                                        if (buffer.contains("@") && content != buffer) {
                                            String message = "(" + CurrentUser + ") : " + content + " (" + sdf.format(resultdate) + ")";
                                            channel.basicPublish(buffer.substring(1), "", null, message.getBytes());
                                        } else if (input.length() > 0) {

                                            String message = "(" + CurrentUser + ") : " + input + " (" + sdf.format(resultdate) + ")";
                                            channel.basicPublish(CurrentUser+"-broadcast", "", null, message.getBytes());

                                        } else {
                                            System.out.println("channel unknown, please join a channel or recheck your message");
                                        }
                                }
                            }

                        } else {
                            System.out.println("format input wrong");
                        }
                    }
                }
                catch(Exception x){
                    x.printStackTrace();
                }
            }
        };
    }
}
