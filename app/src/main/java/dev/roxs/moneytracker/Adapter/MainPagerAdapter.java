package dev.roxs.moneytracker.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import dev.roxs.moneytracker.page.MoneyTrackerFragment;
import dev.roxs.moneytracker.page.WealthFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new WealthFragment();
        }
        return new MoneyTrackerFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
