package com.wemaka.weatherapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wemaka.weatherapp.R;
import com.wemaka.weatherapp.ui.MainActivity; // Импорт для перехода на MainActivity

import java.util.List;

public class SearchResultActivity extends AppCompatActivity {

    private ListView listView;
    private WeatherDatabaseHelper dbHelper;
    private List<String> history;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        listView = findViewById(R.id.listView);
        dbHelper = new WeatherDatabaseHelper(this);

        loadSearchHistory();

        // Обработчик обычного нажатия
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSearch = history.get(position);
            Toast.makeText(this, "Вы выбрали: " + selectedSearch, Toast.LENGTH_SHORT).show();

            // Возвращаем результат в MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selected_city", selectedSearch); // Передаём выбранный город
            startActivity(intent);
        });

        // Обработчик долгого нажатия
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String queryToDelete = history.get(position);
            dbHelper.deleteQuery(queryToDelete); // Удаление записи из БД
            history.remove(position); // Удаление из списка
            adapter.notifyDataSetChanged(); // Обновление списка
            Toast.makeText(this, "Удалено: " + queryToDelete, Toast.LENGTH_SHORT).show();
            return true; // Указываем, что событие обработано
        });
    }

    // Загрузка истории поиска из базы данных
    private void loadSearchHistory() {
        history = dbHelper.getSearchHistory();
        if (history == null || history.isEmpty()) {
            Toast.makeText(this, "История поиска пуста", Toast.LENGTH_SHORT).show();
        } else {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, history);
            listView.setAdapter(adapter);
        }
    }
}
