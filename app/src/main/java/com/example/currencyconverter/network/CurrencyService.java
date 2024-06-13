package com.example.currencyconverter.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CurrencyService {
    @GET("latest/{base}")
    Call<ExchangeRates> getExchangeRates(@Path("base") String base);
}
