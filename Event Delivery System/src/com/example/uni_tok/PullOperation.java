package com.example.uni_tok;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class PullOperation {

    private ArrayList<VideoInformation> hashtagVideoList;
    private CountDownLatch latch;

    PullOperation() {
        hashtagVideoList = null;
        latch = null;
    }

    public ArrayList<VideoInformation> pullHashtags(String channel_or_hashtag, ArrayList<SocketAddress> addresses) {

        hashtagVideoList = new ArrayList<>();

        //Check if this is channel name or hashtag
        try {
            //We use countdown latch and executor service to control threads when they finish
            //in order to continue our calling process only when all threads are finished
            int n_addresses = addresses.size();
            latch = new CountDownLatch(n_addresses);
            ExecutorService executorService = Executors.newFixedThreadPool(n_addresses);
            IntStream.range(0, n_addresses).forEach(index -> executorService.execute(
                    new PullThread(channel_or_hashtag, addresses.get(index))
            ));
            executorService.shutdown();

            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return hashtagVideoList;

    }

    public ArrayList<VideoInformation> pullChannel(SocketAddress publisherAddress) {

        Socket pullSocket;
        ObjectOutputStream objectOutputStream;
        ObjectInputStream objectInputStream;
        ArrayList<VideoInformation> channelVideoList = null;

        try {

            System.out.println(publisherAddress.toString());
            //Make connection with client
            pullSocket = new Socket();
            pullSocket.connect(publisherAddress, 3000);
            objectOutputStream = new ObjectOutputStream(pullSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(pullSocket.getInputStream());

            //Give option code
            objectOutputStream.writeObject(1);
            objectOutputStream.flush();

            //Give operation
            objectOutputStream.writeObject("CHANNEL");
            objectOutputStream.flush();

            //Store channel videos
            channelVideoList = (ArrayList<VideoInformation>) objectInputStream.readObject();

            //Close connections
            objectInputStream.close();
            objectOutputStream.close();
            pullSocket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return channelVideoList;
    }

    public ArrayList<byte[]> pullVideo(ChannelKey channelKey, SocketAddress publisherAddress){

        ArrayList<byte[]> chunks = new ArrayList<>();

        Socket pullSocket;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        try {

            //Make connection with publisher
            pullSocket = new Socket();
            pullSocket.connect(publisherAddress);
            objectInputStream = new ObjectInputStream(pullSocket.getInputStream());
            objectOutputStream = new ObjectOutputStream(pullSocket.getOutputStream());

            //Send option
            objectOutputStream.writeObject(2);
            objectOutputStream.flush();

            //Send channelKey
            objectOutputStream.writeObject(channelKey);
            objectOutputStream.flush();

            //Check if video exists
            boolean exists = (boolean) objectInputStream.readObject();
            if (!exists) {
                return chunks;
            }

            int size = (int) objectInputStream.readObject();
            byte[] chunk;

            for (int i = 0;i < size;i++){
                chunk = new byte[4096];
                chunk = objectInputStream.readAllBytes();
                chunks.add(chunk);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return chunks;
    }

    /** Thread-safe update of hashtag video list. */
    public synchronized void updateHashtagVideoList(ArrayList<VideoInformation> list) {
        hashtagVideoList.addAll(list);
    }


    class PullThread extends Thread {

        private final String hashtag;
        public SocketAddress address;

        /**
         * Constructor
         */
        public PullThread(String hashtag, SocketAddress address) {
            this.hashtag = hashtag;
            this.address = address;

        }

        /**
         * Run thread
         */
        @Override
        public void run() {

            try {

                //Make connection with client
                Socket pullSocket = new Socket();
                pullSocket.connect(address);
                ObjectInputStream objectInputStream = new ObjectInputStream(pullSocket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(pullSocket.getOutputStream());

                //Give option code
                objectOutputStream.writeObject(1);
                objectOutputStream.flush();

                //Give operation
                objectOutputStream.writeObject(hashtag);
                objectOutputStream.flush();

                //Receive video List
                ArrayList<VideoInformation> channelVideoList = (ArrayList<VideoInformation>) objectInputStream.readObject();

                //Concatenate with larger list
                //PROBLEM : LOCAL VARIABLES ARE THREAD SAFE, SO I CANNOT ACCESS THEM INSIDE THREAD
                //(UNLESS THEY ARE FINAL, IN WHICH CASE I CANNOT UPDATE THEM) !!
                updateHashtagVideoList(channelVideoList);

                //Latch countdown
                latch.countDown();


            } catch (IOException | ClassNotFoundException ioException) {
                ioException.printStackTrace();
            }

        }
    }
}