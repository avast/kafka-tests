package com.avast.kafkatests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class AbstractComponent implements RunnableComponent {
    private static final String COMPONENT_NAME_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int COMPONENT_NAME_LENGTH = 8;
    protected static final Random random = new Random();

    protected final String instanceName;
    protected final Logger logger;

    public AbstractComponent() {
        this.instanceName = randomComponentName();
        this.logger = LoggerFactory.getLogger(this.getClass().getName() + "." + instanceName);
    }

    private static String randomComponentName() {
        char[] data = new char[COMPONENT_NAME_LENGTH];

        for (int i = 0; i < COMPONENT_NAME_LENGTH; ++i) {
            data[i] = COMPONENT_NAME_CHARACTERS.charAt(random.nextInt(COMPONENT_NAME_CHARACTERS.length()));
        }

        return String.valueOf(data);
    }
}
