package com.xyz.platform.games.score.service.service;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class LevelScoreServiceInMemoryImplTest {

    private LevelScoreService levelScoreService;

    @Test
    public void store() {

        // given
        levelScoreService = new LevelScoreServiceInMemoryImpl();

        int levelId = 100;

        int userId1 = 123;
        int userId2 = 133;

        int scoreUserId1 = 50;
        int scoreUserId2 = 327;

        String sessionKeyUserId1 = "1ABCD23";
        String sessionKeyUserId2 = "1ZBCD23";

        // when
        levelScoreService.store(levelId, userId1, scoreUserId1, sessionKeyUserId1);

        scoreUserId1 = scoreUserId1 + 10;
        levelScoreService.store(levelId, userId1, scoreUserId1, sessionKeyUserId1);

        levelScoreService.store(levelId, userId2, scoreUserId2, sessionKeyUserId2);

        // then
        List<String> highScores = levelScoreService.getHighScores(levelId, 10);
        assertEquals(Arrays.asList("133=327", "123=60"), highScores);
    }

    @Test
    public void getHighScores() {

        // given
        SecureRandom random = new SecureRandom();
        int levelId = 789;
        int highScore = Integer.MAX_VALUE;

        for (String calcStrategy : new String[]{"level-scores-by-user-id-nested-strategy", "level-scores-flat-strategy"}) {

            System.out.println("will use strategy: " + calcStrategy);
            levelScoreService = new LevelScoreServiceInMemoryImpl(calcStrategy);

            // when
            List<String> result = levelScoreService.getHighScores(levelId, 100);

            // then
            assertEquals(Collections.emptyList(), result);

            // when
            List<Integer> userIds = IntStream
                    .range(100, 200)
                    .boxed()
                    .collect(Collectors.toList());

            Map<Integer, List<Integer>> levelScoresByUserId = userIds
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Function.identity(),
                                    id -> {
                                        List<Integer> scores = IntStream.range(1, 50)
                                                .boxed()
                                                .map(idx -> random.nextInt() & Integer.MAX_VALUE)
                                                .collect(Collectors.toList());

                                        scores.add(highScore - id);
                                        return scores;
                                    }
                            )
                    );

            levelScoresByUserId.forEach((userId, scores) -> {
                scores.forEach(score -> levelScoreService.store(levelId, userId, score, random.nextLong() + ""));
            });


            List<String> highScores = levelScoreService.getHighScores(levelId, 1000);

            // then
            assertEquals(100, highScores.size());
            highScores.forEach(score -> {

                int userId = Integer.parseInt(score.split("=")[0]);
                int userScore = Integer.parseInt(score.split("=")[1]);

                assertEquals(Integer.MAX_VALUE - userId, userScore);
            });


            // when (limit 15)
            highScores = levelScoreService.getHighScores(levelId, 15);

            // then
            assertEquals(15, highScores.size());
            highScores.forEach(score -> {

                int userId = Integer.parseInt(score.split("=")[0]);
                int userScore = Integer.parseInt(score.split("=")[1]);

                assertEquals(Integer.MAX_VALUE - userId, userScore);
            });
        }

    }
}