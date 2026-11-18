package edu.uiuc.weatherlens.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

import edu.uiuc.weatherlens.R;

/** Applies a validated theme to supported views. */
public final class ThemeManager {

    /** Utility class. */
    private ThemeManager() {
    }

    /** Applies a theme to the activity content view. */
    public static void applyTheme(AppCompatActivity activity, ThemeSpec themeSpec) {
        View contentRoot = activity.findViewById(android.R.id.content);
        if (contentRoot instanceof ViewGroup && ((ViewGroup) contentRoot).getChildCount() > 0) {
            applyTheme(((ViewGroup) contentRoot).getChildAt(0), themeSpec);
            return;
        }

        applyTheme(contentRoot, themeSpec);
    }

    /** Applies a theme to a root view and its children. */
    public static void applyTheme(View rootView, ThemeSpec themeSpec) {
        if (rootView == null || themeSpec == null) {
            return;
        }

        rootView.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor(themeSpec.getHeaderColor()),
                        Color.parseColor(themeSpec.getBackgroundColor())}));
        applyThemeRecursively(rootView, themeSpec);
    }

    /** Walks the view tree and updates supported view types. */
    private static void applyThemeRecursively(View view, ThemeSpec themeSpec) {
        if (view instanceof BottomNavigationView) {
            BottomNavigationView nav = (BottomNavigationView) view;
            int surface = Color.parseColor(themeSpec.getCardColor());
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(surface);
            shape.setCornerRadius(28 * view.getResources().getDisplayMetrics().density);
            nav.setBackground(shape);
            int accent = Color.parseColor(themeSpec.getAccentColor());
            GradientDrawable activePill = new GradientDrawable();
            activePill.setColor(accent);
            activePill.setCornerRadius(24 * view.getResources().getDisplayMetrics().density);
            StateListDrawable itemBackground = new StateListDrawable();
            itemBackground.addState(new int[]{android.R.attr.state_checked}, activePill);
            itemBackground.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            nav.setItemBackground(itemBackground);
            int selected = readableText(Color.parseColor(themeSpec.getTextColor()), accent);
            int normal = readableText(Color.parseColor(themeSpec.getTextColor()), surface);
            ColorStateList colors = new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{selected, normal});
            nav.setItemIconTintList(colors);
            nav.setItemTextColor(colors);
            return; // Preserve the navigation component's checked-state colors.
        }
        if (view instanceof Chip) {
            Chip chip = (Chip) view;
            int selected = Color.parseColor(themeSpec.getAccentColor());
            int normal = Color.parseColor(themeSpec.getCardColor());
            int preferred = Color.parseColor(themeSpec.getTextColor());
            int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}};
            chip.setChipBackgroundColor(new ColorStateList(states, new int[]{selected, normal}));
            chip.setTextColor(new ColorStateList(states,
                    new int[]{readableText(preferred, selected), readableText(preferred, normal)}));
            chip.setChipStrokeWidth(0);
            return;
        }
        if ("lens_card".equals(view.getTag())) {
            GradientDrawable card = new GradientDrawable();
            card.setColor(Color.parseColor(themeSpec.getCardColor()));
            card.setCornerRadius(28 * view.getResources().getDisplayMetrics().density);
            view.setBackground(card);
        }

        if (view instanceof MaterialButton) {
            applyMaterialButtonTheme((MaterialButton) view, themeSpec);
        } else if (view instanceof Button) {
            applyButtonTheme((Button) view, themeSpec);
        } else if (view instanceof TextInputLayout) {
            applyInputLayoutTheme((TextInputLayout) view, themeSpec);
        } else if (view instanceof EditText) {
            applyEditTextTheme((EditText) view, themeSpec);
        } else if (view instanceof TextView) {
            applyTextTheme((TextView) view, themeSpec);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int childIndex = 0; childIndex < viewGroup.getChildCount(); childIndex++) {
                applyThemeRecursively(viewGroup.getChildAt(childIndex), themeSpec);
            }
        }
    }

    /** Applies title and body text colors. */
    private static void applyTextTheme(TextView textView, ThemeSpec themeSpec) {
        textView.setTextColor(readableText(Color.parseColor(themeSpec.getTextColor()), surfaceColor(textView, themeSpec)));

    }

    /** Applies accent colors to a Material button. */
    private static void applyMaterialButtonTheme(MaterialButton button, ThemeSpec themeSpec) {
        int fill = Color.parseColor("secondary".equals(button.getTag())
                ? themeSpec.getCardColor() : themeSpec.getAccentColor());
        button.setBackgroundTintList(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{Color.rgb(230, 222, 216), fill}));
        button.setCornerRadius((int) (28 * button.getResources().getDisplayMetrics().density));
        button.setAllCaps(false);
        button.setStrokeWidth(0);
        button.setTextColor(new ColorStateList(
                new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}},
                new int[]{Color.rgb(105, 98, 93), readableText(Color.parseColor(themeSpec.getTextColor()), fill)}));
    }

    /** Styles dynamically created action buttons with the same palette. */
    private static void applyButtonTheme(Button button, ThemeSpec themeSpec) {
        int fill = Color.parseColor("secondary".equals(button.getTag())
                ? themeSpec.getBackgroundColor() : themeSpec.getAccentColor());
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(24 * button.getResources().getDisplayMetrics().density);
        button.setBackground(shape);
        button.setAllCaps(false);
        button.setTextColor(readableText(Color.parseColor(themeSpec.getTextColor()), fill));
    }

    /** Applies border and hint colors to an outlined input. */
    private static void applyInputLayoutTheme(TextInputLayout inputLayout, ThemeSpec themeSpec) {
        int borderColor = readableText(Color.parseColor(themeSpec.getBorderColor()), surfaceColor(inputLayout, themeSpec));
        int textColor = readableText(Color.parseColor(themeSpec.getTextColor()), surfaceColor(inputLayout, themeSpec));
        inputLayout.setBoxStrokeColor(borderColor);
        inputLayout.setHintTextColor(ColorStateList.valueOf(textColor));
        inputLayout.setDefaultHintTextColor(ColorStateList.valueOf(textColor));
    }

    /** Applies text colors to an editable field. */
    private static void applyEditTextTheme(EditText editText, ThemeSpec themeSpec) {
        int textColor = readableText(Color.parseColor(themeSpec.getTextColor()), surfaceColor(editText, themeSpec));
        editText.setTextColor(textColor);
        editText.setHintTextColor(textColor);
    }

    /** Keep model-generated text legible on its actual surface. */
    private static int readableText(int preferred, int background) {
        int opaqueBackground = background | 0xFF000000;
        if (ColorUtils.calculateContrast(preferred, opaqueBackground) >= 4.5) return preferred;
        return ColorUtils.calculateLuminance(opaqueBackground) > 0.179 ? Color.BLACK : Color.WHITE;
    }

    private static int surfaceColor(View view, ThemeSpec theme) {
        View current = view;
        while (current != null) {
            if ("lens_card".equals(current.getTag())) return Color.parseColor(theme.getCardColor());
            if ("lens_sun".equals(current.getTag())) return Color.rgb(255, 240, 205);
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        // The gradient spans both endpoints: use whichever gives lower contrast.
        int background = Color.parseColor(theme.getBackgroundColor());
        int header = Color.parseColor(theme.getHeaderColor());
        int text = Color.parseColor(theme.getTextColor());
        return ColorUtils.calculateContrast(text, background | 0xFF000000)
                < ColorUtils.calculateContrast(text, header | 0xFF000000) ? background : header;
    }
}
