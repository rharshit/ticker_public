package com.ticker.bbrsi.rx;

import com.ticker.bbrsi.service.BbRsiService;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.StratThread;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.bbrsi.constants.BbRsiConstants.BB_RSI_THREAD_COMP_NAME;

@Getter
@Setter
@Slf4j
@Component(BB_RSI_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class BbRsiThread extends StratThread<BbRsiService> {

    public BbRsiThread(ExchangeSymbolEntity entity) {
        super(entity);
    }

    private long triggerWaveEndTime;
    private long tradeStartTime;

    private float targetValue = 0;

    @Override
    public void resetTriggers() {
        super.resetTriggers();
        triggerWaveEndTime = 0;
    }
}
