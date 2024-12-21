package com.wemaka.weatherapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.wemaka.weatherapp.data.service.LocationService;
import com.wemaka.weatherapp.databinding.ActivityMainBinding;
import com.wemaka.weatherapp.data.repository.WeatherForecastRepository;
import com.wemaka.weatherapp.ui.fragment.MainFragment;
import com.wemaka.weatherapp.ui.viewmodel.MainViewModel;
import com.wemaka.weatherapp.ui.viewmodel.MainViewModelProviderFactory;
import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity;

import lombok.Getter;

public class MainActivity extends LocaleAwareCompatActivity {
	public static final String TAG = "MainActivity";
	private ActivityMainBinding binding;
	@Getter
	private MainViewModel model;
	@Override
	protected void onResume() {
		super.onResume();

		// Проверка, передан ли город из SearchResultActivity
		if (getIntent().hasExtra("selected_city")) {
			String city = getIntent().getStringExtra("selected_city");
			if (city != null) {
				Log.d(TAG, "Получен город: " + city);
				// Вызов метода для отображения погоды по выбранному городу
				fetchWeatherForCity(city);
				getIntent().removeExtra("selected_city"); // Очистка данных после использования
			}
			else {
				Log.d(TAG, "selected_city отсутствует в Intent");
			}
		}
	}

	private void fetchWeatherForCity(String city) {
		// Реализуйте ваш метод для загрузки данных о погоде
		Toast.makeText(this, "Загрузка погоды для города: " + city, Toast.LENGTH_SHORT).show();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		LocationService locationService = new LocationService(this);
		WeatherForecastRepository repository = new WeatherForecastRepository(locationService);
		MainViewModelProviderFactory viewModelProviderFactory =
				new MainViewModelProviderFactory(repository, this.getApplication());

		model = new ViewModelProvider(this, viewModelProviderFactory).get(MainViewModel.class);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(binding.placeHolder.getId(), MainFragment.newInstance())
					.commit();
		}

		ViewCompat.setOnApplyWindowInsetsListener(binding.placeHolder, (v, insets) -> {
			Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
			return insets;
		});
	}
}