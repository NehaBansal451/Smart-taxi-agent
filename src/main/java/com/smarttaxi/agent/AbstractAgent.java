package com.smarttaxi.agent;

import java.util.Random;

/** Common parts of all agents: a name and a source of randomness. */
public abstract class AbstractAgent implements Agent {

    private final String name;
    protected final Random random;

    protected AbstractAgent(String name, Random random) {
        this.name = name;
        this.random = random;
    }

    @Override
    public String getName() {
        return name;
    }
}
