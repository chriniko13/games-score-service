package com.xyz.platform.games.score.service.handler;

import com.sun.net.httpserver.HttpHandler;
import com.xyz.platform.games.score.service.infra.TestHttpExchange;
import com.xyz.platform.games.score.service.service.LevelScoreService;
import com.xyz.platform.games.score.service.service.LoginService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;


@RunWith(MockitoJUnitRunner.class)
public class RequestHandlerTest {

    private RequestHandler requestHandler;

    @Mock
    private LoginService loginService;

    @Mock
    private LevelScoreService levelScoreService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        requestHandler = new RequestHandler(loginService, levelScoreService);
    }

    @Test
    public void handle_login() throws Exception {

        // given
        int userId = 1234;
        HttpHandler handler = requestHandler.get();
        Mockito.when(loginService.get(userId)).thenReturn("MOCKSESSIONID");

        // when
        TestHttpExchange httpExchange = TestHttpExchange.produce("/" + userId + "/login", "GET");
        handler.handle(httpExchange);

        // then
        String payload = httpExchange.getStringOutputStream().getString();
        assertEquals("MOCKSESSIONID", payload);
        assertEquals(200, httpExchange.getRespCode());
        Mockito.verify(loginService).get(userId);

    }

    @Test
    public void handle_submit_user_score_level_operation() throws Exception {

        // given
        int userId = 1234;
        String sessionKey = "12AB34CD";
        String score = "1711";

        HttpHandler handler = requestHandler.get();

        Mockito.when(loginService.isValidSession(sessionKey)).thenReturn(true);
        Mockito.when(loginService.get(sessionKey)).thenReturn(userId);


        // when
        TestHttpExchange httpExchange
                = TestHttpExchange.produce("/"+userId+"/score?sessionkey="+sessionKey, "POST", score);
        handler.handle(httpExchange);


        // then
        String payload = httpExchange.getStringOutputStream().getString();
        assertEquals("", payload);
        assertEquals(200, httpExchange.getRespCode());

        Mockito.verify(loginService).isValidSession(sessionKey);
        Mockito.verify(loginService).get(sessionKey);
    }

    @Test
    public void handle_level_high_score_list() throws Exception {

        // given
        int levelId = 10;
        HttpHandler handler = requestHandler.get();

        List<String> scores = Arrays.asList("1=2", "3=4", "5=6");

        Mockito.when(levelScoreService.getHighScores(any(Integer.class), any(Integer.class)))
                .thenReturn(scores);

        // when
        TestHttpExchange httpExchange = TestHttpExchange.produce("/" + levelId + "/highscorelist", "GET");
        handler.handle(httpExchange);

        // then
        String payload = httpExchange.getStringOutputStream().getString();
        assertEquals("1=2,3=4,5=6", payload);
        assertEquals(200, httpExchange.getRespCode());

    }


    @Test
    public void handle_bad_request() throws Exception {

        // given
        HttpHandler handler = requestHandler.get();

        // when
        TestHttpExchange httpExchange = TestHttpExchange.produce("/games/offers/1", "GET");
        handler.handle(httpExchange);

        // then
        String payload = httpExchange.getStringOutputStream().getString();
        assertEquals("Bad request", payload);
        assertEquals(400, httpExchange.getRespCode());

    }

    @Test
    public void handle_unknown_error() throws Exception {

        // given
        int userId = 1234;
        HttpHandler handler = requestHandler.get();
        Mockito.when(loginService.get(userId)).thenThrow(new IllegalStateException("ooops something happened"));

        // when
        TestHttpExchange httpExchange = TestHttpExchange.produce("/" + userId + "/login", "GET");
        handler.handle(httpExchange);

        // then
        String payload = httpExchange.getStringOutputStream().getString();
        assertEquals("Internal Server Error Occurred", payload);
        assertEquals(500, httpExchange.getRespCode());

        Mockito.verify(loginService).get(userId);
    }
}