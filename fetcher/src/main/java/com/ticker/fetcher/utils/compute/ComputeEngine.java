package com.ticker.fetcher.utils.compute;


import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.TimeUtil;
import com.ticker.fetcher.utils.compute.study.StudyConstants.Study;
import com.ticker.fetcher.utils.compute.study.model.StudyModel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A class to store and manage all the properties and values
 * of computations that are required by the thread.
 */
@Slf4j
public class ComputeEngine {
    private final FetcherThread thread;
    private final LinkedList<ComputeData> values = new LinkedList<>();
    private final Map<Study, StudyModel> studies = new EnumMap<>(Study.class);
    private int windowSize = 1;

    public ComputeEngine(FetcherThread thread) {
        this.thread = thread;
        values.add(new ComputeData(TimeUtil.getMinuteTimestamp(System.currentTimeMillis()), thread.getC()));
        addAllStudies();
    }

    private void addAllStudies() {
        Arrays.stream(Study.values()).forEach(this::addStudy);
    }

    public void addStudy(Study study) {
        try {
            if (!studies.containsKey(study)) {
                StudyModel model = study.getModel().getConstructor(FetcherThread.class).newInstance(thread);
                studies.putIfAbsent(study, model);
                updateMaxWindowSize();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateLast(double value) {
        synchronized (values) {
            values.getLast().value = value;
            adjustValues();
        }
    }

    public void pushValue(ComputeData value) {
        synchronized (values) {
            values.addLast(value);
            adjustValues();
        }
    }

    public void pushValues(List<ComputeData> valuesToAdd) {
        synchronized (values) {
            values.addAll(valuesToAdd);
            adjustValues();
        }
    }

    /**
     * Trim array to fit the window size if required
     */
    private void adjustValues() {
        while (values.size() > windowSize) {
            values.removeFirst();
        }
        log.info("{} : Compute engine values {}", thread.getThreadName(),
                values.stream().map(computeData -> String.valueOf(computeData.value)).collect(Collectors.joining(", ")));
        computeAllValues();
    }

    private void computeAllValues() {
        log.info("{} : Computing all values", thread.getThreadName());
        studies.values().forEach(new Consumer<StudyModel>() {
            @Override
            public void accept(StudyModel studyModel) {
                studyModel.compute(values);
            }
        });
    }

    /**
     * Sets the max size of window required based on active studies
     */
    public void updateMaxWindowSize() {
        final int[] maxWindowSize = {1};
        studies.keySet().forEach(study -> {
            if (study.getWindowSize() > maxWindowSize[0]) {
                maxWindowSize[0] = study.getWindowSize();
            }
        });
        windowSize = maxWindowSize[0];
    }

    //TODO: Remove second cutoff implementation if not required
    public void secondCutoff() {
    }

    public void minuteCutoff() {
        pushValue(new ComputeData(TimeUtil.getMinuteTimestamp(System.currentTimeMillis()), thread.getC()));
    }

    public static class ComputeData {
        private final long minuteTimestamp;
        private double value;

        public ComputeData(long minuteTimestamp, double value) {
            this.minuteTimestamp = minuteTimestamp;
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }
}
