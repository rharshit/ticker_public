package com.ticker.fetcher.utils.compute;


import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.TimeUtil;
import com.ticker.fetcher.utils.compute.study.StudyConstants.Study;
import com.ticker.fetcher.utils.compute.study.model.StudyModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A class to store and manage all the properties and values
 * of computations that are required by the thread.
 */
@Slf4j
public class ComputeEngine {
    private final FetcherThread thread;
    private final Executor computeExecutor = Executors.newWorkStealingPool();
    private final LinkedList<ComputeData> values = new LinkedList<>();
    private final Map<Study, StudyModel> studies = new EnumMap<>(Study.class);
    private int windowSize = 1;

    public ComputeEngine(FetcherThread thread) {
        this.thread = thread;
        values.add(new ComputeData(thread.getC(), System.currentTimeMillis()));
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

    public void updateLastPrice(double value) {
        updateLastPrice(value, System.currentTimeMillis());
    }

    public void updateLastPrice(double value, long timestamp) {
        long minuteTimestamp = TimeUtil.getMinuteTimestamp(timestamp);
        synchronized (values) {
            if (values.getLast().getMinuteTimestamp() == minuteTimestamp) {
                if (timestamp >= values.getLast().getLatestTimestamp()) {
                    values.getLast().setValue(value);
                    values.getLast().setLatestTimestamp(timestamp);
                }
            } else {
                values.addLast(new ComputeData(value, timestamp));
            }
        }
        adjustValues();
    }

    /**
     * Push a new value to the end of the list.
     * Use this for adding new data point, at the end of the window
     *
     * @param value
     */
    public void pushValue(ComputeData value) {
        synchronized (values) {
            values.addLast(value);
        }
        adjustValues();
    }

    /**
     * Add a list of values to the data list.
     * This does not guarantee the position of the data entry of each data point.
     * All the data will be sorted in ascending order of the timestamps.
     *
     * @param valuesToAdd
     */
    public void addValues(List<ComputeData> valuesToAdd) {
        synchronized (values) {
            values.addAll(valuesToAdd);
        }
        adjustValues();
    }

    /**
     * Adjust array to fit the window size if required
     */
    private void adjustValues() {
        if (values.isEmpty()) {
            return;
        }
        synchronized (values) {
            values.sort(Comparator.comparingLong(ComputeData::getLatestTimestamp));
            long windowEndTimestamp = values.getLast().getMinuteTimestamp();
            long windowStartTimestamp = TimeUtil.getMinuteTimestamp(windowEndTimestamp, -windowSize);
            int i = 0;
            ComputeData prevData = null;
            while (i < values.size()) {
                boolean remove = false;
                ComputeData data = values.get(i);
                long minuteTimestamp = data.getMinuteTimestamp();
                if (minuteTimestamp < windowStartTimestamp || data.getValue().isNaN() || data.getValue() == 0) {
                    remove = true;
                }
                if (!remove && prevData != null && prevData.getMinuteTimestamp() == minuteTimestamp) {
                    prevData.setValue(data.value);
                    remove = true;
                }
                if (remove) {
                    values.remove(data);
                } else {
                    prevData = data;
                    i++;
                }
            }
            log.trace("{} : Compute engine values {}", thread.getThreadName(),
                    values.stream().map(computeData -> String.valueOf(computeData.value)).collect(Collectors.joining(", ")));
            computeAllValues();
        }
    }

    private void computeAllValues() {
        log.trace("{} : Computing all values", thread.getThreadName());
        studies.forEach((study, studyModel) ->
                computeExecutor.execute(() -> {
                    List<ComputeEngine.ComputeData> valuesWindow =
                            values.subList(Math.max(values.size() - study.getWindowSize(), 0), values.size());
                    studyModel.compute(valuesWindow, study.getWindowSize());
                    studyModel.setValues(thread);
                })
        );
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
        pushValue(new ComputeData(thread.getC(), System.currentTimeMillis()));
    }

    @Data
    public static class ComputeData {
        private long latestTimestamp;
        private Double value;

        public ComputeData(double value, long latestTimestamp) {
            this.value = value;
            this.latestTimestamp = latestTimestamp;
        }

        public long getMinuteTimestamp() {
            return TimeUtil.getMinuteTimestamp(latestTimestamp);
        }
    }
}
