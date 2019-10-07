package com.xyz.platform.games.score.service.it;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GamesScoreServiceClientResponse {

    private final int responseCode;
    private final String payload;
}
