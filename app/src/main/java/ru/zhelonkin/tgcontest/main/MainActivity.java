package ru.zhelonkin.tgcontest.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import ru.zhelonkin.tgcontest.Prefs;
import ru.zhelonkin.tgcontest.R;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Prefs.isDarkMode(this) ? R.style.AppTheme_Night : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.theme) {
            switchTheme();
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchTheme() {
        Prefs.setDarkMode(this, !Prefs.isDarkMode(this));
        recreate();
    }
}
