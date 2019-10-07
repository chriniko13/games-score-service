package com.xyz.platform.games.score.service.infra;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class TestHttpExchange extends HttpExchange {

    @Getter
    private final StringOutputStream stringOutputStream;

    @Getter
    private final String uri;

    private final String requestMethod;

    private final String requestBody;

    @Getter
    private int respCode;

    public TestHttpExchange(String uri, String requestMethod) {
        this(uri, requestMethod, null);
    }

    public TestHttpExchange(String uri, String requestMethod, String requestBody) {
        this.stringOutputStream = new StringOutputStream();
        this.uri = uri;
        this.requestMethod = requestMethod;
        this.requestBody = requestBody;
    }

    public static TestHttpExchange produce(String uri, String requestMethod) {
        return new TestHttpExchange(uri, requestMethod);
    }

    public static TestHttpExchange produce(String uri, String requestMethod, String requestBody) {
        return new TestHttpExchange(uri, requestMethod, requestBody);
    }

    public Headers getRequestHeaders() {
        return null;
    }

    @Override
    public Headers getResponseHeaders() {
        return null;
    }

    @Override
    public URI getRequestURI() {
        return URI.create(uri);
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public InputStream getRequestBody() {
        return requestBody != null ? new ByteArrayInputStream(requestBody.getBytes()) : null;
    }

    @Override
    public OutputStream getResponseBody() {
        return stringOutputStream;
    }

    @Override
    public void sendResponseHeaders(int code, long responseLength) throws IOException {
        this.respCode = code;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public int getResponseCode() {
        return 0;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {
    }

    @Override
    public void setStreams(InputStream inputStream, OutputStream outputStream) {
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }
}
