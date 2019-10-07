package com.xyz.platform.games.score.service.service;

public interface LoginService {

    String get(int userId);

    int get(String sessionKey);

    boolean isValidSession(String sessionKey);

}
