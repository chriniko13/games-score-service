package com.xyz.platform.games.score.service.it;

import com.xyz.platform.games.score.service.property.ServiceSettings;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GamesScoreServiceClient {

    //private static final int TIMEOUT = 2 * 60_000; // Note: for debugging.
    private static final int TIMEOUT = 15_000;

    @Setter
    private String sessionKey;

    @Getter
    private final int userId;

    private final String serviceUrl;

    public GamesScoreServiceClient(int userId) {
        this.userId = userId;
        int serverPort = ServiceSettings.INSTANCE.getIntProperty("http-server.port");
        serviceUrl = "http://localhost:" + serverPort + "/";
    }

    public GamesScoreServiceClientResponse login() {
        String loginUrl = serviceUrl + userId + "/login";

        return communicationTemplate(loginUrl, con -> {
            con.setRequestMethod("GET");
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);

            int responseCode = con.getResponseCode();
            String responsePayload = readResponse(con);
            con.disconnect();

            return new GamesScoreServiceClientResponse(responseCode, responsePayload);
        });
    }

    public GamesScoreServiceClientResponse submitScore(int levelId, int score) {
        if (sessionKey == null) {
            throw new IllegalStateException("you should call login first, " +
                    "store session key and then submit score.");
        }

        String submitScoreUrl = serviceUrl + levelId + "/score?sessionkey=" + sessionKey;

        return communicationTemplate(submitScoreUrl, con -> {

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);

            // send score as a post's payload.
            try (OutputStream os = con.getOutputStream();
                 OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);) {
                osw.write(String.valueOf(score));
                osw.flush();
                con.connect();
            }

            String responsePayload = readResponse(con);
            int responseCode = con.getResponseCode();
            con.disconnect();

            return new GamesScoreServiceClientResponse(responseCode, responsePayload);
        });
    }


    public GamesScoreServiceClientResponse getHighscoreList(int levelId) {
        String getHighscoreListUrl = serviceUrl + levelId + "/highscorelist";

        return communicationTemplate(getHighscoreListUrl, con -> {
            con.setRequestMethod("GET");
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);

            int responseCode = con.getResponseCode();
            String responsePayload = readResponse(con);
            con.disconnect();

            return new GamesScoreServiceClientResponse(responseCode, responsePayload);
        });

    }

    private GamesScoreServiceClientResponse communicationTemplate(String resource,
                                                                  ThrowingFunction<HttpURLConnection, GamesScoreServiceClientResponse, Exception> f) {
        try {
            URL url = new URL(resource);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            return f.apply(con);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String readResponse(HttpURLConnection con) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            return content.toString();
        }
    }
}
