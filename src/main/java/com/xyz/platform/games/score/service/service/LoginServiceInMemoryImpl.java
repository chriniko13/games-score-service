package com.xyz.platform.games.score.service.service;

import com.xyz.platform.games.score.service.property.ServiceSettings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class LoginServiceInMemoryImpl implements LoginService {

    private final ConcurrentHashMap<Integer, SessionKey> sessionKeysByUserId;
    private final ConcurrentHashMap<String, Integer> sessionKeys;

    private final SecureRandom random;
    private final long sessionKeyTimeoutInSeconds;

    public LoginServiceInMemoryImpl() {
        this.sessionKeysByUserId = new ConcurrentHashMap<>();
        this.sessionKeys = new ConcurrentHashMap<>();

        this.random = new SecureRandom();

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("ses-key-invalidator");
            return t;
        });
        scheduledExecutorService.scheduleWithFixedDelay(
                this::sessionKeysTimeValidityCheck, 2, 2, TimeUnit.SECONDS
        );

        Runtime.getRuntime().addShutdownHook(new Thread(scheduledExecutorService::shutdown));
        int sessionKeyTimeoutInMinutes = ServiceSettings.INSTANCE.getIntProperty("session-key.timeout.minutes");
        this.sessionKeyTimeoutInSeconds = TimeUnit.SECONDS.convert(sessionKeyTimeoutInMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String get(int userId) {
        SessionKey sessionKey = sessionKeysByUserId.computeIfAbsent(
                userId,
                _userId -> {
                    String key = getKey();
                    sessionKeys.put(key, _userId);
                    return new SessionKey(key, Instant.now());
                }
        );
        return sessionKey.getKey();
    }

    @Override
    public int get(String sessionKey) {
        return sessionKeys.get(sessionKey);
    }

    @Override
    public boolean isValidSession(String s) {
        return sessionKeys.containsKey(s);
    }

    private String getKey() {
        return Integer.toUnsignedString(random.nextInt(), Character.MAX_RADIX).toUpperCase();
    }

    private void sessionKeysTimeValidityCheck() {
        Instant now = Instant.now();

        Iterator<Map.Entry<Integer, SessionKey>> iterator = sessionKeysByUserId.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<Integer, SessionKey> entry = iterator.next();
            Integer userId = entry.getKey();
            SessionKey sessionKey = entry.getValue();
            Instant created = sessionKey.getCreated();

            long secondsPassed = Duration.between(created, now).abs().getSeconds();
            if (secondsPassed >= sessionKeyTimeoutInSeconds) {
                log.debug(
                        "will remove session key: {} for user id: {}, seconds passed: {}",
                        sessionKey.getKey(), userId, secondsPassed
                );
                iterator.remove();
                sessionKeys.remove(sessionKey.getKey());
            }
        }
    }

    // ---

    @Data
    @AllArgsConstructor
    private static class SessionKey {
        private String key;
        private Instant created;
    }
}
