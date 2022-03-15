package com.ticker.bbrsisafe.rx;

import com.ticker.bbrsisafe.service.BbRsiSafeService;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.StratThread;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.bbrsisafe.constants.BbRsiSafeConstants.BB_RSI_SAFE_THREAD_COMP_NAME;

/**
 * The type Bb rsi safe thread.
 */
@Getter
@Setter
@Slf4j
@Component(BB_RSI_SAFE_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class BbRsiSafeThread<S extends BbRsiSafeService<?, ?>> extends StratThread<S> {

    private long triggerWaveEndTime;
    private long tradeStartTime;
    private double tradeValue;
    private double trailValue;
    private double targetValue = 0;
    private double peak;
    private double dip;
    private double rsiPrev;
    private long rsiSetTime;
    private int panicSell = 0;
    private int panicBuy = 0;
    private boolean goodToTrigger = false;
    private boolean satisfied = false;

    /**
     * Instantiates a new Bb rsi safe thread.
     *
     * @param entity the entity
     */
    public BbRsiSafeThread(ExchangeSymbolEntity entity) {
        super(entity);
    }

    @Override
    public void setRsi(double rsi) {
        if (!service.isSameMinTrigger(rsiSetTime)) {
            rsiPrev = rsi;
            rsiSetTime = System.currentTimeMillis();
        }
        super.setRsi(rsi);
    }

    /**
     * Gets rsi diff.
     *
     * @return the rsi diff
     */
    public double getRsiDiff() {
        return getRsi() - getRsiPrev();
    }

    /**
     * Sets peak.
     *
     * @param peak the peak
     */
    public void setPeak(double peak) {
        if (this.peak == 0 || this.peak < peak) {
            log.debug(getThreadName() + " : peak changed from " + this.peak);
            this.peak = peak;
            log.debug(getThreadName() + " : peak changed to   " + this.peak);
        }
    }

    /**
     * Sets dip.
     *
     * @param dip the dip
     */
    public void setDip(double dip) {
        if (this.dip == 0 || this.dip > dip) {
            log.debug(getThreadName() + " : dip changed from " + this.dip);
            this.dip = dip;
            log.debug(getThreadName() + " : dip changed to   " + this.dip);
        }
    }

    @Override
    public void resetTriggers() {
        log.trace(getThreadName() + " : Resetting triggers");
        super.resetTriggers();
        triggerWaveEndTime = 0;
        tradeStartTime = 0;
        tradeValue = 0;
        trailValue = 0;
        dip = 0;
        peak = 0;
        panicSell = 0;
        panicBuy = 0;
        setGoodToTrigger(false);
        setSatisfied(false);
    }
}
