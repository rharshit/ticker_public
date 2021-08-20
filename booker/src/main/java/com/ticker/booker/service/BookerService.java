package com.ticker.booker.service;

import com.ticker.common.exception.TickerException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.TokenException;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class BookerService {

    private static KiteConnect kiteSdk;
    private static User user;
    private static String requestToken;

    private static String apiKey;
    private static String apiSecret;
    private static String userId;

    private static KiteConnect getKiteSdk() {
        if (kiteSdk == null) {
            kiteSdk = new KiteConnect(apiKey);

            kiteSdk.setUserId(userId);

            kiteSdk.setSessionExpiryHook(new SessionExpiryHook() {
                @Override
                public void sessionExpired() {
                    log.warn("Kite session expired");
                }
            });
        }
        return kiteSdk;
    }

    private static void generateSession() throws IOException, KiteException {
        user = getKiteSdk().generateSession(requestToken, apiSecret);
        getKiteSdk().setAccessToken(user.accessToken);
        getKiteSdk().setPublicToken(user.publicToken);
    }

    @Autowired
    public void getPropertiesBean(@Value("${com.zerodha.apikey}") String apiKey, @Value("${com.zerodha.apisecret}") String apiSecret, @Value("${com.zerodha.userid}") String userId) {
        BookerService.apiKey = apiKey;
        BookerService.apiSecret = apiSecret;
        BookerService.userId = userId;
    }

    public String getZerodhaLoginURL() {
        return getKiteSdk().getLoginURL();
    }

    public void setRequestToken(String requestToken) {
        BookerService.requestToken = requestToken;
        try {
            generateSession();
        } catch (KiteException | IOException e) {
            log.error("Error while generating session", e);
        }
    }

    public Map<String, Margin> getZerodhaMargins() {
        try {
            return getKiteSdk().getMargins();
        } catch (TokenException e) {
            log.error("Error in token while getting margins");
            log.error(e.message);
            throw new TickerException("Error in token while getting Margins");
        } catch (KiteException | Exception e) {
            log.error("Error while getting margins", e);
            throw new TickerException("Error while getting Margins");
        }
    }

    public Integer bookRegularOrder(String tradingSymbol, String exchange, String transactionType, String orderType,
                                    Integer quantity, String product, Float price, Float triggerPrice,
                                    Integer disclosedQuantity, String validity, String tag) {
        return null;
    }

    public void populateLogs(String logs) {
        log.info(logs);
    }

}
