package com.example.uni_tok;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeMap;

public class BrokerImpl implements Broker{

    /** Class Variables */
    private static Socket requestSocket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private static String ID;
    private static int brokerHash;

    private static ServerSocket serverSocket;

    private static HashMap<String, ArrayList<SocketAddress>> brokerHashtags;
    private static TreeMap<Integer, SocketAddress> brokerHashes;
    private static HashMap<String, SocketAddress> brokerChannelNames;

    private static HashMap<String, ArrayList<SocketAddress>> hashtagSubscriptions;
    private static HashMap<String, ArrayList<SocketAddress>> channelSubscriptions;

    public static void main(String[] args) {

        new BrokerImpl().initialize(4442);
    }


    @Override
    public void initialize(int port)  {

        try {
            System.out.println("Broker IP : " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        brokerHashtags = new HashMap<>();
        brokerChannelNames = new HashMap<>();
        brokerHashes = new TreeMap<>();
        hashtagSubscriptions = new HashMap<>();
        channelSubscriptions = new HashMap<>();

        Socket connectionSocket = null;

        try {
            serverSocket = new ServerSocket(port, 60, InetAddress.getLocalHost());
            System.out.println(InetAddress.getLocalHost());

            //CALCULATE BROKERHASH
            String serverSocketAddress = serverSocket.getLocalSocketAddress().toString();
            ID = String.format("Broker_%s", serverSocketAddress);
            brokerHash = calculateKeys(ID);
            System.out.println("My hash is:" + brokerHash);

            //GIVE TO ADDRESS KEEPER THE SOCKET ADDRESS OF THE BROKER
            connect();

            objectOutputStream.writeObject(1);
            objectOutputStream.flush();

            String string_socket = serverSocket.getLocalSocketAddress().toString().split("/")[1];
            String[] array = string_socket.split(":");
            InetAddress hear_ip = InetAddress.getByName(array[0]);
            int hear_port = Integer.parseInt(array[1]);
            SocketAddress hear_address = new InetSocketAddress(hear_ip, hear_port);

            objectOutputStream.writeObject(hear_address);
            objectOutputStream.flush();

            objectOutputStream.writeObject(brokerHash);
            objectOutputStream.flush();

            disconnect();

            while(true) {
                connectionSocket = serverSocket.accept();
                System.out.println(connectionSocket.getRemoteSocketAddress());
                new Handler(connectionSocket).start();
            }

        } catch(IOException e) {
            /* Crash the server if IO fails. Something bad has happened. */
            throw new RuntimeException("Could not create ServerSocket ", e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void connect(){

        try {
            Scanner in5 = new Scanner(System.in);
            System.out.println("Give Address Keeper IP address : ");
            String inetAddress = in5.nextLine();
            requestSocket = new Socket(InetAddress.getByName(inetAddress), 4000);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
            System.out.println("Broker is running...");
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void disconnect(){

        try {
            objectInputStream.close();
            objectOutputStream.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int calculateKeys(String id) {

        int digest = 0;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] bb = sha256.digest(id.getBytes(StandardCharsets.UTF_8));
            BigInteger bigInteger = new BigInteger(1, bb);
            digest = bigInteger.intValue();

            return digest;
        }
        catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        finally {
            return digest;
        }
    }

    @Override
    public HashMap<ChannelKey, String> filterConsumers(HashMap<ChannelKey, String> videoList, String channelName) {

        for (ChannelKey channelKey : videoList.keySet()) {
            if (channelKey.getChannelName() == channelName) {
                videoList.remove(channelKey);
            }
        }
        return videoList;
    }

    /** A Thread subclass to handle one client conversation */
    class Handler extends Thread {

        Socket socket;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        /**
         * Construct a Handler
         */
        Handler(Socket s) {
            socket = s;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            try {
                int option = (int) objectInputStream.readObject();

                /** Node Requests Handle */
                if (option == 0) {  // Send Broker Hashes

                    objectOutputStream.writeObject(brokerHashes);
                    objectOutputStream.flush();
                }

                /** Consumer - User Requests Handle */
                else if (option == 1) {  // Register User

                    String channel_name = (String) objectInputStream.readObject();
                    String topic = (String) objectInputStream.readObject();
                    String responseSuccess = "Subscribed to " + topic + " successfully.";
                    String responseFailure = "Attempt to subscribe has failed. Unable to find channel " + topic + ".";


                    SocketAddress user_hear_address = (SocketAddress) objectInputStream.readObject();

                    if (topic.charAt(0) == '#') {
                        if (hashtagSubscriptions.containsKey(topic)) {
                            ArrayList<SocketAddress> value = hashtagSubscriptions.get(topic);
                            value.add(user_hear_address); //proper address
                            hashtagSubscriptions.put(topic, value);
                        } else {
                            ArrayList<SocketAddress> value = new ArrayList<>();
                            value.add(user_hear_address); //proper address
                            hashtagSubscriptions.put(topic, value);
                        }
                        //GIVE SUCCESS MESSAGE
                        objectOutputStream.writeObject(responseSuccess);
                        objectOutputStream.flush();
                    } else {
                        if (channelSubscriptions.containsKey(topic)) {
                            ArrayList<SocketAddress> value = channelSubscriptions.get(topic);
                            value.add(user_hear_address); //proper address
                            channelSubscriptions.put(topic, value);

                            //GIVE SUCCESS MESSAGE
                            objectOutputStream.writeObject(responseSuccess);
                            objectOutputStream.flush();
                        } else {
                            if (brokerChannelNames.containsKey(topic)) {
                                ArrayList<SocketAddress> value = new ArrayList<>();
                                value.add(user_hear_address); //proper address
                                channelSubscriptions.put(topic, value);

                                //GIVE SUCCESS MESSAGE
                                objectOutputStream.writeObject(responseSuccess);
                                objectOutputStream.flush();
                            } else {
                                //GIVE FAILURE MESSAGE
                                objectOutputStream.writeObject(responseFailure);
                                objectOutputStream.flush();
                            }
                        }
                    }

                } else if (option == 2) {  // Get Topic Video List

                    PullOperation pull_operation = new PullOperation();

                    String channel_or_hashtag = (String) objectInputStream.readObject();
                    String channelName = (String) objectInputStream.readObject();
                    ArrayList<VideoInformation> videoList = new ArrayList<>();

                    if (channel_or_hashtag.charAt(0) == '#') {
                        ArrayList<SocketAddress> addresses = brokerHashtags.get(channel_or_hashtag);
                        if (addresses != null)
                            videoList = pull_operation.pullHashtags(channel_or_hashtag, addresses);
                    } else {
                        SocketAddress publisherAddress = brokerChannelNames.get(channel_or_hashtag);
                        if (publisherAddress != null)
                            videoList = pull_operation.pullChannel(publisherAddress);
                    }

                    /**FILTER-CONSUMERS-START*/
                    if (!videoList.isEmpty()) {
                        for (VideoInformation vi: videoList) {
                            if (vi.getChannelKey().getChannelName().equals(channelName)) {
                                videoList.remove(vi);
                            }
                        }
                    }
                    /**FILTER-CONSUMERS-END*/

                    objectOutputStream.writeObject(videoList);
                    objectOutputStream.flush();


                } else if (option == 3) {// Play Data

                    try {
                        PullOperation pull_operation = new PullOperation();

                        //RECEIVE CHANNEL KEY AND EXTRACT SOCKET ADDRESS OF PUBLISHER
                        ChannelKey key = (ChannelKey) objectInputStream.readObject();
                        SocketAddress publisherAddress = brokerChannelNames.get(key.getChannelName());

                        //PULL VIDEO FROM PUBLISHER
                        ArrayList<byte[]> video_chunks = pull_operation.pullVideo(key, publisherAddress);

                        //SEND VIDEO CHUNKS
                        objectOutputStream.writeObject(video_chunks.size());
                        objectOutputStream.flush();

                        while (!video_chunks.isEmpty()) {
                            byte[] clientToServer = video_chunks.remove(0);
                            objectOutputStream.write(clientToServer);
                            objectOutputStream.flush();
                        }

                    } catch (IOException | ClassNotFoundException ioException) {
                        ioException.printStackTrace();
                    } catch (NoSuchElementException nsee) {
                        objectOutputStream.writeObject("This channel doesn't exist");
                        objectOutputStream.flush();
                    }

                } else if (option == 4) { //FIRST CONNECTION

                    boolean unique = true;

                    //RECEIVE CHANNEL NAME AND SOCKET ADDRESS FOR CONNECTIONS
                    String channel_name = (String) objectInputStream.readObject();
                    SocketAddress socketAddress = (SocketAddress) objectInputStream.readObject();

                    //CHECK IF CHANNEL NAME IS UNIQUE
                    if (brokerChannelNames.containsKey(channel_name)) {
                        unique = false;
                    }else {
                        brokerChannelNames.put(channel_name, socketAddress);
                    }

                    objectOutputStream.writeObject(unique);
                    objectOutputStream.flush();

                    /** Publisher Requests Handle */

                } else if (option == 7) {  // Notify Brokers for Hashtags

                    String hashtag = (String) objectInputStream.readObject();
                    String action = (String) objectInputStream.readObject();
                    SocketAddress socketAddress = (SocketAddress) objectInputStream.readObject();

                    if (action.equals("ADD")) {
                        if (brokerHashtags.get(hashtag) == null) {
                            ArrayList<SocketAddress> value = new ArrayList<>();
                            value.add(socketAddress);
                            brokerHashtags.put(hashtag, value);
                        } else {
                            ArrayList<SocketAddress> value = brokerHashtags.get(hashtag);
                            value.add(socketAddress);
                            brokerHashtags.put(hashtag, value);
                        }
                    } else if (action.equals("REMOVE")) {
                        if (brokerHashtags.get(hashtag).size() > 1) {
                            ArrayList<SocketAddress> value = brokerHashtags.get(hashtag);
                            value.remove(socketAddress);
                            brokerHashtags.put(hashtag, value);
                        } else {
                            brokerHashtags.remove(hashtag);
                        }
                    }
                } else if (option == 8) { //Notify Brokers for changes

                    String action = (String) objectInputStream.readObject();
                    if (action.equals("hashtag")) {
                        String hashtag = (String) objectInputStream.readObject();
                        ChannelKey channelKey = (ChannelKey) objectInputStream.readObject();
                        String title = (String) objectInputStream.readObject();
                        ArrayList<String> associatedHashtags = (ArrayList<String>) objectInputStream.readObject();

                        if (hashtagSubscriptions.get(hashtag) != null){
                            VideoInformation vi = new VideoInformation(channelKey, title, associatedHashtags);
                            for (SocketAddress socketAddress : hashtagSubscriptions.get(hashtag)) {
                                new Notifier(socketAddress, vi, hashtag).start();
                            }
                        }

                    } else if (action.equals("channel")) {
                        ChannelKey channelKey = (ChannelKey) objectInputStream.readObject();
                        String title = (String) objectInputStream.readObject();
                        ArrayList<String> associatedHashtags = (ArrayList<String>) objectInputStream.readObject();

                        if (channelSubscriptions.get(channelKey.getChannelName()) != null) {
                            VideoInformation vi = new VideoInformation(channelKey, title, associatedHashtags);
                            for (SocketAddress socketAddress : channelSubscriptions.get(channelKey.getChannelName())) {
                                new Notifier(socketAddress, vi, null).start();
                            }
                        }
                    }

                } else if (option == 9){

                    String channelNameOrHashtag = (String) objectInputStream.readObject();
                    SocketAddress socketAddress = (SocketAddress) objectInputStream.readObject();

                    if (channelNameOrHashtag.charAt(0) != '#') {
                        ArrayList<SocketAddress> updatedSocketAddresses = new ArrayList<>(channelSubscriptions.get(channelNameOrHashtag));
                        updatedSocketAddresses.remove(socketAddress);
                        channelSubscriptions.put(channelNameOrHashtag, updatedSocketAddresses);
                    } else{
                        ArrayList<SocketAddress> updatedSocketAddresses = new ArrayList<>(hashtagSubscriptions.get(channelNameOrHashtag));
                        updatedSocketAddresses.remove(socketAddress);
                        hashtagSubscriptions.put(channelNameOrHashtag, updatedSocketAddresses);
                    }
                }
                try {
                    objectInputStream.close();
                    objectOutputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**NEW HANDLER TO SEND NOTIFICATION FOR NEW VIDEOS TO SUBSCRIBED USERS*/
    class Notifier extends Thread {

        VideoInformation vi;
        ChannelKey channelKey;
        String title;
        String hashtag;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        /** Construct a Handler */
        Notifier(SocketAddress socketAddress, VideoInformation vi, String hashtag) {
            this.vi = vi;
            this.channelKey = vi.getChannelKey();
            this.title = vi.getTitle();
            this.hashtag=hashtag;

            Socket connectionSocket = new Socket();
            try {
                connectionSocket.connect(socketAddress);
                objectOutputStream = new ObjectOutputStream(connectionSocket.getOutputStream());
                objectInputStream = new ObjectInputStream(connectionSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            String notificationMessage;

            if (hashtag == null) {
                notificationMessage = "The channel " + channelKey.getChannelName() +
                        " that you are subscribed to has uploaded a new video with title "  + title +
                        " and videoID " + channelKey.getVideoID() + ".";
            } else {
                notificationMessage = "There is a new video in topic " + hashtag +
                        " that you are subscribed, from the channel " + channelKey.getChannelName() + " and title " +
                        title + " with videoID " + channelKey.getVideoID() + ".";
            }

            try {
                objectOutputStream.writeObject(3);
                objectOutputStream.flush();

                objectOutputStream.writeObject(notificationMessage);
                objectOutputStream.flush();

                objectOutputStream.writeObject(vi);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            disconnect();
        }
    }

}