package com.xyz.platform.games.score.service.infra;

import java.io.IOException;
import java.io.OutputStream;

public class StringOutputStream  extends OutputStream {

    private StringBuilder data = new StringBuilder();

    public void write(int b) throws IOException {
        this.data.append((char) b );
    }

    public String getString() {
        return data.toString();
    }
}
