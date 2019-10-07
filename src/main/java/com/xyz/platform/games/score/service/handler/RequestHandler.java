package com.xyz.platform.games.score.service.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.xyz.platform.games.score.service.core.Either;
import com.xyz.platform.games.score.service.core.InputStreamExtensions;
import com.xyz.platform.games.score.service.error.ProcessingError;
import com.xyz.platform.games.score.service.property.ServiceSettings;
import com.xyz.platform.games.score.service.service.LevelScoreService;
import com.xyz.platform.games.score.service.service.LoginService;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Log4j2
public class RequestHandler {

    private final Pattern loginPathRegex;
    private final Pattern submitUserScoreToALevelRegex;
    private final Pattern getHighscoreListForALevelRegex;

    private final LoginService loginService;
    private final LevelScoreService levelScoreService;

    private final int highscoreListLimitSize;


    public RequestHandler(LoginService loginService, LevelScoreService levelScoreService) {
        this.loginService = loginService;
        this.levelScoreService = levelScoreService;

        this.loginPathRegex = Pattern.compile("/\\d+/login$");
        this.submitUserScoreToALevelRegex = Pattern.compile("/\\d+/score\\?sessionkey=[a-zA-Z0-9]+$");
        this.getHighscoreListForALevelRegex = Pattern.compile("/\\d+/highscorelist");

        this.highscoreListLimitSize = ServiceSettings.INSTANCE.getIntProperty("high-score-list.limit-size");
    }

    public HttpHandler get() {
        return httpExchange -> {
            String requestMethod = httpExchange.getRequestMethod();
            String requestURI = httpExchange.getRequestURI().toString();

            try {
                if (requestMethod.equalsIgnoreCase("GET")
                        && loginPathRegex.matcher(requestURI).matches()) {

                    loginOperation(httpExchange, requestURI);

                } else if (requestMethod.equalsIgnoreCase("POST")
                        && submitUserScoreToALevelRegex.matcher(requestURI).matches()) {

                    storeUserLevelScore(httpExchange, requestURI);

                } else if (requestMethod.equalsIgnoreCase("GET")
                        && getHighscoreListForALevelRegex.matcher(requestURI).matches()) {

                    getLevelHighscoreList(httpExchange, requestURI);

                } else { // Fallback
                    reply(httpExchange, "Bad request", 400);
                }
            } catch (Exception unknownError) {
                String msg = "internal error occurred: " + unknownError.getMessage();
                log.error(msg, unknownError);
                reply(httpExchange, "Internal Server Error Occurred", 500);
            }
        };
    }

    private void loginOperation(HttpExchange httpExchange, String requestURI) {
        String userIdAsString = requestURI.split("/")[1];
        int userId = Integer.parseInt(userIdAsString);
        String sessionId = loginService.get(userId);
        reply(httpExchange, sessionId, 200);
    }

    private void storeUserLevelScore(HttpExchange httpExchange, String requestURI) {
        String requestBody = InputStreamExtensions.convert(httpExchange.getRequestBody());

        convertToInt(requestBody)
                .map(score -> {
                    int levelId = Integer.parseInt(requestURI.split("/")[1]);
                    String query = httpExchange.getRequestURI().getQuery(); // Note: this will always have only one query param entry.
                    String sessionKey = query.split("=")[1];

                    //log.debug("score: " + score + ", sessionKey: " + sessionKey + ", levelId: " + levelId);

                    if (!loginService.isValidSession(sessionKey)) {
                        reply(httpExchange, "provided session key is invalid", 401);
                        return null;
                    }

                    int userId = loginService.get(sessionKey);
                    levelScoreService.store(levelId, userId, score, sessionKey);
                    replyOk(httpExchange);
                    return null;
                })
                .onLeftExecute(httpExchangeConsumer -> {
                    httpExchangeConsumer.accept(httpExchange);
                    return null;
                });
    }

    private void getLevelHighscoreList(HttpExchange httpExchange, String requestURI) {
        int levelId = Integer.parseInt(requestURI.split("/")[1]);

        List<String> highScores = levelScoreService.getHighScores(levelId, highscoreListLimitSize);
        String result = String.join(",", highScores);
        reply(httpExchange, result, 200);
    }

    private Either<Consumer<HttpExchange>, Integer> convertToInt(String requestBody) {
        try {
            return Either.right(Integer.parseInt(requestBody));
        } catch (NumberFormatException e) {
            return Either.left(exchange -> {
                String msg = "invalid score provided";
                log.error(msg, e);
                reply(exchange, msg, 400);
            });
        }
    }

    private void reply(HttpExchange exchange, String response, int responseCode) {
        try {
            exchange.sendResponseHeaders(responseCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e) {
            String msg = "could not reply, message: " + e.getMessage();
            log.error(msg, e);
            throw new ProcessingError(msg, e);
        }
    }

    private void replyOk(HttpExchange exchange) {
        reply(exchange, "", 200);
    }

}
