package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.touch.BlankAreaInputSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Toggles uncovered stream areas between independently configured mouse and shooting modes. */
@SuppressLint("ViewConstructor")
public class BlankAreaModeButton extends DigitalButton {
    private static final String MODE_MOUSE = "mouse";
    private static final String MODE_SHOOTING = "shooting";

    private final List<MappedInputButton.Binding> toggleBindings = new ArrayList<>();
    private String activeMode = MODE_SHOOTING;
    private int mouseSensitivityPercent;
    private int shootingSensitivityPercent;
    private boolean mouseLeftClick = true;
    private boolean shootingLeftClick;
    private boolean inputDown;

    public BlankAreaModeButton(VirtualController controller, int elementId, int layer,
                               Context context) {
        super(controller, elementId, layer, context);
        int legacySensitivity = BlankAreaInputSettings.getLegacySensitivityPercent(context);
        mouseSensitivityPercent = legacySensitivity;
        shootingSensitivityPercent = legacySensitivity;
        updateLabel();

        addDigitalButtonListener(new DigitalButtonListener() {
            @Override
            public void onClick() {
                beginModeSwitch();
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                endModeSwitch();
            }
        });
    }

    private boolean isMouseMode() {
        return MODE_MOUSE.equals(activeMode);
    }

    private void updateLabel() {
        setText(getContext().getString(isMouseMode() ?
                R.string.osc_blank_mode_mouse_label : R.string.osc_blank_mode_shooting_label));
        setContentDescription(getContext().getString(R.string.osc_blank_mode_button_description,
                getContext().getString(isMouseMode() ?
                        R.string.osc_blank_mode_mouse : R.string.osc_blank_mode_shooting)));
    }

    private String getBindingSummary(List<MappedInputButton.Binding> bindings) {
        if (bindings.isEmpty()) {
            return getContext().getString(R.string.osc_binding_none);
        }

        StringBuilder summary = new StringBuilder();
        for (MappedInputButton.Binding binding : bindings) {
            if (summary.length() != 0) summary.append('+');
            summary.append(binding.label);
        }
        return summary.toString();
    }

    void applyActiveSettings() {
        BlankAreaInputSettings.apply(getContext(),
                isMouseMode() ? mouseSensitivityPercent : shootingSensitivityPercent,
                isMouseMode() ? mouseLeftClick : shootingLeftClick);
        updateLabel();
    }

    private void beginModeSwitch() {
        if (inputDown) return;
        inputDown = true;
        activeMode = isMouseMode() ? MODE_SHOOTING : MODE_MOUSE;
        applyActiveSettings();
        MappedInputButton.emitBindings(virtualController, toggleBindings, true);
        VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
        Toast.makeText(getContext(), getContext().getString(R.string.osc_blank_mode_switched,
                        getContext().getString(isMouseMode() ?
                                R.string.osc_blank_mode_mouse : R.string.osc_blank_mode_shooting)),
                Toast.LENGTH_SHORT).show();
    }

