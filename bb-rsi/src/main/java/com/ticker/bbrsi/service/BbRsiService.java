package com.ticker.bbrsi.service;

import com.ticker.bbrsi.model.BbRsiThreadModel;
import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.common.service.StratTickerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ticker.bbrsi.constants.BbRsiConstants.*;

@Slf4j
@Service
public class BbRsiService extends StratTickerService<BbRsiThread, BbRsiThreadModel> {

    private static final Map<Integer, String> stateValueMap = new HashMap<Integer, String>() {{
        put(BB_RSI_THREAD_STATE_STRAT_FAILED, "Thread failed");
        put(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER, "Waiting for trigger");
        put(BB_RSI_THREAD_STATE_LT_TRIGGER_START, "Lower trigger started");
        put(BB_RSI_THREAD_STATE_UT_TRIGGER_START, "Upper trigger started");
        put(BB_RSI_THREAD_STATE_LT_TRIGGER_END1, "Lower trigger ended1");
        put(BB_RSI_THREAD_STATE_LT_TRIGGER_END2, "Lower trigger ended2");
        put(BB_RSI_THREAD_STATE_UT_TRIGGER_END1, "Upper trigger ended1");
        put(BB_RSI_THREAD_STATE_UT_TRIGGER_END2, "Upper trigger ended2");
        put(BB_RSI_THREAD_STATE_LT_BOUGHT, "Lower trigger bought");
        put(BB_RSI_THREAD_STATE_UT_SOLD, "Upper trigger sold");
        put(BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE, "Waiting for lower wave to end");
        put(BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE, "Waiting for upper wave to end");
        put(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1, "Lower trigger wave ended1");
        put(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2, "Lower trigger wave ended2");
        put(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED3, "Lower trigger wave ended3");
        put(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1, "Upper trigger wave ended1");
        put(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2, "Upper trigger wave ended2");
        put(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED3, "Upper trigger wave ended3");
        put(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD, "Lower trigger sold SO");
        put(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT, "Lower trigger bought SO");
        put(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE, "Lower trigger revenge trading");
        put(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE, "Upper trigger revenge trading");
        put(BB_RSI_THREAD_STATE_LT_PANIC_SELL, "Lower trigger panic sell");
        put(BB_RSI_THREAD_STATE_UT_PANIC_BUY, "Upper trigger panic buy");
    }};

    @Override
    public BbRsiThreadModel createTickerThreadModel(BbRsiThread thread) {
        return new BbRsiThreadModel(thread);
    }

    @Override
    @Async("stratTaskExecutor")
    public void doAction(BbRsiThread thread) {
        if (!thread.isFetching() || thread.isLocked()) {
            return;
        }
        synchronized (thread) {
            thread.setLocked(true);
            try {
                switch (thread.getCurrentState()) {
                    case BB_RSI_THREAD_STATE_STRAT_FAILED:
                        thread.destroy();
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

    private void buyActionRevenge(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Revenge buy");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.resetTriggers();
        } catch (Exception e) {

        }
    }

    private void sellActionRevenge(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Revenge sell");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.resetTriggers();
        } catch (Exception e) {

        }
    }

    private void resetThread(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Reset triggers");
        thread.resetTriggers();
        thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
    }

    private void buyAction2(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Buy action 2");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
            thread.setTradeStartTime(System.currentTimeMillis());
        } catch (Exception e) {

        }
    }

    private void sellAction2(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Sell action 2");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
            thread.setTradeStartTime(System.currentTimeMillis());
        } catch (Exception e) {

        }
    }

