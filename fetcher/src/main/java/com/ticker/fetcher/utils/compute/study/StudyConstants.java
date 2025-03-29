package com.ticker.fetcher.utils.compute.study;

import com.ticker.fetcher.utils.compute.study.model.BollingerBands;
import com.ticker.fetcher.utils.compute.study.model.RelativeStrengthIndex;
import com.ticker.fetcher.utils.compute.study.model.StudyModel;
import com.ticker.fetcher.utils.compute.study.model.TripleExponentialMovingAverage;
import lombok.Getter;

public class StudyConstants {
    public enum Study {
        BB("Bollinger Bands", 20, BollingerBands.class),
        RSI("Relative Strength Index (RSI)", 14, RelativeStrengthIndex.class),
        TEMA("Triple Exponential Moving Average", 9, TripleExponentialMovingAverage.class);

        @Getter
        private final String name;
        @Getter
        private final int windowSize;
        @Getter
        private final Class<? extends StudyModel> model;

        Study(String name, int windowSize, Class<? extends StudyModel> model) {
            this.name = name;
            this.windowSize = windowSize;
            this.model = model;
        }
    }

    public enum ComputeScale {
        SECONDS, MINUTES
    }
}
