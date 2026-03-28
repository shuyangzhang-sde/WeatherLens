package edu.uiuc.cs427app.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import edu.uiuc.cs427app.R;

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

        rootView.setBackgroundColor(Color.parseColor(themeSpec.getBackgroundColor()));
        applyThemeRecursively(rootView, themeSpec);
    }

    /** Walks the view tree and updates supported view types. */
    private static void applyThemeRecursively(View view, ThemeSpec themeSpec) {
        if (isCardContainer(view.getId())) {
            view.setBackgroundColor(Color.parseColor(themeSpec.getCardColor()));
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
        textView.setTextColor(Color.parseColor(themeSpec.getTextColor()));
        if (textView.getId() == R.id.textViewTitle) {
            textView.setBackgroundColor(Color.parseColor(themeSpec.getHeaderColor()));
        }
    }

    /** Applies accent colors to a Material button. */
    private static void applyMaterialButtonTheme(MaterialButton button, ThemeSpec themeSpec) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(themeSpec.getAccentColor())));
        button.setTextColor(Color.parseColor(themeSpec.getTextColor()));
        button.setStrokeColor(ColorStateList.valueOf(
                Color.parseColor(themeSpec.getBorderColor())));
    }

    /** Applies accent colors to a plain button. */
    private static void applyButtonTheme(Button button, ThemeSpec themeSpec) {
        button.setBackgroundColor(Color.parseColor(themeSpec.getAccentColor()));
        button.setTextColor(Color.parseColor(themeSpec.getTextColor()));
    }

    /** Applies border and hint colors to an outlined input. */
    private static void applyInputLayoutTheme(TextInputLayout inputLayout, ThemeSpec themeSpec) {
        int borderColor = Color.parseColor(themeSpec.getBorderColor());
        int textColor = Color.parseColor(themeSpec.getTextColor());
        inputLayout.setBoxStrokeColor(borderColor);
        inputLayout.setHintTextColor(ColorStateList.valueOf(textColor));
        inputLayout.setDefaultHintTextColor(ColorStateList.valueOf(textColor));
    }

    /** Applies text colors to an editable field. */
    private static void applyEditTextTheme(EditText editText, ThemeSpec themeSpec) {
        int textColor = Color.parseColor(themeSpec.getTextColor());
        editText.setTextColor(textColor);
        editText.setHintTextColor(textColor);
    }

    /** Returns true for rows that should use the card color. */
    private static boolean isCardContainer(int viewId) {
        return viewId == R.id.locationRowChampaign
                || viewId == R.id.locationRowChicago
                || viewId == R.id.locationRowLosAngeles;
    }
}