    private void checkForUtWaveEnd1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForUtWaveEnd1");
        checkUtPanicExit(thread);
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : No check");
            return;
        }
        if (isUpwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : Upward trend");
            if (thread.getTargetThreshold() - thread.getCurrentValue() < thread.getTargetThreshold()) {
                log.info(thread.getThreadName() + " : Revenge trading");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE);
            } else {
                log.debug(thread.getThreadName() + " : No action");
//                log.debug(thread.getThreadName() + " : ended 2");
//                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2);
                return;
            }
        } else {
            log.debug(thread.getThreadName() + " : back to wave");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE);
        }
        thread.setTriggerWaveEndTime(System.currentTimeMillis());
    }

    private void checkForLtWaveEnd1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForLtWaveEnd1");
        checkLtPanicExit(thread);
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : No check");
            return;
        }
        if (isDownwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : Downward trend");
            if (thread.getCurrentValue() - thread.getTriggerStartValue() < thread.getTargetThreshold()) {
                log.debug(thread.getThreadName() + " : No action");
//                log.info(thread.getThreadName() + " : Revenge trading");
//                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE);
                return;
            } else {
                log.debug(thread.getThreadName() + " : ended 2");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2);
            }
        } else {
            log.debug(thread.getThreadName() + " : back to wave");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE);
        }
        thread.setTriggerWaveEndTime(System.currentTimeMillis());
    }

    private void checkForUtWaveEnd(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForUtWaveEnd");
        checkUtPanicExit(thread);
        if (isSameMinTrigger(thread.getTradeStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : No check");
            return;
        }
        if (isUpwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : Upward");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkUtPanicExit(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkUtPanicExit");
        thread.setDip(thread.getCurrentValue());
        if (thread.getCurrentValue() - thread.getDip() > thread.getTargetThreshold()) {
            log.info(thread.getThreadName() + " : UtPanicExit");
            panicBuy(thread);
        }
    }

    private void panicBuy(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Panic buy");
        thread.setCurrentState(BB_RSI_THREAD_STATE_UT_PANIC_BUY);
        try {
            log.info(thread.getThreadName() + " : panic square-off");
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
            thread.setTradeStartTime(System.currentTimeMillis());
        } catch (Exception e) {

        }
    }

    private void checkForLtWaveEnd(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForLtWaveEnd");
        checkLtPanicExit(thread);
        if (isSameMinTrigger(thread.getTradeStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : No check");
            return;
        }
        if (isDownwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : Downward");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkLtPanicExit(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkLtPanicExit");
        thread.setPeak(thread.getCurrentValue());
        if (thread.getPeak() - thread.getCurrentValue() > thread.getTargetThreshold()) {
            log.info(thread.getThreadName() + " : LtPanicExit");
            panicSell(thread);
        }
    }

    private void panicSell(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Panic sell");
        thread.setCurrentState(BB_RSI_THREAD_STATE_LT_PANIC_SELL);
        try {
            log.info(thread.getThreadName() + " : panic square-off");
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
            thread.setTradeStartTime(System.currentTimeMillis());
        } catch (Exception e) {

        }
    }

    private void buyAction1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Panic buy");
        try {
            buy(thread, thread.getEntity().getMinQty());
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_BOUGHT);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.setTradeValue(thread.getCurrentValue());
        } catch (Exception e) {

        }
    }

    private void sellAction1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Sell action 1");
        try {
            sell(thread, thread.getEntity().getMinQty());
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_SOLD);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.setTradeValue(thread.getCurrentValue());
        } catch (Exception e) {

        }
    }

    private void checkForLtEnd(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForLtEnd");
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT && isUpwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : LtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkPreMatureLtEnd(BbRsiThread thread) {
        thread.setDip(thread.getCurrentValue());
        if (!isSameMinTrigger(thread.getTriggerStartTime()) && isEomTrigger(35)) {
            log.debug(thread.getThreadName() + " : checkPreMatureLtEnd");
            log.debug(thread.getThreadName() + " : thread.getCurrentValue() - thread.getDip() = " + (thread.getCurrentValue() - thread.getDip()));
            log.debug(thread.getThreadName() + " : " + thread.getCurrentValue() + " - " + thread.getDip() + " = " + (thread.getCurrentValue() - thread.getDip()));
            log.debug(thread.getThreadName() + " : isUpwardTrend(thread) = " + isUpwardTrend(thread));
            if ((isUpwardTrend(thread) && thread.getCurrentValue() - thread.getDip() > thread.getTargetThreshold()) ||
                    (thread.getCurrentValue() - thread.getDip() > 1.2 * thread.getTargetThreshold())) {
                log.debug(thread.getThreadName() + " : PreMatureLtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            }
            log.debug(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForLtEnd1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForLtEnd1");
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT && isUpwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : LtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        } else {
            if (thread.getRsi() <= RSI_LOWER_LIMIT_REBOUND) {
                log.debug(thread.getThreadName() + " : rebound");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            }
//            else {
//                log.debug(thread.getThreadName() + " : reset");
//                thread.resetTriggers();
//                thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
//            }

        }
//        doAction(thread);
    }

    private void checkForUtEnd(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForUtEnd");
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT && isDownwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : UtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkPreMatureUtEnd(BbRsiThread thread) {
        thread.setPeak(thread.getCurrentValue());
        if (!isSameMinTrigger(thread.getTriggerStartTime()) && isEomTrigger(35)) {
            log.debug(thread.getThreadName() + " : checkPreMatureUtEnd");
            log.debug(thread.getThreadName() + " : thread.getPeak() - thread.getCurrentValue() = " + (thread.getPeak() - thread.getCurrentValue()));
            log.debug(thread.getThreadName() + " : " + thread.getPeak() + " - " + thread.getCurrentValue() + " = " + (thread.getPeak() - thread.getCurrentValue()));
            log.debug(thread.getThreadName() + " : isDownwardTrend(thread) = " + isDownwardTrend(thread));
            if (isDownwardTrend(thread) && thread.getPeak() - thread.getCurrentValue() > thread.getTargetThreshold() ||
                    (thread.getPeak() - thread.getCurrentValue() > 1.2 * thread.getTargetThreshold())) {
                log.debug(thread.getThreadName() + " : PreMatureUtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            }
            log.debug(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForUtEnd1(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : checkForUtEnd1");
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureUtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger()) {
            log.debug(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT && isDownwardTrend(thread)) {
            log.debug(thread.getThreadName() + " : UtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        } else {
            if (thread.getRsi() >= RSI_UPPER_LIMIT_REBOUND) {
                log.debug(thread.getThreadName() + " : rebound");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            }
//            else {
//                log.debug(thread.getThreadName() + " : reset");
//                thread.resetTriggers();
//                thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
//            }

        }
//        doAction(thread);
    }

    @Override
    public Map<Integer, String> getStateValueMap() {
        return stateValueMap;
    }

    private void checkTrigger(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkTrigger");
        if (!isEomTrigger()) {
            return;
        }
        if (thread.getRsi() <= RSI_LOWER_LIMIT && thread.getO() > thread.getC()) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            thread.setTriggerStartValue(thread.getCurrentValue());
            thread.setTriggerStartTime(System.currentTimeMillis());
        } else if (thread.getRsi() >= RSI_UPPER_LIMIT && thread.getO() < thread.getC()) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            thread.setTriggerStartValue(thread.getCurrentValue());
            thread.setTriggerStartTime(System.currentTimeMillis());
        }
    }
}
