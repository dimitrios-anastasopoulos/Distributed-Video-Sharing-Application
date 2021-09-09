# Distributed Video Sharing Application

An android application that enables its users to share videos on the platform using hashtags, follow friends or hashtags, while working in a distributed way, operating with multiple back-end servers.

This project consists of 2 sub-projects, the **Event Delivery System** and the **Android Application**. In the **Event Delivery System** sub-project the streaming functionality is implemented and in the **Android Application** sub-project the android platform that uses this functionality is implemented.

## Event Delivery System

The **Event Delivery System** model uses a **"push"** and a **"pull"** function in order to enable data transmission.

* push(topic,value) -> [broker]
* pull(topic,[broker]) -> [topic,value]

For the streaming functionality to work 3 basic components have been used in the project.

* Publishers: This component is responsible for the storage, searching and streaming of videos. Each component of this kind is responsible for the videos of one specific user (channel). Publishers **"push"** data to Brokers.
* Brokers: These server nodes are each responsible for a range of keys (topics), in this case channels or hashtags. Brokers send **"pull"** requests to the Publishers and **"push"** data to Consumers.
* Consumers: This component is responsible for receiving the data. It is practically part of the android application and must be able to play the recompose the video and play in on the device. Consumers send **"pull"** requests to the Brokers.

## Android Application

In this sub-project the layout for the various application screens is created and the previous **Event Delivery System** has been modified in order to work properly with android.

## Running Instructions

In order to run the application both sub-projects are needed. One AddressKeeper and 3 BrokerImpl are required from the **Event Delivery System** are required. Careful to run the different BrokerImpl with different port numbers. Furthermore, 2 AppNodeImpl are required for running the application. Those can be both from the **Event Delivery System**, one from the **Event Delivery System** and one from the **Android Application** or both from the **Android Application**. Obviously in the android application the runnable activity is MainActivity which will start the AppNodeImpl on the android device. In order to run both AppNodeImpl from the **Android Application** 2 seperate emulators are needed.

For the successful connection between Brokers and the android emulator a port redirection is needed. In the terminal of your Android Studio type the following commands:
* telnet localhost 5554
* auth [auth_token]
* redir add tcp:5529:4960
