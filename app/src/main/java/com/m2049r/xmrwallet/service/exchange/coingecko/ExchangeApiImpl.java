/*
 * Copyright (c) 2017-2018 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.service.exchange.coingecko;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ExchangeApiImpl implements ExchangeApi {
    static final String LOKI_CRYPTO_ID = "loki-network";

    @NonNull
    private final OkHttpClient okHttpClient;

    private final HttpUrl baseUrl;

    //so we can inject the mockserver url
    @VisibleForTesting
    ExchangeApiImpl(@NonNull final OkHttpClient okHttpClient, final HttpUrl baseUrl) {

        this.okHttpClient = okHttpClient;
        this.baseUrl = baseUrl;
    }

    public ExchangeApiImpl(@NonNull final OkHttpClient okHttpClient) {
        this(okHttpClient, HttpUrl.parse("https://api.coingecko.com/api/v3/simple/price"));
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {

        if (baseCurrency.equals(quoteCurrency)) {
            callback.onSuccess(new ExchangeRateImpl(baseCurrency, quoteCurrency, 1.0));
            return;
        }

        boolean inverse = false;
        String symbol = baseCurrency;
        String fiat = null;

        if (baseCurrency.equals(Wallet.LOKI_SYMBOL)) {
            symbol = baseCurrency;
            fiat = quoteCurrency;
            inverse = false;
        }

        if (quoteCurrency.equals(Wallet.LOKI_SYMBOL)) {
            symbol = quoteCurrency;
            fiat = baseCurrency;
            inverse = true;
        }

        if (symbol.equals(Wallet.LOKI_SYMBOL)) {
            symbol = LOKI_CRYPTO_ID;
        }

        if (fiat == null) {
            callback.onError(new IllegalArgumentException("no fiat specified"));
            return;
        }

        final boolean inverseRate = inverse;
        final String symbolKey = symbol;
        final String fiatKey = fiat.toLowerCase();

        final HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("ids", symbol.toLowerCase())
                .addQueryParameter("vs_currencies", fiat.toLowerCase())
                .build();

        final Request httpRequest = createHttpRequest(url);

        okHttpClient.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(final Call call, final IOException ex) {
                callback.onError(ex);
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final JSONObject json = new JSONObject(response.body().string());
                        if (json.isNull(symbolKey)) {
                            callback.onError(new ExchangeException(response.code(), "No price found"));
                            return;
                        }

                        final JSONObject price = json.getJSONObject(symbolKey);
                        final double rate = price.getDouble(fiatKey);
                        reportSuccess(baseCurrency, quoteCurrency, rate, inverseRate, callback);
                    } catch (JSONException ex) {
                        callback.onError(new ExchangeException(ex.getLocalizedMessage()));
                    }
                } else {
                    callback.onError(new ExchangeException(response.code(), response.message()));
                }
            }
        });
    }

    void reportSuccess(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate, boolean inverse, ExchangeCallback callback) {
        final ExchangeRate exchangeRate = new ExchangeRateImpl(baseCurrency, quoteCurrency, rate, inverse);
        callback.onSuccess(exchangeRate);
    }

    void reportSuccess(JSONObject jsonObject, boolean swapAssets, ExchangeCallback callback) {
        try {
            final ExchangeRate exchangeRate = new ExchangeRateImpl(jsonObject, swapAssets);
            callback.onSuccess(exchangeRate);
        } catch (JSONException ex) {
            callback.onError(new ExchangeException(ex.getLocalizedMessage()));
        } catch (ExchangeException ex) {
            callback.onError(ex);
        }
    }

    private Request createHttpRequest(final HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
