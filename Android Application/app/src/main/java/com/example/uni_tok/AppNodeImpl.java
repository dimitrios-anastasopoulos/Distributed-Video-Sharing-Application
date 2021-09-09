package com.example.uni_tok;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeMap;

public class AppNodeImpl {

    private static Socket requestSocket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    private static Channel channel;

    private static TreeMap<Integer, SocketAddress> brokerHashes = new TreeMap<>();
    private static SocketAddress channelBroker;

    private static ServerSocket serverSocket;

    private static SocketAddress hear_address;

    private static ArrayList<String> subscribedToChannels = new ArrayList<>();
    private static ArrayList<String> subscribedToHashtags = new ArrayList<>();

    private static ArrayList<VideoInformation> searchVideoList = new ArrayList<>();
    private static ArrayList<VideoInformation> homePageVideoList  = new ArrayList<>();


    public static void main(String[] args) {

        new AppNodeImpl().initialize(4960);
    }


    public static void init(int port) {
        try {
            serverSocket = new ServerSocket(port, 60, InetAddress.getByName("10.0.2.15"));

            File uploadedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Uploaded Videos/");
            uploadedDir.mkdirs();
            File fetchedDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Fetched Videos/");
            fetchedDir.mkdirs();

        } catch (IOException io) {
            Log.d("Initialization Error", "Couldn't initialise App Node!");
        }
    }

    public static Channel getChannel() {
        return channel;
    }

    public static void setChannel(Channel channel) {
        AppNodeImpl.channel = channel;
    }

    public static void getBrokers() throws IOException, ClassNotFoundException {
        objectOutputStream.writeObject(2);
        objectOutputStream.flush();

        brokerHashes = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
    }

    public static boolean setChannelBroker(String name) throws IOException, ClassNotFoundException {
        boolean unique;

        //CHANNEL NAME
        channel = new Channel(name);

        //CONNECT TO APPROPRIATE BROKER
        channelBroker = hashTopic(channel.getChannelName());
        connect(channelBroker);

        //SEND OPTION 4 FOR INITIALIZATION
        objectOutputStream.writeObject(4);
        objectOutputStream.flush();

        //SEND CHANNEL NAME
        objectOutputStream.writeObject(channel.getChannelName());
        objectOutputStream.flush();

        //SEND SOCKET ADDRESS FOR CONNECTIONS
        init(4960);
        String string_socket = serverSocket.getLocalSocketAddress().toString().split("/")[1];
        InetAddress hear_ip = InetAddress.getByName("127.0.0.1");
        int hear_port = 5529;
        Log.d("HEAR IP", hear_ip.toString());
        Log.d("HEAR PORT", Integer.toString(hear_port));
        hear_address = new InetSocketAddress(hear_ip, hear_port);
        objectOutputStream.writeObject(hear_address);
        objectOutputStream.flush();

        //GET RESPONSE IF CHANNEL NAME IS UNIQUE
        unique = (boolean) objectInputStream.readObject();

        return unique;

    }

