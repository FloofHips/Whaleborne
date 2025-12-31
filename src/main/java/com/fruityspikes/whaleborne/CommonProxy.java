package com.fruityspikes.whaleborne;

import com.fruityspikes.whaleborne.server.data.HullbackDirtManager;

public class CommonProxy {
    private final HullbackDirtManager hullbackDirtManager = new HullbackDirtManager();

    public void init() {
    }

    public HullbackDirtManager getHullbackDirtManager() {
        return hullbackDirtManager;
    }
}
