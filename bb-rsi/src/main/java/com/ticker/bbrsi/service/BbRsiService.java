package com.ticker.bbrsi.service;

import com.ticker.bbrsi.model.BbRsiThreadModel;
import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.common.service.StratTickerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.ticker.bbrsi.constants.BbRsiConstants.*;

/**
 * The type Bb rsi service.
 */
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
        put(BB_RSI_THREAD_STATE_LT_GTT_FAILED, "Lower trigger GTT fail");
        put(BB_RSI_THREAD_STATE_UT_GTT_FAILED, "Upper trigger GTT fail");
        put(BB_RSI_THREAD_STATE_LT_REENTER, "Lower trigger Re-enter");
        put(BB_RSI_THREAD_STATE_UT_REENTER, "Upper trigger Re-enter");
    }};

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

    private void buyActionRevenge(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Revenge buy");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            thread.setTradeStartTime(System.currentTimeMillis());
            thread.resetTriggers();
            thread.setGoodToTrigger(true);
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
            thread.setGoodToTrigger(true);
        } catch (Exception e) {

        }
    }

    private void resetThread(BbRsiThread thread) {
        log.info(thread.getThreadName() + " : Reset triggers");
        thread.resetTriggers();
        thread.setGoodToTrigger(true);
        thread.setCurrentState(BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
    }

    private void buyAction2(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Buy action 2");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
            thread.setTradeStartTime(System.currentTimeMillis());
            if (thread.getRsi() >= RSI_UPPER_LIMIT_REBOUND) {
                log.trace(thread.getThreadName() + " : rebound");
                thread.setStates(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT, BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE, BB_RSI_THREAD_STATE_UT_TRIGGER_START);
            }
        } catch (Exception e) {

        }
    }

    private void sellAction2(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Sell action 2");
        try {
            squareOff(thread);
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
            thread.setTradeStartTime(System.currentTimeMillis());
            if (thread.getRsi() <= RSI_LOWER_LIMIT_REBOUND) {
                log.trace(thread.getThreadName() + " : rebound");
                thread.setStates(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD, BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE, BB_RSI_THREAD_STATE_LT_TRIGGER_START);
            }
        } catch (Exception e) {

        }
    }

    private void checkForUtWaveEnd1(BbRsiThread thread) {
        log.trace("");
        log.trace(thread.getThreadName() + " : checkForUtWaveEnd1");
        checkUtPanicExit(thread);
        checkUtSatisfy(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_REENTER
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_SOLD) {
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Upward trend");
            if (thread.getTrailValue() - thread.getCurrentValue() < 0.8 * thread.getTargetThreshold()) {
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

    private void checkUtSatisfy(BbRsiThread thread) {
        if (thread.isSatisfied()) {
            double curr = thread.getCurrentValue();
            double trail = thread.getTrailValue();
            double newTrail = curr + ((2 * thread.getTargetThreshold()) / 3);
            thread.setTrailValue(Math.min(trail, newTrail));
        } else if (thread.getTrailValue() - thread.getTargetThreshold() >= thread.getCurrentValue() - 0.004) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2);
            if (isDownwardTrend(thread)) {
                thread.setIntermediateState(BB_RSI_THREAD_STATE_UT_REENTER);
                thread.setSatisfied(true);
            } else {
                squareOff(thread);
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
            }
        }
    }

    private void checkForLtWaveEnd1(BbRsiThread thread) {
        log.trace("");

        log.trace(thread.getThreadName() + " : checkForLtWaveEnd1");
        checkLtPanicExit(thread);
        checkLtSatisfy(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_REENTER
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_BOUGHT) {
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger()) {
            log.trace(thread.getThreadName() + " : No check");
            return;
        }
        if (isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : Downward trend");
            if (thread.getCurrentValue() - thread.getTrailValue() < 0.8 * thread.getTargetThreshold()) {
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

    private void checkLtSatisfy(BbRsiThread thread) {
        if (thread.isSatisfied()) {
            double curr = thread.getCurrentValue();
            double trail = thread.getTrailValue();
            double newTrail = curr - ((2 * thread.getTargetThreshold()) / 3);
            thread.setTrailValue(Math.max(trail, newTrail));
        } else if (thread.getTrailValue() + thread.getTargetThreshold() <= thread.getCurrentValue() + 0.004) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2);
            if (isUpwardTrend(thread)) {
                thread.setIntermediateState(BB_RSI_THREAD_STATE_LT_REENTER);
                thread.setSatisfied(true);
            } else {
                squareOff(thread);
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
            }
        }
    }

    private void checkForUtWaveEnd(BbRsiThread thread) {
        log.trace("");
        log.trace(thread.getThreadName() + " : checkForUtWaveEnd");
        checkUtPanicExit(thread);
        checkUtSatisfy(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_REENTER
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_SOLD) {
            return;
        }
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
        if ((thread.getCurrentValue() - thread.getDip() > thread.getTargetThreshold())
                || ((thread.getRsiDiff() > RSI_UPPER_LIMIT_PANIC_DIFF || thread.getCurrentValue() - thread.getTrailValue() > thread.getTargetThreshold())
                && thread.getCurrentValue() - thread.getDip() > 0.5 * thread.getTargetThreshold())
                || (thread.isSatisfied() && thread.getTrailValue() <= thread.getCurrentValue())) {
            log.debug("");
            log.debug(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
            log.debug(thread.getThreadName() + " : TrailValue " + thread.getTrailValue());
            log.debug(thread.getThreadName() + " : Dip " + thread.getDip());
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            log.debug(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
            log.debug(thread.getThreadName() + " : UtPanicExit");
            int additionalPanic1 = 0;
            int additionalPanic2 = 0;
            if (thread.getRsiDiff() > RSI_UPPER_LIMIT_PANIC_DIFF || (thread.isSatisfied() && thread.getTrailValue() >= thread.getCurrentValue())) {
                log.debug("Panic buy faster rsi diff");
                additionalPanic1 = PANIC_TIME_OFF / (PANIC_TIME_OFF_EMERGENCY_RETRIES - (thread.isSatisfied() ? 1 : 0));
            }
            if (thread.getCurrentValue() - thread.getDip() + 0.004 > (thread.isSatisfied() ? 1 : 1.2) * thread.getTargetThreshold()) {
                double factor = (((thread.getCurrentValue() - thread.getDip()) / thread.getTargetThreshold() - 0.5f)
                        * ((thread.getCurrentValue() < thread.getTrailValue() - 0.3 * thread.getTargetThreshold() ? 1 : 3) + (thread.isSatisfied() ? 1 : 0)) * PANIC_TIME_OFF) / PANIC_TIME_OFF_EMERGENCY_RETRIES;
                log.debug("Panic buy faster " + factor);
                additionalPanic2 = (int) Math.max(factor, 1);
            }
            log.debug("Panic buy net faster " + (additionalPanic1 + additionalPanic2));
            thread.setPanicBuy(thread.getPanicBuy() + additionalPanic1 + additionalPanic2);
            panicBuy(thread);
        } else {
            if (thread.getPanicBuy() > 0) {
                log.debug(thread.getThreadName() + " : Reset panic buy");
                thread.setPanicBuy(0);
            }
        }
        log.trace(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
        log.trace(thread.getThreadName() + " : TrailValue " + thread.getTrailValue());
        log.trace(thread.getThreadName() + " : Dip " + thread.getDip());
        log.trace(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
        log.trace(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());

        if (thread.isLowValue()) {
            if (thread.getTrailValue() - thread.getCurrentValue() >= thread.getTargetThreshold() + 0.005) {
                log.info(thread.getThreadName() + " : Threshold reached");
                buyAction2(thread);
            }
        }
    }

    private void panicBuy(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Panic buy " + thread.getPanicBuy());
        thread.setPanicBuy(thread.getPanicBuy() + 1);
        if (thread.getPanicBuy() > PANIC_TIME_OFF) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_PANIC_BUY);
            try {
                double tradeStart = thread.getTrailValue();
                log.info(thread.getThreadName() + " : panic square-off");
                double tradeEnd = squareOff(thread);
                if (tradeStart - tradeEnd > thread.getTargetThreshold() || thread.isSatisfied()) {
                    thread.setCurrentState(BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT);
                } else {
                    thread.resetTriggers();
                    thread.setGoodToTrigger(true);
                    thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_START);
                }
                thread.setTradeStartTime(System.currentTimeMillis());
            } catch (Exception e) {
                log.info("Error while buying back", e);
            }
        }
    }

    private void checkForLtWaveEnd(BbRsiThread thread) {
        log.trace("");
        log.trace(thread.getThreadName() + " : checkForLtWaveEnd");
        checkLtPanicExit(thread);
        checkLtSatisfy(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_REENTER
                || thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_BOUGHT) {
            return;
        }
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
        if ((thread.getPeak() - thread.getCurrentValue() > thread.getTargetThreshold())
                || ((thread.getRsiDiff() < RSI_LOWER_LIMIT_PANIC_DIFF || thread.getTrailValue() - thread.getCurrentValue() > thread.getTargetThreshold())
                && thread.getPeak() - thread.getCurrentValue() > 0.5 * thread.getTargetThreshold())
                || (thread.isSatisfied() && thread.getTrailValue() >= thread.getCurrentValue())) {
            log.debug("");
            log.debug(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
            log.debug(thread.getThreadName() + " : TrailValue " + thread.getTrailValue());
            log.debug(thread.getThreadName() + " : Peak " + thread.getPeak());
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            log.debug(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());
            log.debug(thread.getThreadName() + " : LtPanicExit");
            int additionalPanic1 = 0;
            int additionalPanic2 = 0;
            if (thread.getRsiDiff() < RSI_LOWER_LIMIT_PANIC_DIFF || (thread.isSatisfied() && thread.getTrailValue() >= thread.getCurrentValue())) {
                log.debug("Panic sell faster rsi diff");
                additionalPanic1 = PANIC_TIME_OFF / (PANIC_TIME_OFF_EMERGENCY_RETRIES - (thread.isSatisfied() ? 1 : 0));
            }
            if (thread.getPeak() - thread.getCurrentValue() + 0.004 > (thread.isSatisfied() ? 1 : 1.2) * thread.getTargetThreshold()) {
                double factor = (((thread.getPeak() - thread.getCurrentValue()) / thread.getTargetThreshold() - 0.5f)
                        * ((thread.getCurrentValue() > thread.getTrailValue() + 0.3 * thread.getTargetThreshold() ? 1 : 3) + (thread.isSatisfied() ? 1 : 0)) * PANIC_TIME_OFF) / PANIC_TIME_OFF_EMERGENCY_RETRIES;
                log.debug("Panic sell faster " + factor);
                additionalPanic2 = (int) Math.max(factor, 1);
            }
            log.debug("Panic sell net faster " + (additionalPanic1 + additionalPanic2));
            thread.setPanicSell(thread.getPanicSell() + additionalPanic1 + additionalPanic2);
            panicSell(thread);
        } else {
            if (thread.getPanicSell() > 0) {
                log.debug(thread.getThreadName() + " : Reset panic sell");
                thread.setPanicSell(0);
            }
        }
        log.trace(thread.getThreadName() + " : CurrentValue " + thread.getCurrentValue());
        log.trace(thread.getThreadName() + " : TrailValue " + thread.getTrailValue());
        log.trace(thread.getThreadName() + " : Peak " + thread.getPeak());
        log.trace(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
        log.trace(thread.getThreadName() + " : TargetThreshold " + thread.getTargetThreshold());

        if (thread.isLowValue()) {
            if (thread.getCurrentValue() - thread.getTrailValue() >= thread.getTargetThreshold() + 0.005) {
                log.info(thread.getThreadName() + " : Threshold reached");
                sellAction2(thread);
            }
        }
    }


    private void panicSell(BbRsiThread thread) {
        log.debug(thread.getThreadName() + " : Panic sell " + thread.getPanicSell());
        thread.setPanicSell(thread.getPanicSell() + 1);
        if (thread.getPanicSell() > PANIC_TIME_OFF) {
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_PANIC_SELL);
            try {
                double tradeStart = thread.getTrailValue();
                log.info(thread.getThreadName() + " : panic square-off");
                double tradeEnd = squareOff(thread);
                if (tradeEnd - tradeStart > thread.getTargetThreshold() || thread.isSatisfied()) {
                    thread.setCurrentState(BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD);
                } else {
                    thread.resetTriggers();
                    thread.setGoodToTrigger(true);
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
            thread.setTrailValue(thread.getCurrentValue());
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
            thread.setTrailValue(thread.getCurrentValue());
        } catch (Exception e) {

        }
    }

    private void checkForLtEnd(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForLtEnd");
        checkLtGTT(thread);
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger(20)) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT && isUpwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : LtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkLtGTT(BbRsiThread thread) {
        if (thread.getTema() <= thread.getBbL() || thread.getRsi() < GTT_RSI_LOWER_LIMIT) {
            thread.setGoodToTrigger(true);
        }
        if (!thread.isGoodToTrigger() && thread.getRsi() > RSI_LOWER_LIMIT_REBOUND) {
            thread.setIntermediateState(BB_RSI_THREAD_STATE_LT_GTT_FAILED, BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
        }
    }

    private void checkPreMatureLtEnd(BbRsiThread thread) {
        thread.setDip(thread.getCurrentValue());
        if (thread.getRsi() >= RSI_LOWER_LIMIT
                && thread.getTema() >= thread.getBbL()
                && !isSameMinTrigger(thread.getTriggerStartTime())
                && isEomTrigger(10)
                && thread.isGoodToTrigger()) {
            log.debug("");
            log.debug(thread.getThreadName() + " : checkPreMatureLtEnd");
            log.debug(thread.getThreadName() + " : thread.getCurrentValue() - thread.getDip() = " + (thread.getCurrentValue() - thread.getDip()));
            log.debug(thread.getThreadName() + " : " + thread.getCurrentValue() + " - " + thread.getDip() + " = " + (thread.getCurrentValue() - thread.getDip()));
            log.debug(thread.getThreadName() + " : isUpwardTrend(thread) = " + isUpwardTrend(thread));
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            if ((isUpwardTrend(thread) && thread.getCurrentValue() - thread.getDip() > 0.5 * thread.getTargetThreshold()) ||
                    (thread.getCurrentValue() - thread.getDip() > 0.75f * thread.getTargetThreshold() &&
                            thread.getRsiDiff() > RSI_LOWER_LIMIT_PREMATURE_DIFF)) {
                log.debug(thread.getThreadName() + " : PreMatureLtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            }
            log.debug(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForLtEnd1(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForLtEnd1");
        checkLtGTT(thread);
        checkPreMatureLtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_LT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureLtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger(20)) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() > RSI_LOWER_LIMIT
                && thread.getTema() > thread.getBbL()
                && isUpwardTrend(thread)
                && thread.isGoodToTrigger()) {
            log.trace(thread.getThreadName() + " : LtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_LT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkForUtEnd(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForUtEnd");
        checkUtGTT(thread);
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureUtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerStartTime()) || !isEomTrigger(20)) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT && isDownwardTrend(thread)) {
            log.trace(thread.getThreadName() + " : UtEnd");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END1);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    private void checkUtGTT(BbRsiThread thread) {
        if (thread.getTema() >= thread.getBbU() || thread.getRsi() > GTT_RSI_UPPER_LIMIT) {
            thread.setGoodToTrigger(true);
        }
        if (!thread.isGoodToTrigger() && thread.getRsi() < RSI_UPPER_LIMIT_REBOUND) {
            thread.setIntermediateState(BB_RSI_THREAD_STATE_UT_GTT_FAILED, BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER);
        }
    }

    private void checkPreMatureUtEnd(BbRsiThread thread) {
        thread.setPeak(thread.getCurrentValue());
        if (thread.getRsi() <= RSI_UPPER_LIMIT
                && thread.getTema() <= thread.getBbU()
                && !isSameMinTrigger(thread.getTriggerStartTime())
                && isEomTrigger(10)
                && thread.isGoodToTrigger()) {
            log.debug("");
            log.debug(thread.getThreadName() + " : checkPreMatureUtEnd");
            log.debug(thread.getThreadName() + " : thread.getPeak() - thread.getCurrentValue() = " + (thread.getPeak() - thread.getCurrentValue()));
            log.debug(thread.getThreadName() + " : " + thread.getPeak() + " - " + thread.getCurrentValue() + " = " + (thread.getPeak() - thread.getCurrentValue()));
            log.debug(thread.getThreadName() + " : isDownwardTrend(thread) = " + isDownwardTrend(thread));
            log.debug(thread.getThreadName() + " : RsiDiff " + thread.getRsiDiff());
            if ((isDownwardTrend(thread) && thread.getPeak() - thread.getCurrentValue() > 0.5f * thread.getTargetThreshold()) ||
                    (thread.getPeak() - thread.getCurrentValue() > 0.75f * thread.getTargetThreshold() &&
                            thread.getRsiDiff() < RSI_UPPER_LIMIT_PREMATURE_DIFF)) {
                log.debug(thread.getThreadName() + " : PreMatureUtEnd");
                thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            }
            log.debug(thread.getThreadName() + " : thread.getTargetThreshold() = " + thread.getTargetThreshold());
        }
    }

    private void checkForUtEnd1(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkForUtEnd1");
        checkUtGTT(thread);
        checkPreMatureUtEnd(thread);
        if (thread.getCurrentState() == BB_RSI_THREAD_STATE_UT_TRIGGER_END2) {
            log.info(thread.getThreadName() + " : PreMatureUtEnd");
            return;
        }
        if (isSameMinTrigger(thread.getTriggerWaveEndTime()) || !isEomTrigger(20)) {
            log.trace(thread.getThreadName() + " : no check");
            return;
        }
        if (thread.getRsi() < RSI_UPPER_LIMIT
                && thread.getTema() < thread.getBbU()
                && isDownwardTrend(thread)
                && thread.isGoodToTrigger()) {
            log.trace(thread.getThreadName() + " : UtEnd1");
            thread.setCurrentState(BB_RSI_THREAD_STATE_UT_TRIGGER_END2);
            thread.setTriggerWaveEndTime(System.currentTimeMillis());
        }
    }

    @Override
    public Map<Integer, String> getStateValueMap() {
        return stateValueMap;
    }

    private void checkTrigger(BbRsiThread thread) {
        log.trace(thread.getThreadName() + " : checkTrigger");
        thread.resetTriggers();
        if (!isEomTrigger(15)) {
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
