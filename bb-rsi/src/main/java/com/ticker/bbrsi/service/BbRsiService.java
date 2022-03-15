package com.ticker.bbrsi.service;

import com.ticker.bbrsi.model.BbRsiThreadModel;
import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.bbrsisafe.service.BbRsiSafeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.ticker.bbrsi.constants.BbRsiConstants.*;

/**
 * The type Bb rsi service.
 */
@Slf4j
@Service
public class BbRsiService extends BbRsiSafeService<BbRsiThread, BbRsiThreadModel> {

    @Override
    public BbRsiThreadModel createTickerThreadModel(BbRsiThread thread) {
        return new BbRsiThreadModel(thread);
    }

    @Override
    public void doAction(BbRsiThread thread) {
        if (!thread.isFetching() || thread.isLocked() || thread.getTargetThreshold() == 0) {
            return;
        }
        synchronized (thread) {
            thread.setLocked(true);
            try {
                switch (thread.getCurrentState()) {
                    case BB_RSI_THREAD_STATE_STRAT_FAILED:
                        thread.terminateThread(false);
                        break;
                    case 0:
                        thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
                        break;
                    case BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER:
                        checkTrigger(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_TRIGGER_START:
                        checkForLtEnd(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_TRIGGER_END1:
                        checkForLtEnd1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_TRIGGER_START:
                        checkForUtEnd(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_TRIGGER_END1:
                        checkForUtEnd1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_TRIGGER_END2:
                        buyAction1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_TRIGGER_END2:
                        sellAction1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_BOUGHT:
                        thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE);
                        break;
                    case BB_RSI_THREAD_STATE_UT_SOLD:
                        thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE);
                        break;
                    case BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE:
                        checkForLtWaveEnd(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE:
                        checkForUtWaveEnd(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1:
                        checkForLtWaveEnd1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1:
                        checkForUtWaveEnd1(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2:
                        sellAction2(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2:
                        buyAction2(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE:
                        sellActionRevenge(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE:
                        buyActionRevenge(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_PANIC_SELL:
                        panicSell(thread);
                        break;
                    case BB_RSI_THREAD_STATE_UT_PANIC_BUY:
                        panicBuy(thread);
                        break;
                    case BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD:
                    case BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT:
                        resetThread(thread);
                        break;
                }
            } catch (Exception e) {
                log.info(thread.getThreadName() + " : Exception caught in doAction()");
                log.error("doAction(): ", e);
            }
            thread.setLocked(false);
        }
    }

    @Override
    protected boolean isGttTimeFrame() {
        int time = Integer.parseInt(BB_RSI_TIME_FORMAT.format(new Date(System.currentTimeMillis())));
        return time > 900 && time < 1600;
    }
}