    private void endModeSwitch() {
        if (!inputDown) return;
        MappedInputButton.emitBindings(virtualController, toggleBindings, false);
        inputDown = false;
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateSensitivityLabel(TextView label, int modeNameResource, int percent) {
        label.setText(getContext().getString(R.string.osc_blank_mode_sensitivity,
                getContext().getString(modeNameResource), percent));
    }

    private SeekBar addModeSettings(LinearLayout parent, int modeNameResource,
                                    int sensitivityPercent, boolean allowLeftClick,
                                    List<CheckBox> clickCheckBoxes) {
        TextView title = new TextView(getContext());
        title.setText(modeNameResource);
        title.setTextSize(18);
        title.setPadding(0, dp(10), 0, dp(2));
        parent.addView(title);

        TextView sensitivityLabel = new TextView(getContext());
        updateSensitivityLabel(sensitivityLabel, modeNameResource, sensitivityPercent);
        parent.addView(sensitivityLabel);

        SeekBar sensitivityBar = new SeekBar(getContext());
        sensitivityBar.setMax(BlankAreaInputSettings.MAX_SENSITIVITY_PERCENT -
                BlankAreaInputSettings.MIN_SENSITIVITY_PERCENT);
        sensitivityBar.setProgress(sensitivityPercent -
                BlankAreaInputSettings.MIN_SENSITIVITY_PERCENT);
        sensitivityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityLabel(sensitivityLabel, modeNameResource,
                        progress + BlankAreaInputSettings.MIN_SENSITIVITY_PERCENT);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        parent.addView(sensitivityBar);

        CheckBox clickCheckBox = new CheckBox(getContext());
        clickCheckBox.setText(R.string.osc_blank_mode_allow_left_click);
        clickCheckBox.setChecked(allowLeftClick);
        parent.addView(clickCheckBox);
        clickCheckBoxes.add(clickCheckBox);
        return sensitivityBar;
    }

    @Override
    protected void showBindingDialog() {
        List<MappedInputButton.Binding> pendingBindings = new ArrayList<>(toggleBindings);
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        TextView explanation = new TextView(getContext());
        explanation.setText(R.string.osc_blank_mode_summary);
        content.addView(explanation);

        TextView bindingLabel = new TextView(getContext());
        bindingLabel.setText(R.string.osc_blank_mode_binding);
        bindingLabel.setPadding(0, dp(10), 0, dp(2));
        content.addView(bindingLabel);

        Button bindingButton = new Button(getContext());
        bindingButton.setAllCaps(false);
        bindingButton.setText(getBindingSummary(pendingBindings));
        bindingButton.setOnClickListener(view -> BindingEditorDialog.show(getContext(),
                getContext().getString(R.string.osc_blank_mode_binding_title),
                MappedInputButton.getBindings(), pendingBindings, false, false,
                savedBindings -> {
                    pendingBindings.clear();
                    pendingBindings.addAll(savedBindings);
                    bindingButton.setText(getBindingSummary(pendingBindings));
                }, null));
        content.addView(bindingButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        List<CheckBox> clickCheckBoxes = new ArrayList<>();
        SeekBar mouseSensitivity = addModeSettings(content, R.string.osc_blank_mode_mouse,
                mouseSensitivityPercent, mouseLeftClick, clickCheckBoxes);
        SeekBar shootingSensitivity = addModeSettings(content, R.string.osc_blank_mode_shooting,
                shootingSensitivityPercent, shootingLeftClick, clickCheckBoxes);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(content);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_blank_mode_button)
                .setView(scrollView)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    endModeSwitch();
                    toggleBindings.clear();
                    toggleBindings.addAll(pendingBindings);
                    mouseSensitivityPercent = mouseSensitivity.getProgress() +
                            BlankAreaInputSettings.MIN_SENSITIVITY_PERCENT;
                    shootingSensitivityPercent = shootingSensitivity.getProgress() +
                            BlankAreaInputSettings.MIN_SENSITIVITY_PERCENT;
                    mouseLeftClick = clickCheckBoxes.get(0).isChecked();
                    shootingLeftClick = clickCheckBoxes.get(1).isChecked();
                    applyActiveSettings();
                    VirtualControllerConfigurationLoader.saveProfile(virtualController,
                            getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_blank_mode_button, (dialog, which) ->
                        virtualController.removeBlankAreaModeButton(BlankAreaModeButton.this))
                .show();
    }

    @Override
    public void releaseInput() {
        endModeSwitch();
        setPressed(false);
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("BLANK_AREA_MODE_BUTTON", true);
        configuration.put("ACTIVE_MODE", activeMode);
        configuration.put("MOUSE_SENSITIVITY", mouseSensitivityPercent);
        configuration.put("SHOOTING_SENSITIVITY", shootingSensitivityPercent);
        configuration.put("MOUSE_LEFT_CLICK", mouseLeftClick);
        configuration.put("SHOOTING_LEFT_CLICK", shootingLeftClick);

        JSONArray bindingIds = new JSONArray();
        for (MappedInputButton.Binding binding : toggleBindings) {
            bindingIds.put(binding.id);
        }
        configuration.put("TOGGLE_BINDINGS", bindingIds);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        activeMode = MODE_SHOOTING.equals(configuration.optString("ACTIVE_MODE", activeMode)) ?
                MODE_SHOOTING : MODE_MOUSE;
        mouseSensitivityPercent = BlankAreaInputSettings.clampSensitivity(
                configuration.optInt("MOUSE_SENSITIVITY", mouseSensitivityPercent));
        shootingSensitivityPercent = BlankAreaInputSettings.clampSensitivity(
                configuration.optInt("SHOOTING_SENSITIVITY", shootingSensitivityPercent));
        mouseLeftClick = configuration.optBoolean("MOUSE_LEFT_CLICK", true);
        shootingLeftClick = configuration.optBoolean("SHOOTING_LEFT_CLICK", false);

        toggleBindings.clear();
        JSONArray bindingIds = configuration.optJSONArray("TOGGLE_BINDINGS");
        if (bindingIds != null) {
            for (int i = 0; i < bindingIds.length(); i++) {
                MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(
                        bindingIds.optString(i));
                if (binding != null) toggleBindings.add(binding);
            }
        }
        inputDown = false;
        applyActiveSettings();
    }
}
