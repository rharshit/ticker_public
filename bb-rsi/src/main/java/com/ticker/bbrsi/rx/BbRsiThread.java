package com.ticker.bbrsi.rx;

import com.ticker.bbrsi.service.BbRsiService;
import com.ticker.bbrsisafe.rx.BbRsiSafeThread;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.bbrsi.constants.BbRsiConstants.BB_RSI_THREAD_COMP_NAME;

/**
 * The type Bb rsi thread.
 */
@Getter
@Setter
@Slf4j
@Component(BB_RSI_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class BbRsiThread extends BbRsiSafeThread<BbRsiService> {

}
