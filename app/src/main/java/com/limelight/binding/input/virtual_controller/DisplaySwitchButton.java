package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Toggles between two Sunshine capture outputs using its Ctrl+Alt+Shift+F1..F12 shortcuts.
 */
@SuppressLint("ViewConstructor")
public class DisplaySwitchButton extends DigitalButton {
    private static final int MIN_DISPLAY = 1;
    private static final int MAX_DISPLAY = 12;

    private int firstDisplay = 1;
    private int secondDisplay = 2;
    private int nextDisplay = secondDisplay;
    private int activeDisplay;
    private boolean inputDown;

    public DisplaySwitchButton(VirtualController controller, int elementId, int layer,
                               Context context) {
        super(controller, elementId, layer, context);
        updateLabel();

        addDigitalButtonListener(new DigitalButtonListener() {
            @Override
            public void onClick() {
                setInputDown(true);
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                setInputDown(false);
            }
        });
    }

    private void updateLabel() {
        setText(getContext().getString(R.string.osc_display_switch_label,
                firstDisplay, secondDisplay));
    }

    private void sendShortcutKey(int keyCode, boolean down) {
        virtualController.mappedKeyboardEvent(keyCode, down);
    }

    private void setInputDown(boolean down) {
        if (inputDown == down || virtualController.getInputSink() == null) {
            return;
        }

        inputDown = down;
        if (down) {
            activeDisplay = nextDisplay;
            sendShortcutKey(KeyEvent.KEYCODE_CTRL_LEFT, true);
            sendShortcutKey(KeyEvent.KEYCODE_ALT_LEFT, true);
            sendShortcutKey(KeyEvent.KEYCODE_SHIFT_LEFT, true);
            sendShortcutKey(KeyEvent.KEYCODE_F1 + activeDisplay - 1, true);
        }
        else {
            sendShortcutKey(KeyEvent.KEYCODE_F1 + activeDisplay - 1, false);
            sendShortcutKey(KeyEvent.KEYCODE_SHIFT_LEFT, false);
            sendShortcutKey(KeyEvent.KEYCODE_ALT_LEFT, false);
            sendShortcutKey(KeyEvent.KEYCODE_CTRL_LEFT, false);
            nextDisplay = activeDisplay == firstDisplay ? secondDisplay : firstDisplay;
            activeDisplay = 0;
        }
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Spinner addDisplaySpinner(LinearLayout parent, int labelResource, int selectedDisplay) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView label = new TextView(getContext());
        label.setText(labelResource);
        label.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));

        String[] displayNumbers = new String[MAX_DISPLAY];
        for (int i = 0; i < displayNumbers.length; i++) {
            displayNumbers[i] = Integer.toString(i + 1);
        }

        Spinner spinner = new Spinner(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, displayNumbers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedDisplay - MIN_DISPLAY);
        row.addView(spinner, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
        return spinner;
    }

    @Override
    protected void showBindingDialog() {
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        TextView explanation = new TextView(getContext());
        explanation.setText(R.string.osc_display_switch_summary);
        explanation.setPadding(0, 0, 0, dp(8));
        content.addView(explanation);

        Spinner firstSpinner = addDisplaySpinner(content,
                R.string.osc_display_switch_start, firstDisplay);
        Spinner secondSpinner = addDisplaySpinner(content,
                R.string.osc_display_switch_other, secondDisplay);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_display_switch)
                .setView(content)
                .setPositiveButton(R.string.osc_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_display_switch, (chooser, which) ->
                        virtualController.removeDisplaySwitchButton(DisplaySwitchButton.this))
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    int selectedFirst = firstSpinner.getSelectedItemPosition() + MIN_DISPLAY;
                    int selectedSecond = secondSpinner.getSelectedItemPosition() + MIN_DISPLAY;
                    if (selectedFirst == selectedSecond) {
                        Toast.makeText(getContext(), R.string.osc_display_switch_distinct,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    releaseInput();
                    firstDisplay = selectedFirst;
                    secondDisplay = selectedSecond;
                    nextDisplay = secondDisplay;
                    updateLabel();
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    @Override
    public void releaseInput() {
        setInputDown(false);
        setPressed(false);
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("DISPLAY_SWITCH", true);
        configuration.put("FIRST_DISPLAY", firstDisplay);
        configuration.put("SECOND_DISPLAY", secondDisplay);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        firstDisplay = clampDisplay(configuration.optInt("FIRST_DISPLAY", 1));
        secondDisplay = clampDisplay(configuration.optInt("SECOND_DISPLAY", 2));
        if (firstDisplay == secondDisplay) {
            secondDisplay = firstDisplay == MAX_DISPLAY ? MIN_DISPLAY : firstDisplay + 1;
        }
        nextDisplay = secondDisplay;
        activeDisplay = 0;
        updateLabel();
    }

    private static int clampDisplay(int display) {
        return Math.max(MIN_DISPLAY, Math.min(MAX_DISPLAY, display));
    }
}
