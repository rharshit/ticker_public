package com.ticker.bbrsisafe.rx;

import com.ticker.bbrsisafe.service.BbRsiService;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.StratThread;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.bbrsisafe.constants.BbRsiConstants.BB_RSI_THREAD_COMP_NAME;

@Getter
@Setter
@Slf4j
@Component(BB_RSI_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class BbRsiThread extends StratThread<BbRsiService> {

    private long triggerWaveEndTime;
    private long tradeStartTime;
    private float tradeValue;
    private float targetValue = 0;
    private float peak;
    private float dip;
    private float rsiPrev;
    private long rsiSetTime;
    private int panicSell = 0;
    private int panicBuy = 0;
    private boolean safeState = true;
    private boolean goodToTrigger = false;

    public BbRsiThread(ExchangeSymbolEntity entity) {
        super(entity);
    }

    @Override
    public void setRsi(float rsi) {
        if (!service.isSameMinTrigger(rsiSetTime)) {
            rsiPrev = rsi;
            rsiSetTime = System.currentTimeMillis();
        }
        super.setRsi(rsi);
    }

    public float getRsiDiff() {
        return getRsi() - getRsiPrev();
    }

    public void setPeak(float peak) {
        if (this.peak == 0 || this.peak < peak) {
            log.debug(getThreadName() + " : peak changed from " + this.peak);
            this.peak = peak;
            log.debug(getThreadName() + " : peak changed to   " + this.peak);
        }
    }

    public void setDip(float dip) {
        if (this.dip == 0 || this.dip > dip) {
            log.debug(getThreadName() + " : dip changed from " + this.dip);
            this.dip = dip;
            log.debug(getThreadName() + " : dip changed to   " + this.dip);
        }
    }

    @Override
    public void setPositionQty(int positionQty) {
        super.setPositionQty(positionQty);
        if (getPositionQty() == 0) {
            setSafeState(true);
        }
    }

    @Override
    public void resetTriggers() {
        log.trace(getThreadName() + " : Resetting triggers");
        super.resetTriggers();
        triggerWaveEndTime = 0;
        tradeStartTime = 0;
        tradeValue = 0;
        dip = 0;
        peak = 0;
        panicSell = 0;
        panicBuy = 0;
        setSafeState(true);
        setGoodToTrigger(false);
    }
}
