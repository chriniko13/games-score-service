package com.xyz.platform.games.score.service.core;

import com.xyz.platform.games.score.service.error.ProcessingError;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Log4j2
public class InputStreamExtensions {

    private InputStreamExtensions() {
    }

    public static String convert(InputStream inputStream) {
        try {

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (IOException error) {
            String msg = "could not convert input stream to string, message: " + error.getMessage();
            log.error(msg, error);
            throw new ProcessingError(msg, error);
        }
    }

}
