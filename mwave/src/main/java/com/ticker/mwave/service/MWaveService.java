package com.ticker.mwave.service;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.service.TickerThreadService;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.rx.MWaveThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MWaveService extends TickerThreadService<MWaveThread, MWaveThreadModel> {

    @Override
    public void createThread(String exchange, String symbol) {
        MWaveThread thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            return;
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (MWaveThread) ctx.getBean("fetcherThread");
            thread.setEntity(entity);
            getThreadPool().add(thread);

            thread.setService(this);

            log.info(thread.getThreadName() + " : added thread");
            thread.start();
        }

    }

    @Override
    public MWaveThreadModel createTickerThreadModel(MWaveThread thread) {
        return new MWaveThreadModel(thread);
    }
}
