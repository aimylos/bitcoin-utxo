package Blockchainj.Blockchain.Server;

import Blockchainj.Blockchain.Main.UserParams;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Client - Simple request client.
 *
 * Does Prototype Protocol requests to the server and retrieves the responses.
 *
 */

public class Client {
    /* Server address */
    private final InetAddress serverAddr;

    /* Server port */
    private final int serverPort;


    /* Constructor */
    public Client(String serverIP, int serverPort) throws UnknownHostException {
        this.serverAddr = InetAddress.getByName(serverIP);
        this.serverPort = serverPort;
    }


    /* perform request */
    private Message doRequest(Message message, boolean doOutput)
            throws IOException, IllegalArgumentException {
        /* Open socket */
        if(doOutput) {
            System.out.println("Connecting...");
        }
        Socket socket = new Socket(serverAddr, serverPort);
        if(doOutput) {
            System.out.println("Connected");
        }

        //noinspection TryFinallyCanBeTryWithResources
        try {
            /* Write message */
            if(doOutput) {
                System.out.println("Sending message...");
            }
            message.serializeToSocket(socket.getOutputStream());
            if(doOutput) {
                System.out.println("Message sent.");
            }

            /* Read response */
            if(doOutput) {
                System.out.println("Waiting for response...");
            }
            //Message response = Message.deserializeFromSocket(socket.getInputStream());
            Message response = Message.deserializeFromSocket(
                    new BufferedInputStream(socket.getInputStream()));
            if(doOutput) {
                System.out.println("Response recieved.");
            }

            return response;
        } finally {
            if(doOutput) {
                System.out.println("Closing connection...");
            }
            socket.close();Option request = new Option("r", "request", true, "Request. Use:\n" +
                MessageGetCustom.requestFormat());
        request.setRequired(true);
        request.setArgs(20);
            if(doOutput) {
                System.out.println("Connection closed.");
            }
        }
    }



    /* Perform a one time request. */
    public static void main(String[] args) {
        String hline = new String(new char[25]).replace("\0", "=");
        PrintStream printStream = System.out;

        /* User options */
        Options options = new Options();
        options.addOption("prg", "progressOutput", false, "Print client progress details.");
        options.addOption("msg", "messageOutput", false, "Print messages.");
        options.addOption("prev", "previewResponse", false, "Print response preview.");
        options.addOption("raw", "rawResponse", false, "Print raw response data bytes.");
        options.addOption("full", "fullResponse", false, "Print full readble response.");
        Option port = new Option("p", "port", true, "Server port. Default is: " +
                UserParams.getInt("SERVER_PORT"));
        Option host = new Option("h", "host", true, "Server ip. Default is: " +
                UserParams.getString("SERVER_IP"));
        Option request = new Option("r", "request", true, "Request. Use:\n" +
                MessageGetCustom.requestFormat());
        request.setRequired(true);
        request.setArgs(20);
        request.setOptionalArg(true);
        options.addOption(port);
        options.addOption(host);
        options.addOption(request);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            printStream.println(e.getMessage());
            formatter.printHelp("Client", options);
            System.exit(1);
            return;
        }

        /* Set parameters */
        boolean doProgressOutput = cmd.hasOption("prg");
        boolean doMessageOutput = cmd.hasOption("msg");
        boolean doPreviewOutput = cmd.hasOption("prev");
        boolean doCompleteRawOutput = cmd.hasOption("raw");
        boolean doCompleteReadableOutput = cmd.hasOption("full");
        if(!(doProgressOutput | doPreviewOutput | doCompleteRawOutput | doCompleteReadableOutput)) {
            doProgressOutput = true;
            doPreviewOutput = true;
            doMessageOutput = true;
        }

        /* Server infos */
        String serverIP;
        int serverPort;
        if(cmd.hasOption("p")) {
            serverPort = Integer.parseInt(cmd.getOptionValue("p"));
        } else {
            serverPort = UserParams.getInt("SERVER_PORT");
        }
        if(cmd.hasOption("h")) {
            serverIP = cmd.getOptionValue("h");
        } else {
            serverIP = UserParams.getString("SERVER_IP");
        }

        /* Request args */
        String[] requestArgs = cmd.getOptionValues("r");
        int requestArgsOffset = 0;

        /* Make new client */
        Client client;
        try {
            client = new Client(serverIP, serverPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        /* Computing request */
        if(doProgressOutput) {
            printStream.println(hline);
            printStream.println("Computing request...");
        }
        MessageGetCustom message;
        try {
            message = MessageGetCustom.getMessageGetCustom(
                    requestArgs, requestArgsOffset);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            //noinspection ImplicitArrayToString
            printStream.println(requestArgs);
            formatter.printHelp("Client", options);
            System.exit(1);
            return;
        }
        if(doProgressOutput) {
            printStream.println("Request computed: ");
        }
        if(doMessageOutput) {
            message.print(printStream, true, false);
        }


        /* Do request and get response */
        if(doProgressOutput) {
            printStream.println("\n" + hline);
        }
        Message response;
        try {
            response = client.doRequest(message, doProgressOutput);
        } catch (IllegalArgumentException | IOException e) {
            throw new RuntimeException("Internal error.", e);
        }


        /* Print result */
        if(doProgressOutput) {
            printStream.println("\n" + hline);
            printStream.println("Response message: ");
        }
        try {
            /* Reject message */
            if (MessageReject.isReject(response)) {
                MessageReject messageReject = new MessageReject(response);
                if(doMessageOutput) {
                    messageReject.print(printStream, true, false);
                }
                throw new RuntimeException("Server responded with error: " +
                            messageReject.toStringReject());
            }
            /* Data message */
            else if(MessageDataCustom.isDataCustom(response)) {
                MessageDataCustom messageDataCustom = new MessageDataCustom(response);
                if(doMessageOutput) {
                    messageDataCustom.print(printStream, true, false, true);
                }
                if(doPreviewOutput) {
                    printStream.println("\n\nResult:\n");
                    messageDataCustom.printPreviewReadableData(printStream);
                }
                else if(doCompleteReadableOutput) {
                    printStream.println("\n\nResult:\n");
                    messageDataCustom.printCompleteReadableData(printStream);
                }
                else if(doCompleteRawOutput) {
                    printStream.println("\n\nResult:\n");
                    messageDataCustom.printRawData(printStream);
                }
            }
            /* Unknown message */
            else {
                if(doMessageOutput) {
                    response.print(printStream, true);
                }
                throw new RuntimeException("Server responded with unknown message: " +
                        message.toString());
            }
        } catch (IllegalArgumentException | IOException e) {
            if(doMessageOutput) {
                response.print(printStream, false);
            }
            throw new RuntimeException(e);

        }
    }
}
