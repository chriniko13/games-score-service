package com.xyz.platform.games.score.service.property;

import com.xyz.platform.games.score.service.error.ProcessingError;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public enum ServiceSettings {

    INSTANCE;

    private final Properties properties;

    ServiceSettings() {
        try {
            properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            throw new ProcessingError("could not load service settings", e);
        }
    }

    public int getIntProperty(String name) {
        return Optional
                .ofNullable(properties.getProperty(name))
                .map(Integer::parseInt)
                .orElseThrow(()-> new ProcessingError("could not fetch property with name: " + name));
    }

    public String getStringProperty(String name) {
        return Optional
                .ofNullable(properties.getProperty(name))
                .orElseThrow(()-> new ProcessingError("could not fetch property with name: " + name));
    }
}