    public static boolean setSearchTopicVideoList(String topic) {

        searchVideoList.clear();

        boolean fetched_successfully = false;
        //Get right broker
        SocketAddress socketAddress = hashTopic(topic);

        //Connect to that broker
        connect(socketAddress);

        try {
            //Write option
            objectOutputStream.writeObject(2);
            objectOutputStream.flush();

            //Write channel name or hashtag
            objectOutputStream.writeObject(topic);
            objectOutputStream.flush();

            //Write this user's channel name
            objectOutputStream.writeObject(channel.getChannelName());
            objectOutputStream.flush();

            //Read videoList
            searchVideoList = (ArrayList<VideoInformation>) objectInputStream.readObject();

            fetched_successfully = true;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        return fetched_successfully;
    }

    /**
     * This page uploads to Home Page a video every time a
     * channel or hashtag we are subscribed to uploads a video
     */
    public synchronized static void refreshHomePage(VideoInformation vi) {

        ArrayList<ChannelKey> keys = new ArrayList<>();
        for (VideoInformation info : homePageVideoList) {
            keys.add(info.getChannelKey());
        }

        if (!keys.contains(vi.getChannelKey())) {
            homePageVideoList.add(0, vi);
            if (homePageVideoList.size() > 10) {
                homePageVideoList.remove(homePageVideoList.size() - 1);
            }
        }
    }

    public synchronized static void refreshHomePage(ArrayList<VideoInformation> vi_list) {

        ArrayList<ChannelKey> keys = new ArrayList<>();
        for (VideoInformation info : homePageVideoList) {
            keys.add(info.getChannelKey());
        }

        for (VideoInformation vi : vi_list) {
            if (!keys.contains(vi.getChannelKey())) {
                homePageVideoList.add(vi);
            }
        }

        Collections.sort(homePageVideoList);
        while (homePageVideoList.size() > 10) {
            homePageVideoList.remove(homePageVideoList.size() - 1);
        }
    }

    public synchronized static void refreshHomePage(String topic) {
        if (topic.charAt(0) == '#') {
            for (VideoInformation item : homePageVideoList) {
                if (item.getHashtags().contains(topic)) {
                    homePageVideoList.remove(item);
                }
            }
        }
        else {
            for (VideoInformation item : homePageVideoList) {
                if (item.getChannelName().equals(topic)) {
                    homePageVideoList.remove(item);
                }
            }
        }
    }

    public static ArrayList<VideoInformation> getHomePageVideoList() {
        return homePageVideoList;
    }

    public static ArrayList<VideoInformation> getSearchTopicVideoList() {
        return searchVideoList;
    }

    public static ArrayList<String> getSubscribedToChannels() {
        return subscribedToChannels;
    }

    public static ArrayList<String> getSubscribedToHashtags() {
        return subscribedToHashtags;
    }

    public static boolean Upload(String path, ArrayList<String> associatedHashtags, String videoName){
        VideoFile videoFile = new VideoFile(path, associatedHashtags, videoName);

        ChannelKey channelKey = new ChannelKey((AppNodeImpl.getChannel()).getChannelName(),
                AppNodeImpl.getChannel().getCounterVideoID()).setDate(videoFile.getDate());
        HashMap<String, String> notificationHashtags = (AppNodeImpl.getChannel()).addVideoFile(videoFile, channelKey);

        if (!notificationHashtags.isEmpty()) {
            for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                AppNodeImpl.notifyBrokersForHashTags(item.getKey(), item.getValue());
        }

        AppNodeImpl.notifyBrokersForChanges(channelKey, associatedHashtags, videoName, associatedHashtags, true);

        return true;
    }

    public static void handleRequest() {
        try {
            while(true) {
                Log.d("WAITING FOR CONNECTIONS", "START");
                Socket connectionSocket = serverSocket.accept();
                new ServeRequest(connectionSocket).start();
            }
        } catch(IOException e) {
            /* Crash the server if IO fails. Something bad has happened. */
            Log.d("SERVER SOCKET", "CONNECTION ACCEPT FAIL");
            throw new RuntimeException("Could not create ServerSocket ", e);
        } finally {
            try {
                Log.d("Socket State", "Closed");
                serverSocket.close();
            } catch (IOException | NullPointerException ioException) {
                Log.d("Exception : ", ioException.getMessage());
            }
        }
    }

    public void initialize(int port) {

        //GET BROKERS
        connect();
        try {
            objectOutputStream.writeObject(2);
            objectOutputStream.flush();

            brokerHashes = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        runUser();

    }

    public static boolean addHashTag(VideoFile video, ArrayList<String> hashtagsAdded) {

        ArrayList<String> hashtags = new ArrayList<>();
        for (String hashtag : hashtagsAdded) {
            if (!video.getAssociatedHashtags().contains(hashtag))
                hashtags.add(hashtag);
        }

        if (hashtags.isEmpty()) {
            Log.d("ADD HASHTAGS", "No hashtags found to add.");
        } else {

            HashMap<String, String> notificationHashtags = channel.updateVideoFile(video, hashtags, "ADD");
            if (!notificationHashtags.isEmpty()) {
                for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                    notifyBrokersForHashTags(item.getKey(), item.getValue());
            }

            ChannelKey channelKey = new ChannelKey(channel.getChannelName(), video.getVideoID());
            channelKey.setDate(video.getDate());

            notifyBrokersForChanges(channelKey, hashtags, video.getVideoName(), video.getAssociatedHashtags(), false);
        }
        return true;
    }

    public static boolean removeHashTag(VideoFile video, ArrayList<String> hashtagsRemoved) {

        ArrayList<String> hashtags = new ArrayList<>();
        for (String hashtag : hashtagsRemoved) {
            if (video.getAssociatedHashtags().contains(hashtag)) {
                hashtags.add(hashtag);
            }
        }

        if (hashtags.isEmpty()) {
            Log.d("REMOVE HASHTAGS", "No hashtags found to remove.");
        } else {

            HashMap<String, String> notificationHashtags = channel.updateVideoFile(video, hashtags, "REMOVE");
            if (!notificationHashtags.isEmpty()) {
                for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                    notifyBrokersForHashTags(item.getKey(), item.getValue());
            }
        }
        return true;
    }

//    @Override
//    public List<Broker> getBrokerList() {
//        return brokers;
//    }

    public static SocketAddress hashTopic(String hashtopic) {

        int digest;
        SocketAddress brokerAddress = brokerHashes.get(brokerHashes.firstKey());
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            //byte[] bb = sha256.digest(hashtopic.getBytes(StandardCharsets.UTF_8));
            byte[] bb = sha256.digest(hashtopic.getBytes(Charset.forName("UTF-8")));
            BigInteger bigInteger = new BigInteger(1, bb);
            digest = bigInteger.intValue();

            //Fit to the right broker
            for (int hash : brokerHashes.keySet()) {
                if (digest <= hash) {
                    brokerAddress = brokerHashes.get(hash);
                    break;
                }
            }
        }
        catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } finally {
            return brokerAddress;
        }
    }

