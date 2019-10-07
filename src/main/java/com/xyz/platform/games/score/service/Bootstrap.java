package com.xyz.platform.games.score.service;

import com.xyz.platform.games.score.service.handler.RequestHandler;
import com.xyz.platform.games.score.service.resource.HttpEnvironment;
import com.xyz.platform.games.score.service.service.LevelScoreServiceInMemoryImpl;
import com.xyz.platform.games.score.service.service.LoginServiceInMemoryImpl;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Bootstrap {

    public void run() {

        // Note: do the wiring.
        RequestHandler requestHandler = new RequestHandler(
                new LoginServiceInMemoryImpl(),
                new LevelScoreServiceInMemoryImpl()
        );

        new HttpEnvironment(requestHandler).initialize();
        log.info("http environment initialized...");
    }

}
