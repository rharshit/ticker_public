package com.ticker.mwave.service;

import com.ticker.common.service.StratTickerService;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.rx.MWaveThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_STRAT_FAILED;
import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_WAITING_FOR_ACTION;

@Slf4j
@Service
public class MWaveService extends StratTickerService<MWaveThread, MWaveThreadModel> {

    @Override
    public MWaveThreadModel createTickerThreadModel(MWaveThread thread) {
        return new MWaveThreadModel(thread);
    }


    @Override
    @Async("stratTaskExecutor")
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

    // TODO: Implement method
    private void checkState(MWaveThread thread) {
        log.trace(thread.getThreadName() + " : checkState");
    }

}
