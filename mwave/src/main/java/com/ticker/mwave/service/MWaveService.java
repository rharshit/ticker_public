package com.ticker.mwave.service;

import com.ticker.common.service.StratTickerService;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.rx.MWaveThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_STRAT_FAILED;
import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_WAITING_FOR_ACTION;

/**
 * The type M wave service.
 */
@Slf4j
@Service
public class MWaveService extends StratTickerService<MWaveThread, MWaveThreadModel> {

    private static final Map<Integer, String> stateValueMap = new HashMap<Integer, String>() {{
        put(MWAVE_THREAD_STATE_STRAT_FAILED, "Thread failed");
        put(MWAVE_THREAD_STATE_WAITING_FOR_ACTION, "Waiting for action");
    }};

    @Override
    public MWaveThreadModel createTickerThreadModel(MWaveThread thread) {
        return new MWaveThreadModel(thread);
    }

    @Override
    public void doAction(MWaveThread thread) {
        if (!thread.isFetching()) {
            return;
        }
        switch (thread.getCurrentState()) {
            case 0:
                thread.setCurrentState(MWAVE_THREAD_STATE_WAITING_FOR_ACTION);
                break;
            case MWAVE_THREAD_STATE_WAITING_FOR_ACTION:
                checkState(thread);
                break;
            case MWAVE_THREAD_STATE_STRAT_FAILED:
                thread.destroy();
                break;
        }
    }

    @Override
    public Map<Integer, String> getStateValueMap() {
        return stateValueMap;
    }

    // TODO: Implement method
    private void checkState(MWaveThread thread) {
        log.trace(thread.getThreadName() + " : checkState");
    }

}
