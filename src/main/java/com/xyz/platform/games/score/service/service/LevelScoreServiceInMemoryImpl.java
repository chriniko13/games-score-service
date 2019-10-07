package com.xyz.platform.games.score.service.service;

import com.xyz.platform.games.score.service.property.ServiceSettings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class LevelScoreServiceInMemoryImpl implements LevelScoreService {

    private static final String LEVEL_SCORES_BY_USER_ID_NESTED_STRATEGY = "level-scores-by-user-id-nested-strategy";
    private static final String LEVEL_SCORES_FLAT_STRATEGY = "level-scores-flat-strategy";

    private final ConcurrentHashMap<Integer /*levelId*/, ConcurrentHashMap<Integer /*userId*/, ConcurrentSkipListSet<LevelScoreEntry>>> levelScoresByUserId;
    private final ConcurrentHashMap<Integer /*levelId*/, ConcurrentSkipListSet<LevelScoreEntry>> levelScores;

    private final HighScoreListCalculatorStrategy calculatorStrategy;

    public LevelScoreServiceInMemoryImpl() {
        levelScoresByUserId = new ConcurrentHashMap<>();
        levelScores = new ConcurrentHashMap<>();

        String highScoreListCalculatorStrategy = ServiceSettings.INSTANCE.getStringProperty("high-score-list.calculator-strategy");

        if (highScoreListCalculatorStrategy.equals(LEVEL_SCORES_BY_USER_ID_NESTED_STRATEGY)) {
            calculatorStrategy = new LevelScoresByUserIdHighScoreListCalculatorStrategy(levelScoresByUserId);
        } else if (highScoreListCalculatorStrategy.equals(LEVEL_SCORES_FLAT_STRATEGY)) {
            calculatorStrategy = new LevelScoresHighScoreListCalculatorStrategy(levelScores);
        } else {
            throw new IllegalStateException("not valid high score list calculator strategy selected");
        }
    }

    public LevelScoreServiceInMemoryImpl(String highScoreListCalculatorStrategy) {
        levelScoresByUserId = new ConcurrentHashMap<>();
        levelScores = new ConcurrentHashMap<>();

        if (highScoreListCalculatorStrategy.equals(LEVEL_SCORES_BY_USER_ID_NESTED_STRATEGY)) {
            calculatorStrategy = new LevelScoresByUserIdHighScoreListCalculatorStrategy(levelScoresByUserId);
        } else if (highScoreListCalculatorStrategy.equals(LEVEL_SCORES_FLAT_STRATEGY)) {
            calculatorStrategy = new LevelScoresHighScoreListCalculatorStrategy(levelScores);
        } else {
            throw new IllegalStateException("not valid high score list calculator strategy selected");
        }
    }

    @Override
    public void store(int levelId, int userId, int score, String sessionKey) {
        LevelScoreEntry levelScoreEntry = new LevelScoreEntry(userId, score, sessionKey, Instant.now());
        updateLevelScoresByUserId(levelId, userId, levelScoreEntry);
        updateLevelScores(levelId, levelScoreEntry);
    }

    @Override
    public List<String> getHighScores(int levelId, int limit) {
        long startTime = System.nanoTime();
        try {
            return calculatorStrategy.process(levelId, limit);
        } finally {
            long totalTime = System.nanoTime() - startTime;
            long totalTimeInMS = TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS);
            log.info("high score list cal time took(ms): {}, strategy name: {}", totalTimeInMS, calculatorStrategy.strategyName());
        }
    }

    private void updateLevelScoresByUserId(int levelId, int userId, LevelScoreEntry levelScoreEntry) {
        levelScoresByUserId.computeIfAbsent(levelId, __ -> new ConcurrentHashMap<>());

        ConcurrentHashMap<Integer, ConcurrentSkipListSet<LevelScoreEntry>> scoresByUserId = this.levelScoresByUserId.get(levelId);

        scoresByUserId.compute(userId, (__, levelScoreEntries) -> {
            if (levelScoreEntries == null) {
                levelScoreEntries = new ConcurrentSkipListSet<>(Comparator.comparing(LevelScoreEntry::getScore).reversed());
            }
            levelScoreEntries.add(levelScoreEntry);
            return levelScoreEntries;
        });
    }

    private void updateLevelScores(int levelId, LevelScoreEntry levelScoreEntry) {
        levelScores.computeIfAbsent(levelId,
                __ -> new ConcurrentSkipListSet<>(
                        Comparator.comparing(LevelScoreEntry::getScore).reversed()
                                .thenComparing(LevelScoreEntry::getUserId)
                                .thenComparing(LevelScoreEntry::getSessionKey)
                                .thenComparing(LevelScoreEntry::getSubmitted)
                )
        );
        levelScores.get(levelId).add(levelScoreEntry);
    }

    // ---

    @Data
    @AllArgsConstructor
    private static class LevelScoreEntry {
        private int userId;
        private int score;
        private String sessionKey;
        private Instant submitted;
    }

    interface HighScoreListCalculatorStrategy {
        List<String> process(int levelId, int limit);

        String strategyName();
    }

    private class LevelScoresByUserIdHighScoreListCalculatorStrategy implements HighScoreListCalculatorStrategy {

        private final ConcurrentHashMap<Integer /*levelId*/, ConcurrentHashMap<Integer /*userId*/, ConcurrentSkipListSet<LevelScoreEntry>>> levelScoresByUserId;

        private LevelScoresByUserIdHighScoreListCalculatorStrategy(ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ConcurrentSkipListSet<LevelScoreEntry>>> levelScoresByUserId) {
            this.levelScoresByUserId = levelScoresByUserId;
        }

        @Override
        public List<String> process(int levelId, int limit) {
            ConcurrentHashMap<Integer /*userId*/, ConcurrentSkipListSet<LevelScoreEntry>> levelScoresByUsers = this.levelScoresByUserId.get(levelId);
            if (levelScoresByUsers == null || levelScoresByUsers.isEmpty()) {
                return Collections.emptyList();
            }

            List<LevelScoreEntry> levelScoreEntries = new LinkedList<>();
            for (ConcurrentSkipListSet<LevelScoreEntry> userLevelScores : levelScoresByUsers.values()) {
                try {
                    LevelScoreEntry levelScoreEntry = userLevelScores.first();
                    if (levelScoreEntry != null) {
                        levelScoreEntries.add(levelScoreEntry);
                    }
                } catch (NoSuchElementException ignore) {
                }
            }

            levelScoreEntries.sort(
                    Comparator.comparing(LevelScoreEntry::getScore).reversed()
                            .thenComparing(LevelScoreEntry::getUserId)
            );

            if (levelScoreEntries.size() > limit) {
                levelScoreEntries = levelScoreEntries.subList(0, limit);
            }
            return levelScoreEntries
                    .stream()
                    .map(e -> e.getUserId() + "=" + e.getScore())
                    .collect(Collectors.toList());
        }

        @Override
        public String strategyName() {
            return LEVEL_SCORES_BY_USER_ID_NESTED_STRATEGY;
        }
    }

    private class LevelScoresHighScoreListCalculatorStrategy implements HighScoreListCalculatorStrategy {

        private final ConcurrentHashMap<Integer /*levelId*/, ConcurrentSkipListSet<LevelScoreEntry>> levelScores;

        public LevelScoresHighScoreListCalculatorStrategy(ConcurrentHashMap<Integer, ConcurrentSkipListSet<LevelScoreEntry>> levelScores) {
            this.levelScores = levelScores;
        }

        @Override
        public List<String> process(int levelId, int limit) {

            ConcurrentSkipListSet<LevelScoreEntry> levelScoreEntries = this.levelScores.get(levelId);
            if (levelScoreEntries == null || levelScoreEntries.isEmpty()) {
                return Collections.emptyList();
            }

            // Note: important to maintain order that skip list has already, in order to not perform sorting later.
            LinkedHashMap<Integer/*userId*/, LevelScoreEntry> resultEntries = new LinkedHashMap<>();

            Iterator<LevelScoreEntry> iterator = levelScoreEntries.iterator();
            while (iterator.hasNext()) {

                LevelScoreEntry levelScoreEntry = iterator.next();
                int userId = levelScoreEntry.getUserId();

                LevelScoreEntry alreadyExists = resultEntries.get(userId);

                if (alreadyExists == null) {
                    resultEntries.put(userId, levelScoreEntry);
                } else { // it means that we have already a record for this user id, so keep the highest score.
                    int asIsScore = alreadyExists.getScore();
                    int toBeScore = levelScoreEntry.getScore();

                    if (toBeScore >= asIsScore) {
                        resultEntries.put(userId, levelScoreEntry);
                    }
                }
            }

            return resultEntries
                    .values()
                    .stream()
                    .map(e -> e.getUserId() + "=" + e.getScore())
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public String strategyName() {
            return LEVEL_SCORES_FLAT_STRATEGY;
        }
    }

}
