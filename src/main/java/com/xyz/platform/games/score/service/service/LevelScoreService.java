package com.xyz.platform.games.score.service.service;

import java.util.List;

public interface LevelScoreService {

    void store(int levelId, int userId, int score, String sessionKey /* Note: for auditing purposes*/);

    List<String> getHighScores(int levelId, int limit);

}
