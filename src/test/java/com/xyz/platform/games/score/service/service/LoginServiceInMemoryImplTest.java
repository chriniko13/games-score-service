package com.xyz.platform.games.score.service.service;

import com.xyz.platform.games.score.service.infra.Modifier;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LoginServiceInMemoryImplTest {

    private LoginService loginService;

    @Before
    public void setup() {
        loginService = new LoginServiceInMemoryImpl();
    }

    @Test
    public void getByUserId() {

        // given
        int userId = 123;

        // when
        String sessionKey = loginService.get(userId);

        // then
        assertNotNull(sessionKey);

        // when
        String sessionKey2 = loginService.get(userId);

        // then
        assertEquals(sessionKey, sessionKey2);
    }

    @Test
    public void getBySessionId() {

        // given
        int userId = 123;
        String sessionKey = loginService.get(userId);

        // when
        int result = loginService.get(sessionKey);

        // then
        assertEquals(userId, result);
    }

    @Test
    public void isValidSession() throws Exception {

        // given
        int userId = 123;
        String sessionKey = loginService.get(userId);

        // when
        boolean validSession = loginService.isValidSession(sessionKey);

        // then
        assertTrue(validSession);

        // when
        Field sessionKeyTimeoutInSecondsField = LoginServiceInMemoryImpl.class.getDeclaredField("sessionKeyTimeoutInSeconds");
        Modifier.changeLongField(sessionKeyTimeoutInSecondsField, loginService, 1);

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    // then
                    boolean result = loginService.isValidSession(sessionKey);
                    assertFalse(result);

                });
    }


}