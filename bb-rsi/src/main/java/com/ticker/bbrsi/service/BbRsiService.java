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
        if (!thread.isFetching() || thread.isLocked() || thread.getTargetThreshold() == 0) {
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
        log.info(thread.getThreadName() + " : Revenge buy");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.resetTriggers();
        } catch (Exception e) {

        }
    }

    private void sellActionRevenge(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Revenge sell");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.resetTriggers();
        } catch (Exception e) {

        }
    }

    private void resetThread(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Reset triggers");
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
        log.trace(thread.getThreadName() + " : checkForUtWaveEnd1");
        checkUtPanicExit(thread);
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Upward trend");
            if (thread.getTradeValue() - thread.getCurrentValue() < 0.8 * thread.getTargetThreshold()) {
                log.trace(thread.getThreadName() + " : No action");
                return;
            } else {
                log.debug(thread.getThreadName() + " : ended 2");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2);
                return;
            }
        } else {
            log.trace(thread.getThreadName() + " : back to wave");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE);
        }
        thread.setTriggerWaveEndTime(System.currentTimeMillis());
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2) {
            buyAction2(thread);
        }
    }

    private void checkForLtWaveEnd1(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForLtWaveEnd1");
        checkLtPanicExit(thread);
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Downward trend");
            if (thread.getCurrentValue() - thread.getTradeValue() < 0.8 * thread.getTargetThreshold()) {
                log.trace(thread.getThreadName() + " : No action");
                return;
            } else {
                log.trace(thread.getThreadName() + " : ended 2");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2);
            }
        } else {
            log.trace(thread.getThreadName() + " : back to wave");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE);
        }
        thread.setTriggerWaveEndTime(System.currentTimeMillis());
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2) {
            sellAction2(thread);
        }
    }

    private void checkForUtWaveEnd(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForUtWaveEnd");
        checkUtPanicExit(thread);
        if (isSameMinTrigger(thread.getTradeStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Upward");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkUtPanicExit(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkUtPanicExit");
        thread.setDip(thread.getCurrentValue());
        log.debug("");
        if ((thread.getCurrentValue() - thread.getDip() > thread.getTargetThreshold()) ||
                ((thread.getRsiDiff() > 10 || thread.getCurrentValue() - thread.getTradeValue() > thread.getTargetThreshold())
                        && thread.getCurrentValue() - thread.getDip() > 0.5 * thread.getTargetThreshold())) {
            log.debug(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
            log.debug(thread.getThreadName() + " : TradeValue " + thread.getTradeValue());
            log.debug(thread.getThreadName() + " : Dip " + thread.getDip());
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            log.debug(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
            log.info(thread.getThreadName() + " : UtPanicExit");
            if (thread.getRsiDiff() > 10 || thread.getCurrentValue() - thread.getDip() > 1.2 * thread.getTargetThreshold()) {
                log.info("Panic buy faster");
                thread.setPanicBuy(thread.getPanicBuy() + PANIC_TIME_OFF / PANIC_TIME_OFF_EMERGENCY_RETRIES);
            }
            panicBuy(thread);
        } else {
            if (thread.getPanicBuy() > 0) {
                log.info(thread.getThreadName() + " : Reset panic buy");
                thread.setPanicBuy(0);
            }
        }
        log.trace(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
        log.trace(thread.getThreadName() + " : TradeValue " + thread.getTradeValue());
        log.trace(thread.getThreadName() + " : Peak " + thread.getPeak());
        log.trace(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
        log.trace(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
    }

    private void panicBuy(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Panic buy " + thread.getPanicBuy());
        thread.setPanicBuy(thread.getPanicBuy() + 1);
        if (thread.getPanicBuy() > PANIC_TIME_OFF) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_PANIC_BUY);
            try {
                float tradeStart = thread.getTradeValue();
                log.info(thread.getThreadName() + " : panic square-off");
                float tradeEnd = squareOff(thread);
                if (tradeStart - tradeEnd > thread.getTargetThreshold()) {
                    thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
                } else {
                    thread.resetTriggers();
                    thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
                }
                thread.setTradeStartTime(System.currentTimeMillis());
            } catch (Exception e) {
                log.info("Error while buying back", e);
            }
        }
    }

    private void checkForLtWaveEnd(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForLtWaveEnd");
        checkLtPanicExit(thread);
        if (isSameMinTrigger(thread.getTradeStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Downward");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkLtPanicExit(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkLtPanicExit");
        thread.setPeak(thread.getCurrentValue());
        log.debug("");
        if ((thread.getPeak() - thread.getCurrentValue() > thread.getTargetThreshold()) ||
                ((thread.getRsiDiff() < -10 || thread.getTradeValue() - thread.getCurrentValue() > thread.getTargetThreshold())
                        && thread.getPeak() - thread.getCurrentValue() > 0.5 * thread.getTargetThreshold())) {
            log.debug(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
            log.debug(thread.getThreadName() + " : TradeValue " + thread.getTradeValue());
            log.debug(thread.getThreadName() + " : Peak " + thread.getPeak());
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            log.debug(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
            log.info(thread.getThreadName() + " : LtPanicExit");
            if (thread.getRsiDiff() < -10 || thread.getPeak() - thread.getCurrentValue() > 1.2 * thread.getTargetThreshold()) {
                log.info("Panic sell faster");
                thread.setPanicSell(thread.getPanicSell() + PANIC_TIME_OFF / PANIC_TIME_OFF_EMERGENCY_RETRIES);
            }
            panicSell(thread);
        } else {
            if (thread.getPanicSell() > 0) {
                log.info(thread.getThreadName() + " : Reset panic sell");
                thread.setPanicSell(0);
            }
        }
        log.trace(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
        log.trace(thread.getThreadName() + " : TradeValue " + thread.getTradeValue());
        log.trace(thread.getThreadName() + " : Peak " + thread.getPeak());
        log.trace(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
        log.trace(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
    }

    private void panicSell(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Panic sell " + thread.getPanicSell());
        thread.setPanicSell(thread.getPanicSell() + 1);
        if (thread.getPanicSell() > PANIC_TIME_OFF) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_PANIC_SELL);
            try {
                float tradeStart = thread.getTradeValue();
                log.info(thread.getThreadName() + " : panic square-off");
                float tradeEnd = squareOff(thread);
                if (tradeEnd - tradeStart > thread.getTargetThreshold()) {
                    thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
                } else {
                    thread.resetTriggers();
                    thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
                }
                thread.setTradeStartTime(System.currentTimeMillis());
            } catch (Exception e) {
                log.info("Error while selling back", e);
            }
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
        log.trace(thread.getThreadName() + " : checkForLtEnd");
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT && isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : LtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkPreMatureLtEnd(BbRsiThread thread) {
        thread.setDip(thread.getCurrentValue());
        if (!isSameMinTrigger(thread.getTriggerStartTime()) && isEomTrigger(35)) {
            log.trace(thread.getThreadName() + " : checkPreMatureLtEnd");
            log.trace(thread.getThreadName() + " : thread.getCurrentValue() - thread.getDip() = " + (thread.getCurrentValue() - thread.getDip()));
            log.trace(thread.getThreadName() + " : " + thread.getCurrentValue() + " - " + thread.getDip() + " = " + (thread.getCurrentValue() - thread.getDip()));
            log.trace(thread.getThreadName() + " : isUpwardTrend(thread) = " + isUpwardTrend(thread));
            if (((isEomTrigger(30) && isUpwardTrend(thread)) && thread.getCurrentValue() - thread.getDip() > thread.getTargetThreshold()) ||
                    (thread.getCurrentValue() - thread.getDip() > 1.2 * thread.getTargetThreshold())) {
                log.trace(thread.getThreadName() + " : PreMatureLtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            }
            log.trace(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForLtEnd1(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForLtEnd1");
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT && isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : LtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        } else {
            if (thread.getRsi() <= RSI_LOWER_LIMIT_REBOUND) {
                log.trace(thread.getThreadName() + " : rebound");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            }
        }
    }

    private void checkForUtEnd(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForUtEnd");
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT && isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : UtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkPreMatureUtEnd(BbRsiThread thread) {
        thread.setPeak(thread.getCurrentValue());
        if (!isSameMinTrigger(thread.getTriggerStartTime()) && isEomTrigger(35)) {
            log.trace(thread.getThreadName() + " : checkPreMatureUtEnd");
            log.trace(thread.getThreadName() + " : thread.getPeak() - thread.getCurrentValue() = " + (thread.getPeak() - thread.getCurrentValue()));
            log.trace(thread.getThreadName() + " : " + thread.getPeak() + " - " + thread.getCurrentValue() + " = " + (thread.getPeak() - thread.getCurrentValue()));
            log.trace(thread.getThreadName() + " : isDownwardTrend(thread) = " + isDownwardTrend(thread));
            if (((isEomTrigger(30) && isDownwardTrend(thread)) && thread.getPeak() - thread.getCurrentValue() > thread.getTargetThreshold()) ||
                    (thread.getPeak() - thread.getCurrentValue() > 1.2 * thread.getTargetThreshold())) {
                log.trace(thread.getThreadName() + " : PreMatureUtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            }
            log.trace(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForUtEnd1(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForUtEnd1");
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureUtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT && isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : UtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        } else {
            if (thread.getRsi() >= RSI_UPPER_LIMIT_REBOUND) {
                log.trace(thread.getThreadName() + " : rebound");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            }
        }
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
