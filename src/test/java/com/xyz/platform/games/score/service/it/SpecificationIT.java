package com.xyz.platform.games.score.service.it;

import com.xyz.platform.games.score.service.Bootstrap;
import org.junit.BeforeClass;

public abstract class SpecificationIT {

    @BeforeClass
    public static void setup() {
        new Bootstrap().run();
    }

}