    public static void push(int id, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream) throws NoSuchElementException {

        ArrayList<byte[]> chunks = generateChunks(channel.getVideoFile_byID(id));

        try {
            objectOutputStream.writeObject(true);
            objectOutputStream.flush();

            objectOutputStream.writeObject(chunks.size());
            objectOutputStream.flush();

            while (!chunks.isEmpty()) {
                byte[] clientToServer = chunks.remove(0);
                objectOutputStream.write(clientToServer);
                objectOutputStream.flush();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

//    @Override
//    public void notifyFailure(Broker broker) {
//
//    }

    public static void notifyBrokersForHashTags(String hashtag, String action) {
        SocketAddress socketAddress = hashTopic(hashtag);
        connect(socketAddress);
        try {
            objectOutputStream.writeObject(7);
            objectOutputStream.flush();

            objectOutputStream.writeObject(hashtag);
            objectOutputStream.flush();

            objectOutputStream.writeObject(action);
            objectOutputStream.flush();

            objectOutputStream.writeObject(hear_address);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public static void notifyBrokersForChanges(ChannelKey channelKey, ArrayList<String> hashtags, String title, ArrayList<String> associatedHashtags, boolean action) {

        if (!hashtags.isEmpty()) {
            for (String hashtag : hashtags) {
                SocketAddress socketAddress = hashTopic(hashtag);
                connect(socketAddress);
                try {
                    objectOutputStream.writeObject(8);
                    objectOutputStream.flush();

                    objectOutputStream.writeObject("hashtag");
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(hashtag);
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(channelKey);
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(title);
                    objectOutputStream.flush();

                    objectOutputStream.writeObject(associatedHashtags);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }
        }

        if (action) {
            SocketAddress socketAddress = hashTopic(channelKey.getChannelName());
            connect(socketAddress);
            try {
                objectOutputStream.writeObject(8);
                objectOutputStream.flush();

                objectOutputStream.writeObject("channel");
                objectOutputStream.flush();

                objectOutputStream.writeObject(channelKey);
                objectOutputStream.flush();

                objectOutputStream.writeObject(title);
                objectOutputStream.flush();

                objectOutputStream.writeObject(associatedHashtags);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }
    }

    public static ArrayList<byte[]> generateChunks(VideoFile video) {

        ArrayList<byte[]> my_arraylist = new ArrayList<byte []>();

        boolean flag = true;
        int i = 0;
        byte[] inputBuffer = video.getVideoFileChunk();

        while (i < inputBuffer.length) {
            byte[] buffer = new byte[4096];
            for (int j = 0;j < buffer.length;j++) {
                if (i < inputBuffer.length)
                    buffer[j] = inputBuffer[i++];
            }
            my_arraylist.add(buffer);
        }
        return my_arraylist;
    }

//    public TreeMap<Integer, SocketAddress> getBrokerMap() {
//
//        connect();
//        try {
//            objectOutputStream.writeObject(2);
//            objectOutputStream.flush();
//
//            brokerHashes = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//        } finally {
//            disconnect();
//        }
//        return brokerHashes;

        /*
        System.out.println("I am in here");
        try {
            serverSocket = new ServerSocket(4950, 60, InetAddress.getLocalHost());
            updateNodes();
            serverSocket.setSoTimeout(2000);
            try {
                Socket connectionSocket = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(connectionSocket.getInputStream());
                int option = (int) objectInputStream.readObject();
                brokerHashes = (TreeMap<Integer, SocketAddress>) objectInputStream.readObject();
            } catch (SocketTimeoutException ste) {
                System.out.println("Can't connect to a server. Terminating....");
                System.exit(-1);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
        */

//    }

    public static void connect(SocketAddress socketAddress) {

        try {
            Log.d("Enter connection", "Enter connection");
            requestSocket = new Socket();
            requestSocket.connect(socketAddress);
            requestSocket.setSoTimeout(0);
            Log.d("Connected!", "Connected!");
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void connect() {

        try {
            Scanner in5 = new Scanner(System.in);
            System.out.println("Give Address Keeper IP address : ");
            String inetAddress = in5.nextLine();
            requestSocket = new Socket(InetAddress.getByName(inetAddress), 4000);
            objectOutputStream = new ObjectOutputStream(requestSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            objectInputStream.close();
            objectOutputStream.close();
            requestSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void updateNodes() throws IOException {
//
//        System.out.println("In update Nodes");
//
//        MulticastSocket socket = new MulticastSocket(multicastPort);
//        socket.joinGroup(multicastIP);
//
//        //SEND % AND SOCKET ADDRESS TO RECEIVE BROKERHASHES
//        String appNodeChar = "%";
//        String address = serverSocket.getLocalSocketAddress().toString();
//        String appNodeChar_address = appNodeChar + "," + address;
//        byte[] buffer = appNodeChar_address.getBytes();
//
//        //MAKE PACKET AND SEND IT
//        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastIP, multicastPort);
//        socket.send(packet);
//
//        try {
//            TimeUnit.SECONDS.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Packet sent");
//
//        socket.leaveGroup(multicastIP);
//
//        //CLOSE SOCKET
//        socket.close();
//
//    }


    public static boolean register(SocketAddress socketAddress, String topic) {

        connect(socketAddress);
        boolean successful_subscription = false;

        try {
            objectOutputStream.writeObject(1);
            objectOutputStream.flush();

            objectOutputStream.writeObject(channel.getChannelName());
            objectOutputStream.flush();

            objectOutputStream.writeObject(topic);
            objectOutputStream.flush();

            objectOutputStream.writeObject(hear_address);
            objectOutputStream.flush();

            String response = (String) objectInputStream.readObject();
            Log.d("RESPONSE", response);

            if (response.contains("successfully")) {
                if (topic.charAt(0) == '#') {
                    subscribedToHashtags.add(topic);
                } else {
                    subscribedToChannels.add(topic);
                }
                successful_subscription = true;
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        return successful_subscription;
    }

    public static boolean unregister(SocketAddress socketAddress, String topic) {

        boolean successful_unsubscription = false;

        try {
            connect(socketAddress);

            objectOutputStream.writeObject(9);
            objectOutputStream.flush();

            objectOutputStream.writeObject(topic);
            objectOutputStream.flush();

            objectOutputStream.writeObject(hear_address);
            objectOutputStream.flush();

            if (topic.charAt(0) == '#'){
                subscribedToHashtags.remove(topic);
            } else {
                subscribedToChannels.remove(topic);
            }
            successful_unsubscription = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        return successful_unsubscription;
    }

    public static boolean playData(ChannelKey ck) {

        File nf = null;
        FileOutputStream fw = null;
        String channelName = ck.getChannelName();
        int videoID = ck.getVideoID();
        boolean successfullPull = true;

        try {
            //CONNECTING TO BROKER RESPONSIBLE FOR CHANNEL, THAT HAS THE VIDEO WE ASKED FOR
            SocketAddress brokerAddress = hashTopic(channelName);
            connect(brokerAddress);

            objectOutputStream.writeObject(3);
            objectOutputStream.flush();

            objectOutputStream.writeObject(ck);
            objectOutputStream.flush();

            //RECEIVE VIDEO FILE CHUNKS
            byte[] chunk;
            ArrayList<byte[]> chunks = new ArrayList<>();
            int size = (int) objectInputStream.readObject();

            if (size == 0) {
                System.out.println("CHANNEL HAS NO VIDEO WITH THIS ID...");
            }
            //REBUILD CHUNKS FOR TESTING
            else {
                for (int i = 0; i < size; i++) {
                    chunk = new byte[4096];
                    objectInputStream.readFully(chunk);
                    //chunk = objectInputStream.readAllBytes();
                    chunks.add(chunk);
                }
                try {
                    nf = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() +
                            "/Fetched Videos/" + channelName + "_" + videoID + ".mp4");
                    fw = new FileOutputStream(nf, true);
                    for (byte[] ar : chunks) {
                        fw.write(ar);
                    }

                } catch (IOException e) {
                    successfullPull = false;
                    e.printStackTrace();
                } finally {
                    if (fw != null) {
                        fw.close();
                    }
                    disconnect();
                }
            }
        } catch(IOException | ClassNotFoundException e){
            successfullPull = false;
            e.printStackTrace();
        }
        return successfullPull;
    }

    public static HashMap<ChannelKey, String> getChannelVideoMap() {
        return channel.getChannelVideoNames();
    }

    public static HashMap<ChannelKey, ArrayList<String>> getChannelHashtagsMap() {
        return channel.getChannelAssociatedHashtags();
    }

    public static HashMap<ChannelKey, String> getHashtagVideoMap(String hashtag) {
        return channel.getChannelVideoNamesByHashtag(hashtag);
    }

/*
    //CHANGES HAVE BEEN MADE
    class RequestHandler extends Thread {

        public ServerSocket serverSocket;
        public Socket connectionSocket;

        public RequestHandler(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {

            try {
                while(true) {
                    connectionSocket = serverSocket.accept();
                    new ServeRequest(connectionSocket).start();
                }
            } catch(IOException e) {
                //Crash the server if IO fails. Something bad has happened.
                throw new RuntimeException("Could not create ServerSocket ", e);
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException | NullPointerException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
    */
/*
    class ServeRequest extends Thread {

        private Socket socket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        ServeRequest(Socket s) {
            socket = s;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
6
        public void run() {

            try{

                int option = (int) objectInputStream.readObject();

                if (option == 1) { //Pull List

                    //Choice between sending whole channel or files based on hashtag
                    String choice = (String) objectInputStream.readObject();
                    System.out.println(choice);
                    if (choice.equals("CHANNEL")) {
                        HashMap<ChannelKey, String> videoList = getChannelVideoMap();
                        objectOutputStream.writeObject(videoList);
                    }
                    else {
                        HashMap<ChannelKey, String> videoList = getHashtagVideoMap(choice);
                        objectOutputStream.writeObject(videoList);
                    }

                } else if (option == 2) { //Pull Video

                    ChannelKey channelKey = (ChannelKey) objectInputStream.readObject();
                    try {
                        push(channelKey.getVideoID(), objectInputStream, objectOutputStream);
                    } catch (NoSuchElementException nsee) {
                        objectOutputStream.writeObject(false);
                        objectOutputStream.flush();
                    }

                } else if (option == 3) {

                    String notificationMessage = (String) objectInputStream.readObject();
                    System.out.println(notificationMessage);

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    objectInputStream.close();
                    objectOutputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
*/
    public void runUser() {
        //BUILD INTERFACE
        Scanner in = new Scanner(System.in);
        int end = 0;
        String choice;
        do {
            System.out.println("\n===== Menu =====");
            //Consumer Methods
            System.out.println("1. Register User to hashtag or channel");
            System.out.println("2. Get Topic Video List");
            System.out.println("3. Unregister User from hashtag or channel");
            //Publisher Methods
            System.out.println("4. Add Hashtags to a Video");
            System.out.println("5. Remove Hashtags from a Video");
            System.out.println("6. Upload Video");
            System.out.println("7. Delete Video");
            System.out.println("8. Check Profile");
            System.out.println("0. Exit");
            choice = in.nextLine();
            if (choice.equals("1")) {

                String topic;
                System.out.print("Please select a topic (hashtag/channel) that you want to subscribe: ");
                topic = in.nextLine();
                SocketAddress socketAddress = hashTopic(topic);
                register(socketAddress, topic);

            } else if (choice.equals("2")) {

                //Give hashtag
                System.out.print("Please give the hashtag or the channel that you want to search for: ");
                String channel_or_hashtag = in.nextLine();

                //Get right broker
                SocketAddress socketAddress = hashTopic(channel_or_hashtag);

                //Connect to that broker
                connect(socketAddress);

                HashMap<ChannelKey, String> videoList = new HashMap<>();

                try {
                    //Write option
                    objectOutputStream.writeObject(2);
                    objectOutputStream.flush();

                    //Write channel name or hashtag
                    objectOutputStream.writeObject(channel_or_hashtag);
                    objectOutputStream.flush();

                    //Write this user's channel name
                    objectOutputStream.writeObject(channel.getChannelName());
                    objectOutputStream.flush();

                    //Read videoList
                    videoList = (HashMap<ChannelKey, String>) objectInputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }

                boolean wantVideo = true;
                if (videoList.isEmpty()) {
                    System.out.println("No videos\n");
                    wantVideo = false;
                }
                //CHOOSE SOME VIDEO OR GO BACK
                while (wantVideo) {
                    for (Map.Entry<ChannelKey, String> item : videoList.entrySet()) {
                        System.out.println("Channel Name : " + item.getKey().getChannelName() + "     Video ID : "
                                + item.getKey().getVideoID() + "    Video Name : " +item.getValue());
                    }

                    System.out.print("Do you want to see a video from these? (y/n)");
                    String answer = in.nextLine();

                    if (answer.equals("y")) {
                        //playData(videoList);
                    } else wantVideo = false;
                }

            } else if (choice.equals("3")) {

                String topic;
                System.out.print("Please select 'channel' if you want to unsubscribe from a channel " +
                        "or 'hashtag' to unsubscribe from a hashtag: ");
                topic = in.nextLine();

                if (topic.equals("channel")){
                    boolean wantUnsubscribe = true;
                    Scanner in2 = new Scanner(System.in);
                    String answer = "";
                    while (wantUnsubscribe){
                        if (subscribedToChannels.isEmpty()){
                            if (answer.equals(""))
                                System.out.println("You aren't subscribed to any channel. Unsubscribe cancelled...");
                            answer = "n";
                        } else {
                            System.out.println("Channels that you are subscribed: ");
                            for (String channel : subscribedToChannels)
                                System.out.println(channel);
                            System.out.println("Do you want to unsubscribe from one of the above channels? (y/n)");
                            answer = in2.nextLine();
                        }

                        if (answer.equals("y")){
                            try {
                                System.out.println("Give me the channel name that you want to unsubscribe: ");
                                String channelName = in2.nextLine();

                                if (!subscribedToChannels.contains(channelName)) {
                                    System.out.println("You are not subscribed to channel " + channelName);
                                    continue;
                                }

                                subscribedToChannels.remove(channelName);

                                SocketAddress socketAddress1 = hashTopic(channelName);

                                unregister(socketAddress1, channelName);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            wantUnsubscribe = false;
                        }
                    }
                } else if (topic.equals("hashtag")){
                    boolean wantUnsubscribe = true;
                    Scanner in2 = new Scanner(System.in);
                    String answer = "";
                    while (wantUnsubscribe){
                        if (subscribedToHashtags.isEmpty()){
                            if (answer.equals(""))
                                System.out.println("You aren't subscribed to any hashtag. Unsubscribe cancelled...");
                            answer = "n";
                        } else {
                            System.out.println("Hashtags that you are subscribed: ");
                            for (String hashtag : subscribedToHashtags)
                                System.out.println(hashtag);
                            System.out.println("Do you want to unsubscribe from one of the above hashtags? (y/n)");
                            answer = in2.nextLine();
                        }

                        if (answer.equals("y")){
                            try {
                                System.out.println("Give me the hashtag that you want to unsubscribe: ");
                                String hashtag = in2.nextLine();

                                if (!subscribedToHashtags.contains(hashtag)) {
                                    System.out.println("You are not subscribed to hashtag " + hashtag);
                                    continue;
                                }

                                subscribedToHashtags.remove(hashtag);

                                SocketAddress socketAddress1 = hashTopic(hashtag);

                                unregister(socketAddress1, hashtag);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            wantUnsubscribe = false;
                        }
                    }
                } else {
                    System.out.println("You didn't choose 'channel' or 'hashtag'. Unsubscribe cancelled...");
                }

            } else if (choice.equals("4")) {

                int videoID;

                if (channel.getID_VideoFileMap().isEmpty()) {
                    System.out.println("The channel doesn't have any videos to add hashtags.");
                    continue;
                }

                System.out.println(channel.toString());

                System.out.print("Please give the videoID of the video you want to add a hashtag: ");
                videoID = Integer.parseInt(in.nextLine());

                VideoFile video = channel.getVideoFile_byID(videoID);

//                addHashTag(video);

            } else if (choice.equals("5")) {

                int videoID;

                if (channel.getID_VideoFileMap().isEmpty()) {
                    System.out.println("The channel doesn't have any videos to remove hashtags.");
                    continue;
                }

                System.out.println(channel.toString());

                System.out.print("Please give the videoID of the video you want to remove a hashtag: ");
                videoID = Integer.parseInt(in.nextLine());

                VideoFile video = channel.getVideoFile_byID(videoID);

                //removeHashTag(video);

            } else if (choice.equals("6")) {

//                JFileChooser chooser = new JFileChooser(){
//                    @Override
//                    protected JDialog createDialog(Component parent) throws HeadlessException {
//                        JDialog jDialog = super.createDialog(parent);
//                        jDialog.setAlwaysOnTop(true);
//                        return jDialog;
//                    }
//                };
//                FileNameExtensionFilter filter = new FileNameExtensionFilter(".mp4", "mp4");
//                chooser.setFileFilter(filter);
//                int returnVal = chooser.showOpenDialog(null);
//                if (returnVal == JFileChooser.APPROVE_OPTION) {
//                    System.out.println("You chose to upload this file: "+chooser.getSelectedFile().getAbsolutePath());
//
//                    ArrayList<String> associatedHashtags = new ArrayList<>();
//
//                    String filepath = chooser.getSelectedFile().getAbsolutePath();
//
//                    String videoTitle = chooser.getSelectedFile().getName().split("\\.")[0];
//
//                    String hashtag;
//                    while (true) {
//                        System.out.print("Do you want to add a hashtag to your video? (y/n) ");
//                        String answer = in.nextLine();
//                        if (answer.equals("n")) {
//                            break;
//                        }
//
//                        System.out.print("Please give a hashtag for the video: ");
//                        hashtag = in.nextLine();
//
//                        if (!associatedHashtags.contains(hashtag)) {
//                            associatedHashtags.add(hashtag);
//                        }
//                    }
//
//                    VideoFile video = new VideoFile(filepath, associatedHashtags, videoTitle);
//
//                    HashMap<String, String> notificationHashtags = channel.addVideoFile(video);
//
//                    boolean notExists = true;
//                    try {
//                        Path source = Paths.get(filepath);
//                        Path target = Paths.get("Uploaded Videos\\" + videoTitle + ".mp4");
//                        Files.copy(source, target);
//                    } catch (IOException e) {
//                        if (e instanceof FileAlreadyExistsException) {
//                            System.out.println("There is already a video with that name. Upload cancelled...\n");
//                        }
//                        notExists = false;
//                    }
//
//                    if (notExists) {
//                        if (!notificationHashtags.isEmpty()) {
//                            for (Map.Entry<String, String> item : notificationHashtags.entrySet())
//                                notifyBrokersForHashTags(item.getKey(), item.getValue());
//                        }
//
//                        ChannelKey channelKey = new ChannelKey(channel.getChannelName(), video.getVideoID());
//                        notifyBrokersForChanges(channelKey, associatedHashtags, videoTitle, true);
//                    } else {
//                        channel.removeVideoFile(video);
//                    }
//                } else {
//                    System.out.println("You didn't choose any file. Upload cancelled...");
//                }
/*
                String filepath;
                String videoTitle;
                String hashtag;
                ArrayList<String> associatedHashtags = new ArrayList<>();

                System.out.print("Please give the path of the video you want to upload: ");
                filepath = in.nextLine();

                System.out.print("Title of the video: ");
                videoTitle = in.nextLine();

                while (true) {
                    System.out.print("Do you want to add a hashtag to your video? (y/n) ");
                    String answer = in.nextLine();
                    if (answer.equals("n")) {
                        break;
                    }

                    System.out.print("Please give a hashtag for the video: ");
                    hashtag = in.nextLine();

                    if (!associatedHashtags.contains(hashtag)) {
                        associatedHashtags.add(hashtag);
                    }
                }

                VideoFile video = new VideoFile(filepath, associatedHashtags, videoTitle);

                HashMap<String, String> notificationHashtags = channel.addVideoFile(video);
                boolean notExists = true;
                try {
                    Path source = Paths.get(filepath);
                    Path target = Paths.get("Uploaded Videos\\" + videoTitle + ".mp4");
                    Files.copy(source, target);
                } catch (IOException e) {
                    if (e instanceof FileAlreadyExistsException) {
                        System.out.println("There is already a video with that name. Upload cancelled...\n");
                    }
                    e.printStackTrace();
                    notExists = false;
                }

                if (notExists) {
                    if (!notificationHashtags.isEmpty()) {
                        for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                            notifyBrokersForHashTags(item.getKey(), item.getValue());
                    }

                    ChannelKey channelKey = new ChannelKey(channel.getChannelName(), video.getVideoID());
                    notifyBrokersForChanges(channelKey, associatedHashtags, videoTitle, associatedHashtags, true);
                } else {
                    channel.removeVideoFile(video);
                }

 */
            }

            else if (choice.equals("7")){

                int videoID;

                if (channel.getID_VideoFileMap().isEmpty()) {
                    System.out.println("The channel doesn't have any videos to delete.");
                    continue;
                }

                System.out.println(channel.toString());

                System.out.print("Please give the ID of the video you want to delete: ");
                videoID = Integer.parseInt(in.nextLine());

                VideoFile video = channel.getVideoFile_byID(videoID);

                HashMap<String, String> notificationHashtags = channel.removeVideoFile(video);

                try {
                    Path file = Paths.get("Uploaded Videos\\" + video.getVideoName() + ".mp4");
                    Files.delete(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!notificationHashtags.isEmpty()) {
                    for (Map.Entry<String, String> item : notificationHashtags.entrySet())
                        notifyBrokersForHashTags(item.getKey(), item.getValue());
                }

            }else if (choice.equals("8")) {

                System.out.println(channel);

            }else if (choice.equals("0")) {

                end = 1;

            }
        } while (end == 0);

        System.exit(0);

    }
}