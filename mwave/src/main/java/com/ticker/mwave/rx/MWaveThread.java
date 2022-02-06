package com.ticker.mwave.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.StratThread;
import com.ticker.mwave.service.MWaveService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_COMP_NAME;

/**
 * The type M wave thread.
 */
@Getter
@Setter
@Slf4j
@Component(MWAVE_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class MWaveThread extends StratThread<MWaveService> {

    /**
     * Instantiates a new M wave thread.
     *
     * @param entity the entity
     */
    public MWaveThread(ExchangeSymbolEntity entity) {
        super(entity);
    }
}
