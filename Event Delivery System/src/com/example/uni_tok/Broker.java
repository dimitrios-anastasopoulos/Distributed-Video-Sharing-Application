package com.example.uni_tok;

import java.util.HashMap;


interface Broker extends Node {

    public int calculateKeys(String id);

    public HashMap<ChannelKey, String> filterConsumers(HashMap<ChannelKey, String> videoList, String channelName);

}