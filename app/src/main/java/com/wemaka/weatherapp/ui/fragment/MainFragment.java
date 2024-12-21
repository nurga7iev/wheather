package com.wemaka.weatherapp.ui.fragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.wemaka.weatherapp.R;
import com.wemaka.weatherapp.data.service.LocationService;
import com.wemaka.weatherapp.databinding.FragmentMainBinding;
import com.wemaka.weatherapp.store.proto.DataStoreProto;
import com.wemaka.weatherapp.store.proto.DayForecastProto;
import com.wemaka.weatherapp.store.proto.DaysForecastProto;
import com.wemaka.weatherapp.store.proto.LocationCoordProto;
import com.wemaka.weatherapp.store.proto.SettingsProto;
import com.wemaka.weatherapp.ui.MainActivity;
import com.wemaka.weatherapp.ui.SearchResultActivity;
import com.wemaka.weatherapp.ui.viewmodel.MainViewModel;
import com.wemaka.weatherapp.util.Resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class MainFragment extends Fragment {
	public static final String TAG = "MainFragment";
	private FragmentMainBinding binding;
	private MainViewModel model;
	private final ActivityResultLauncher<String[]> locationPermissionRequest =
			registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onLocationPermissionResult);

	public static MainFragment newInstance() {
		return new MainFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentMainBinding.inflate(inflater, container, false);

		// Other initializations
		getChildFragmentManager()
				.beginTransaction()
				.replace(binding.flTodayWeather.getId(), TodayWeatherFragment.newInstance())
				.commit();

		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		model = ((MainActivity) requireActivity()).getModel();

		handleLocationPermission();
		initUi();
		observeViewModel();

		// Set listener for history button

	}

	private void initUi() {
		binding.swipeRefresh.setOnRefreshListener(() -> {
			Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
			handleLocationPermission();
			model.fetchCurrentWeatherAndPlace();
		});

		// Search button listener
		binding.searchBtn.setOnClickListener(v -> {
			SearchMenuFragment searchBottomSheet = SearchMenuFragment.newInstance();
			searchBottomSheet.show(requireActivity().getSupportFragmentManager(), "SearchBottomSheet");
		});

		// Settings button listener
		binding.settingsBtn.setOnClickListener(v -> {
			Animator animator = AnimatorInflater.loadAnimator(requireContext(), R.animator.slide_left);
			animator.setTarget(binding.swipeRefresh);
			animator.start();

			MainSettingsFragment settingsFragment = MainSettingsFragment.newInstance();
			requireActivity().getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_right)
					.replace(R.id.fragment_container, settingsFragment)
					.addToBackStack(null)
					.commit();
		});

		// MotionLayout transition listener
		binding.motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
			@Override
			public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
				binding.swipeRefresh.setEnabled(false);
			}

			@Override
			public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
			}

			@Override
			public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
				if (currentId == R.id.startHeader) {
					binding.swipeRefresh.setEnabled(true);
				}
			}

			@Override
			public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
			}
		});
	}

	private void observeViewModel() {
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
		SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());

		// Observe weather forecast data
		model.getDaysForecast().observe(getViewLifecycleOwner(), resource -> {
			if (resource.isLoading()) {
				binding.swipeRefresh.setRefreshing(true);
			} else if (resource.isSuccess() && resource.getData() != null) {
				binding.swipeRefresh.setRefreshing(false);

				DayForecastProto df = resource.getData().dayForecast;

				binding.tvMainDegree.setText(df.temperature.temperature + "°");
				binding.tvFeelsLike.setText(getString(R.string.degree_feels_like, df.apparentTemp.temperature + "°"));
				binding.imgMainWeatherIcon.setImageResource(df.imgIdWeatherCode);
				binding.tvWeatherMainText.setText(getString(df.weatherCode));

				Calendar calendar = parseDate(df.date);
				binding.tvLastUpdate.setText(getString(R.string.info_last_update,
						monthFormat.format(calendar.getTime()),
						calendar.get(Calendar.DAY_OF_MONTH),
						timeFormat.format(calendar.getTime())));

				binding.tvDayDegrees.setText(getString(R.string.degree_day_night, df.dayTemp.temperature, df.nightTemp.temperature));
			} else if (resource.isError() && resource.getMessage() != null) {
				binding.swipeRefresh.setRefreshing(false);
				showToast(resource.getMessage());
			}
		});

		// Observe place name data
		model.getPlaceName().observe(getViewLifecycleOwner(), resource -> {
			if (resource.isSuccess() && resource.getData() != null) {
				binding.tvCityCountry.setText(resource.getData());
			} else if (resource.isError()) {
				Log.i(TAG, resource.getErrorMes().orElse(""));
			}
		});
	}

	private Calendar parseDate(String formattedDate) {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

		try {
			calendar.setTime(dateFormat.parse(formattedDate));
		} catch (ParseException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return calendar;
	}

	private void showToast(String message) {
		Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
	}

	private void onLocationPermissionResult(Map<String, Boolean> result) {
		Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
		Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

		if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
			ensureLocationProviderEnabled();
		} else {
			Log.i(TAG, "No location access granted");
		}
	}

	private void handleLocationPermission() {
		if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
				ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			ensureLocationProviderEnabled();
		} else {
			locationPermissionRequest.launch(new String[]{
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACCESS_COARSE_LOCATION
			});
		}
	}

	private void ensureLocationProviderEnabled() {
		LocationManager lm = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
		if (!(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || lm.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
			Snackbar.make(binding.clMain, R.string.provider_absence_notification,
							Snackbar.LENGTH_LONG)
					.setAction("Settings", click -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
					.setMaxInlineActionWidth(1)
					.show();
		}
	}
}
