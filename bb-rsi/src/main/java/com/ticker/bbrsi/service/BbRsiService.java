package com.ticker.bbrsi.service;

import com.ticker.bbrsi.model.BbRsiThreadModel;
import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.common.service.StratTickerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ticker.bbrsi.constants.BbRsiConstants.BB_RSI_THREAD_STATE_STRAT_FAILED;
import static com.ticker.bbrsi.constants.BbRsiConstants.BB_RSI_THREAD_STATE_WAITING_FOR_ACTION;

@Slf4j
@Service
public class BbRsiService extends StratTickerService<BbRsiThread, BbRsiThreadModel> {

    private static final Map<Integer, String> stateValueMap = new HashMap<Integer, String>() {{
        put(BB_RSI_THREAD_STATE_STRAT_FAILED, "Thread failed");
        put(BB_RSI_THREAD_STATE_WAITING_FOR_ACTION, "Waiting for action");
    }};

    @Override
    public BbRsiThreadModel createTickerThreadModel(BbRsiThread thread) {
        return new BbRsiThreadModel(thread);
    }

    @Override
    @Async("stratTaskExecutor")
    public void doAction(BbRsiThread thread) {
        if (!thread.isFetching()) {
            return;
        }
        switch (thread.getCurrentState()) {
            case 0:
                thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_ACTION);
                break;
            case BB_RSI_THREAD_STATE_WAITING_FOR_ACTION:
                checkState(thread);
                break;
            case BB_RSI_THREAD_STATE_STRAT_FAILED:
                thread.destroy();
                break;
        }
    }

    @Override
    public Map<Integer, String> getStateValueMap() {
        return stateValueMap;
    }

    // TODO: Implement method
    private void checkState(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkState");
    }

}
