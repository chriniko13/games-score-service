package com.xyz.platform.games.score.service.it;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiClientsFullPipelineIT extends SpecificationIT {

    @Test
    public void specification_works_as_expected() throws Exception {

        // when
        GamesScoreServiceClientResponse response = new GamesScoreServiceClient(1).getHighscoreList(14);

        // then
        Assert.assertEquals(200, response.getResponseCode());
        Assert.assertEquals("", response.getPayload());

        // given
        SecureRandom random = new SecureRandom();

        ExecutorService workersPool = Executors.newFixedThreadPool(300);

        int totalClients = 400;
        int levelId = 17;

        CountDownLatch latch = new CountDownLatch(totalClients);

        int scoresSubmittedPerClient = 20;

        List<GamesScoreServiceClient> clients = IntStream
                .rangeClosed(1, totalClients)
                .boxed()
                .map(GamesScoreServiceClient::new)
                .collect(Collectors.toList());


        // when
        for (GamesScoreServiceClient client : clients) {

            Runnable clientJourney = () -> {
                try {

                    GamesScoreServiceClientResponse loginResponse = client.login();
                    Assert.assertEquals(200, loginResponse.getResponseCode());
                    client.setSessionKey(loginResponse.getPayload());

                    for (int i = 0; i < scoresSubmittedPerClient; i++) {
                        int scoreToSubmit = random.nextInt() & Integer.MAX_VALUE;
                        client.submitScore(levelId, scoreToSubmit);
                    }
                    client.submitScore(levelId, Integer.MAX_VALUE - client.getUserId());

                } catch (Exception e) {
                    System.err.println("client error occurred: " + e.getMessage());
                    Assert.fail();
                } finally {
                    latch.countDown();
                }
            };

            workersPool.execute(clientJourney);
        }

        latch.await();


        // then
        GamesScoreServiceClientResponse highscoreListResponse = clients.get(0).getHighscoreList(levelId);

        Assert.assertEquals(200, highscoreListResponse.getResponseCode());

        String highscoreList = highscoreListResponse.getPayload();

        String[] highscores = highscoreList.split(",");
        Assert.assertEquals(15, highscores.length);

        System.out.println("highscores: " + Arrays.toString(highscores));

        for (String record : highscores) {

            String[] splittedInfo = record.split("=");
            int userId = Integer.parseInt(splittedInfo[0]);
            int score = Integer.parseInt(splittedInfo[1]);

            Assert.assertEquals(Integer.MAX_VALUE - userId, score);
        }

        // clean
        workersPool.shutdown();

    }

}
