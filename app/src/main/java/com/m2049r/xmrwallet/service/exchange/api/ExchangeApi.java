/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2017 m2049r
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

package com.m2049r.xmrwallet.service.exchange.api;


import android.support.annotation.NonNull;


public interface ExchangeApi {

    /**
     * Queries the exchange rate
     *
     * @param baseCurrency  base currency
     * @param quoteCurrency quote currency
     * @param callback      the callback with the exchange rate
     */
    void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                           @NonNull final ExchangeCallback callback);

}

