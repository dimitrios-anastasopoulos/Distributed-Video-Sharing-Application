package com.example.uni_tok;

import java.net.UnknownHostException;


interface Node {

    public void initialize(int port) throws UnknownHostException;

    public void connect();

    public void disconnect();

}