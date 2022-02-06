package com.ticker.mwave.controller;

import com.ticker.common.controller.StratController;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.service.MWaveService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_COMP_NAME;

/**
 * The type M wave controller.
 */
@RestController
@RequestMapping("/")
public class MWaveController extends StratController<MWaveService, MWaveThreadModel> {

    @Override
    public String getThreadCompName() {
        return MWAVE_THREAD_COMP_NAME;
    }
}
