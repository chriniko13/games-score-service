package com.xyz.platform.games.score.service.resource;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.xyz.platform.games.score.service.error.ProcessingError;
import com.xyz.platform.games.score.service.handler.RequestHandler;
import com.xyz.platform.games.score.service.property.ServiceSettings;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class HttpEnvironment {

    private final int port;
    private final RequestHandler requestHandler;

    public HttpEnvironment(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.port = ServiceSettings.INSTANCE.getIntProperty("http-server.port");
    }

    public void initialize() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(
                    new ThreadPoolExecutor(70, 300,
                            60, TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(2500),
                            new ThreadFactory() {
                                private final AtomicInteger threadNumber = new AtomicInteger(1);

                                @Override
                                public Thread newThread(Runnable r) {
                                    Thread t = new Thread(r);
                                    t.setName("http-worker-" + threadNumber.getAndIncrement());
                                    return t;
                                }
                            },
                            new ThreadPoolExecutor.AbortPolicy()
                    )
            );
            HttpContext context = server.createContext("/");
            context.setHandler(requestHandler.get());
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("shutting down http environment...");
                server.stop(1);
            }));

        } catch (IOException e) {
            String msg = "could not initialize http environment, message: " + e.getMessage();
            log.error(msg, e);
            throw new ProcessingError(msg, e);
        }
    }

}
