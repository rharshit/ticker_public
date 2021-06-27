package com.ticker.bbrsi.controller;

import com.ticker.bbrsi.model.BbRsiThreadModel;
import com.ticker.bbrsi.service.BbRsiService;
import com.ticker.common.controller.StratController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ticker.bbrsi.constants.BbRsiConstants.BB_RSI_THREAD_COMP_NAME;

@RestController
@RequestMapping("/")
public class BbRsiController extends StratController<BbRsiService, BbRsiThreadModel> {

    @Override
    public String getThreadCompName() {
        return BB_RSI_THREAD_COMP_NAME;
    }
}
