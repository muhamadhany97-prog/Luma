package com.beeiptv.media.net;

public final class Channel {
    public final String id;
    public final String name;
    public final String number;
    public final String cmd;

    public Channel(String id, String name, String number, String cmd) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.cmd = cmd;
    }
}
