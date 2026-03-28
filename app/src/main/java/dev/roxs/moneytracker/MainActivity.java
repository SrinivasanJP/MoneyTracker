package dev.roxs.moneytracker;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import android.Manifest;

import dev.roxs.moneytracker.Adapter.MainPagerAdapter;
import dev.roxs.moneytracker.helper.Notification_Helper;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notification_Helper.scheduleDailyWork(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Set up ViewPager2
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Money Tracker");
            } else {
                tab.setText("Wealth");
            }
        }).attach();

        // Dynamic status bar + tab bar color per page
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateThemeForPage(position);
            }
        });
    }

    private void updateThemeForPage(int position) {
        Window window = getWindow();
        if (position == 0) {
            // Money Tracker: green theme
            int statusColor = ContextCompat.getColor(this, R.color.colorPrimary);
            int tabBg = ContextCompat.getColor(this, R.color.darkSurface);
            window.setStatusBarColor(statusColor);
            tabLayout.setBackgroundColor(tabBg);
            tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.colorPrimary));
            tabLayout.setTabTextColors(
                    ContextCompat.getColor(this, R.color.gray500),
                    ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            // Wealth: dark navy + cyan theme
            int statusColor = Color.parseColor("#0D1117");
            int tabBg = Color.parseColor("#0D1117");
            int accent = Color.parseColor("#00E5FF");
            window.setStatusBarColor(statusColor);
            tabLayout.setBackgroundColor(tabBg);
            tabLayout.setSelectedTabIndicatorColor(accent);
            tabLayout.setTabTextColors(
                    Color.parseColor("#8B95A5"),
                    accent);
        }
    }
}