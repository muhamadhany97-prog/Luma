package com.beeiptv.media.net;

public final class Channel {
    public final String id;
    public final String name;
    public final String number;
    public final String cmd;
    public final String genre;

    public Channel(String id, String name, String number, String cmd, String genre) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.cmd = cmd;
        this.genre = genre;
    }
}
