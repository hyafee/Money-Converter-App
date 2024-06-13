package com.example.currencyconverter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.currencyconverter.network.CurrencyService;
import com.example.currencyconverter.network.ExchangeRates;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private EditText amountEditText;
    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private TextView resultTextView;
    private boolean isToastShown = false;
    private CurrencyService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        amountEditText = findViewById(R.id.amountEditText);
        fromCurrencySpinner = findViewById(R.id.fromCurrencySpinner);
        toCurrencySpinner = findViewById(R.id.toCurrencySpinner);
        Button convertButton = findViewById(R.id.convertButton);
        resultTextView = findViewById(R.id.resultTextView);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://v6.exchangerate-api.com/v6/aeab228d7f52915dff2b31f1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(CurrencyService.class);

        setupSpinners();

        convertButton.setOnClickListener(v -> convertCurrency());
    }

    private void showToast(String message) {
        if (!isToastShown) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            isToastShown = true;
            findViewById(android.R.id.content).postDelayed(() -> isToastShown = false, 3000);
        }
    }

    private void convertCurrency() {
        String amountStr = amountEditText.getText().toString();
        if (amountStr.isEmpty()) {
            showToast("Silahkan masukkan nominal");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String fromCurrency = fromCurrencySpinner.getSelectedItem().toString();
        String toCurrency = toCurrencySpinner.getSelectedItem().toString();

        resultTextView.setText("Calculating...");

        Call<ExchangeRates> call = service.getExchangeRates(fromCurrency);
        call.enqueue(new Callback<ExchangeRates>() {
            @Override
            public void onResponse(Call<ExchangeRates> call, Response<ExchangeRates> response) {
                if (response.isSuccessful()) {
                    ExchangeRates rates = response.body();
                    if (rates != null && "success".equals(rates.getResult())) {
                        Map<String, Double> rateMap = rates.getConversionRates();
                        if (rateMap != null && rateMap.containsKey(toCurrency)) {
                            Double rate = rateMap.get(toCurrency);
                            double result = amount * rate;

                            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                            currencyFormat.setCurrency(Currency.getInstance(toCurrency));
                            String formattedResult = currencyFormat.format(result);
                            resultTextView.setText(formattedResult);
                        } else {
                            showToast("Rate not available for " + toCurrency);
                        }
                    } else {
                        showToast("Failed to get rates: " + (rates != null ? rates.getResult() : "null response"));
                    }
                } else {
                    showToast("Failed to get rates");
                }
            }

            @Override
            public void onFailure(Call<ExchangeRates> call, Throwable t) {
                showToast("Error: Silahkan cek koneksi internet anda");
            }
        });
    }

    private void setupSpinners() {
        List<String> currencies = List.of("USD", "EUR", "GBP", "IDR", "JPY");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.custom_spinner_item, R.id.currencyTextView, currencies) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return createView(position, convertView, parent);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return createView(position, convertView, parent);
            }

            private View createView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View view = inflater.inflate(R.layout.custom_spinner_item, parent, false);

                TextView currencyTextView = view.findViewById(R.id.currencyTextView);
                ImageView flagImageView = view.findViewById(R.id.flagImageView);

                String currency = currencies.get(position);
                currencyTextView.setText(currency);

                // Set flag image based on currency
                int flagResId = getFlagResId(currency);
                flagImageView.setImageResource(flagResId);

                return view;
            }

            private int getFlagResId(String currency) {
                switch (currency) {
                    case "USD":
                        return R.drawable.us_flag;
                    case "EUR":
                        return R.drawable.eu_flag;
                    case "GBP":
                        return R.drawable.uk_flag;
                    case "IDR":
                        return R.drawable.indonesia_flag;
                    case "JPY":
                        return R.drawable.japan_flag;
                    default:
                        return R.drawable.ic_launcher_foreground; // Default flag
                }
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromCurrencySpinner.setAdapter(adapter);
        toCurrencySpinner.setAdapter(adapter);

        // Set default currency to IDR (Indonesian Rupiah)
        int defaultCurrencyPosition = currencies.indexOf("IDR");
        toCurrencySpinner.setSelection(defaultCurrencyPosition);
    }
}
