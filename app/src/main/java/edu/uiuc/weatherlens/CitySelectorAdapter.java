package edu.uiuc.weatherlens;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import edu.uiuc.weatherlens.theme.ThemeSpec;
import java.util.List;
import java.util.function.Supplier;

/** Spinner's popup lives outside the activity tree, so it gets the active theme here. */
final class CitySelectorAdapter extends ArrayAdapter<String> {
    private final Supplier<ThemeSpec> theme;
    private final Supplier<String> selection;

    CitySelectorAdapter(Context context, List<String> cities, Supplier<ThemeSpec> theme, Supplier<String> selection) {
        super(context, R.layout.view_city_selector_item, cities);
        this.theme = theme;
        this.selection = selection;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(readable(Color.parseColor(theme.get().getBackgroundColor())));
        return label;
    }

    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View row = convertView == null ? LayoutInflater.from(getContext())
                .inflate(R.layout.view_city_dropdown_item, parent, false) : convertView;
        ThemeSpec colors = theme.get();
        boolean selected = getItem(position).equals(selection.get());
        int surface = Color.parseColor(colors.getBackgroundColor());
        int accent = Color.parseColor(colors.getAccentColor());
        int fill = selected ? ColorUtils.blendARGB(surface, accent, 0.5f) : surface;
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(14 * getContext().getResources().getDisplayMetrics().density);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 80)),
                background, background.getConstantState().newDrawable()));
        TextView label = row.findViewById(R.id.dropdownCityName);
        label.setText(getItem(position));
        label.setTextColor(readable(fill));
        TextView check = row.findViewById(R.id.dropdownCityCheck);
        check.setTextColor(readable(fill));
        check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        row.setActivated(selected);
        return row;
    }

    private int readable(int surface) {
        int preferred = Color.parseColor(theme.get().getTextColor());
        if (ColorUtils.calculateContrast(preferred, surface | 0xFF000000) >= 4.5) return preferred;
        return ColorUtils.calculateLuminance(surface | 0xFF000000) > 0.179 ? Color.BLACK : Color.WHITE;
    }
}
