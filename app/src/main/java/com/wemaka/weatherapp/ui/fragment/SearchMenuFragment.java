package com.wemaka.weatherapp.ui.fragment;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.wemaka.weatherapp.R;
import com.wemaka.weatherapp.ui.adapter.SearchMenuAdapter;
import com.wemaka.weatherapp.databinding.FragmentSearchMenuBinding;
import com.wemaka.weatherapp.store.proto.LocationCoordProto;
import com.wemaka.weatherapp.ui.MainActivity;
import com.wemaka.weatherapp.ui.viewmodel.MainViewModel;
import com.wemaka.weatherapp.ui.WeatherDatabaseHelper; // Подключаем класс базы данных

import eightbitlab.com.blurview.BlurViewFacade;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;

public class SearchMenuFragment extends BottomSheetDialogFragment {
	public static final String TAG = "SearchMenuFragment";
	private FragmentSearchMenuBinding binding;
	private MainViewModel model;
	private WeatherDatabaseHelper dbHelper; // Создаем объект для работы с базой данных

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		binding = FragmentSearchMenuBinding.inflate(getLayoutInflater());
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		model = ((MainActivity) requireActivity()).getModel();
		dbHelper = new WeatherDatabaseHelper(requireContext()); // Инициализируем базу данных

		addBlurBackground();

		SearchMenuAdapter searchMenuAdapter = new SearchMenuAdapter();
		searchMenuAdapter.setOnItemClickListener(item -> {
			Log.i(TAG, "Click: " + item.getLatitude() + " : " + item.getLongitude());

			// Записываем выбранное название в поле ввода
			String locationName = String.valueOf(item.getLocationName()); // Используем getLocationName(), если оно существует в классе PlaceInfo
			binding.searchView.setQuery(locationName, false);

			// Сохраняем запрос в базу данных
			if (dbHelper.insertQuery(locationName)) {
				Log.i(TAG, "Query saved to database: " + locationName);
			} else {
				Log.e(TAG, "Failed to save query to database");
			}

			// Загружаем погоду по выбранному местоположению
			model.fetchWeatherAndPlace(
					new LocationCoordProto(
							Double.parseDouble(item.getLatitude()),
							Double.parseDouble(item.getLongitude())
					)
			);

			dismiss();  // Закрываем фрагмент
		});

		binding.rvSearchList.setAdapter(searchMenuAdapter);

		binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.i(TAG, "Click request search");

				// Сохраняем запрос в базу данных
				if (dbHelper.insertQuery(query)) {
					Log.i(TAG, "Query saved to database: " + query);
				} else {
					Log.e(TAG, "Failed to save query to database");
				}

				model.searchLocation(query).observe(getViewLifecycleOwner(), resource -> {
					if (resource.isSuccess() && resource.getData() != null) {
						searchMenuAdapter.submitList(resource.getData());
					} else if (resource.isError() && resource.getMessage() != null) {
						showToast(resource.getMessage());
					}
				});

				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), getTheme());
		dialog.setOnShowListener(dialogInterface -> {
			BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
			View parentLayout = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			if (parentLayout != null) {
				BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parentLayout);
				setupFullHeight(parentLayout);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				behavior.setSkipCollapsed(true);
			}
		});
		return dialog;
	}

	public static SearchMenuFragment newInstance() {
		return new SearchMenuFragment();
	}

	private void setupFullHeight(View bottomSheet) {
		ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
		layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		bottomSheet.setLayoutParams(layoutParams);
	}

	private void addBlurBackground() {
		float radius = 20f;

		View decorView = requireActivity().getWindow().getDecorView();
		ViewGroup rootView = decorView.findViewById(android.R.id.content);

		BlurViewFacade blur;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			blur = binding.blurView.setupWith(rootView, new RenderEffectBlur());
		} else {
			blur = binding.blurView.setupWith(rootView, new RenderScriptBlur(requireContext()));
		}

		blur.setFrameClearDrawable(ResourcesCompat.getDrawable(getResources(),
				R.drawable.block_blur_rounded_background, null)).setBlurRadius(radius);

		binding.blurView.setBackgroundResource(R.drawable.block_blur_rounded_background);

		binding.blurView.setClipToOutline(true);
	}

	private void showToast(String message) {
		Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
	}
}
