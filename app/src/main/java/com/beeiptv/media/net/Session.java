package com.beeiptv.media.net;

import java.util.ArrayList;
import java.util.List;

public final class Session {
    public static Stalker stalker;
    public static String expiry = "";
    public static List<String[]> genres = new ArrayList<>();
    public static List<Channel> channels = new ArrayList<>();
    public static boolean loaded = false;

    private Session() {}
}
