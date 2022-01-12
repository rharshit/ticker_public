package com.ticker.bbrsisafe.controller;

import com.ticker.bbrsisafe.model.BbRsiThreadModel;
import com.ticker.bbrsisafe.service.BbRsiService;
import com.ticker.common.controller.StratController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ticker.bbrsisafe.constants.BbRsiConstants.BB_RSI_THREAD_COMP_NAME;

@RestController
@RequestMapping("/")
public class BbRsiController extends StratController<BbRsiService, BbRsiThreadModel> {

    @Override
    public String getThreadCompName() {
        return BB_RSI_THREAD_COMP_NAME;
    }
}
