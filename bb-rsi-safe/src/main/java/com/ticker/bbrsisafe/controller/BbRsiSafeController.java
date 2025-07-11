package com.ticker.bbrsisafe.controller;

import com.ticker.bbrsisafe.model.BbRsiSafeThreadModel;
import com.ticker.bbrsisafe.service.BbRsiSafeService;
import com.ticker.common.controller.StratController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ticker.bbrsisafe.constants.BbRsiSafeConstants.BB_RSI_SAFE_THREAD_COMP_NAME;

/**
 * The type Bb rsi safe controller.
 */
@RestController
@RequestMapping("/")
public class BbRsiSafeController<S extends BbRsiSafeService<?, ?>, TM extends BbRsiSafeThreadModel<?>> extends StratController<S, TM> {

    @Override
    public String getThreadCompName() {
        return BB_RSI_SAFE_THREAD_COMP_NAME;
    }
}
