package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.limelight.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

/** Maps an analog touch stick to four independently configurable keyboard directions. */
@SuppressLint("ViewConstructor")
public class KeyboardAnalogStick extends AnalogStick {
    private static final float PRESS_THRESHOLD = 0.34f;
    private static final String MAPPING_WASD = "wasd";
    private static final String MAPPING_ARROWS = "arrows";
    private static final String MAPPING_CUSTOM = "custom";

    private MappedInputButton.Binding upBinding = requireBinding("key_w");
    private MappedInputButton.Binding downBinding = requireBinding("key_s");
    private MappedInputButton.Binding leftBinding = requireBinding("key_a");
    private MappedInputButton.Binding rightBinding = requireBinding("key_d");
    private boolean upDown;
    private boolean downDown;
    private boolean leftDown;
    private boolean rightDown;

    public KeyboardAnalogStick(VirtualController controller, Context context, int elementId) {
        super(controller, context, elementId);

        addAnalogStickListener(new AnalogStickListener() {
            @Override
            public void onMovement(float x, float y) {
                updateKeyStates(x, y);
            }

            @Override public void onClick() { }
            @Override public void onDoubleClick() { }

            @Override
            public void onRevoke() {
                releaseInput();
            }
        });
    }

    private static MappedInputButton.Binding requireBinding(String id) {
        MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(id);
        if (binding == null || binding.kind != MappedInputButton.Kind.KEYBOARD) {
            throw new IllegalArgumentException("Unknown keyboard binding: " + id);
        }
        return binding;
    }

    private static MappedInputButton.Binding loadBinding(JSONObject configuration,
                                                          String key,
                                                          MappedInputButton.Binding fallback) {
        MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(
                configuration.optString(key, fallback.id));
        return binding != null && binding.kind == MappedInputButton.Kind.KEYBOARD ?
                binding : fallback;
    }

    private void setPreset(String preset) {
        if (MAPPING_ARROWS.equals(preset)) {
            upBinding = requireBinding("key_up");
            downBinding = requireBinding("key_down");
            leftBinding = requireBinding("key_left");
            rightBinding = requireBinding("key_right");
        }
        else {
            upBinding = requireBinding("key_w");
            downBinding = requireBinding("key_s");
            leftBinding = requireBinding("key_a");
            rightBinding = requireBinding("key_d");
        }
    }

    private void updateKeyStates(float x, float y) {
        boolean nextLeft = x < -PRESS_THRESHOLD;
        boolean nextRight = x > PRESS_THRESHOLD;
        boolean nextUp = y > PRESS_THRESHOLD;
        boolean nextDown = y < -PRESS_THRESHOLD;

        leftDown = sendIfChanged(leftDown, nextLeft, leftBinding.code);
        rightDown = sendIfChanged(rightDown, nextRight, rightBinding.code);
        upDown = sendIfChanged(upDown, nextUp, upBinding.code);
        downDown = sendIfChanged(downDown, nextDown, downBinding.code);
    }

    private boolean sendIfChanged(boolean oldState, boolean newState, int keyCode) {
        if (oldState != newState) {
            virtualController.mappedKeyboardEvent(keyCode, newState);
        }
        return newState;
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Spinner addDirectionSpinner(LinearLayout parent, int labelResource,
                                        MappedInputButton.Binding[] choices,
                                        MappedInputButton.Binding selectedBinding) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView label = new TextView(getContext());
        label.setText(labelResource);
        label.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String[] labels = new String[choices.length];
        int selectedIndex = 0;
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i].label;
            if (choices[i].id.equals(selectedBinding.id)) {
                selectedIndex = i;
            }
        }

        Spinner spinner = new Spinner(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
        row.addView(spinner, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));

        parent.addView(row);
        return spinner;
    }

    @Override
    protected void showBindingDialog() {
        if (BindingEditorDialog.usesFullKeyboard(getContext())) {
            showKeyboardBindingDialog();
        }
        else {
            showListBindingDialog();
        }
    }

    private Button addDirectionButton(LinearLayout parent, int labelResource,
                                      MappedInputButton.Binding[] choices,
                                      MappedInputButton.Binding[] pendingBindings, int index) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView label = new TextView(getContext());
        label.setText(labelResource);
        label.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(pendingBindings[index].label);
        button.setOnClickListener(view -> BindingEditorDialog.show(getContext(),
                getContext().getString(R.string.osc_binding_title,
                        getContext().getString(labelResource)),
                choices, Collections.singletonList(pendingBindings[index]), true, true,
                savedBindings -> {
                    pendingBindings[index] = savedBindings.get(0);
                    button.setText(pendingBindings[index].label);
                }, null));
        row.addView(button, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));
        parent.addView(row);
        return button;
    }

    private void showKeyboardBindingDialog() {
        MappedInputButton.Binding[] choices = MappedInputButton.getKeyboardBindings();
        MappedInputButton.Binding[] pendingBindings = {
                upBinding, downBinding, leftBinding, rightBinding
        };

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);
        addDirectionButton(content, R.string.osc_direction_up, choices, pendingBindings, 0);
        addDirectionButton(content, R.string.osc_direction_down, choices, pendingBindings, 1);
        addDirectionButton(content, R.string.osc_direction_left, choices, pendingBindings, 2);
        addDirectionButton(content, R.string.osc_direction_right, choices, pendingBindings, 3);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_keyboard_stick)
                .setView(content)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    releaseInput();
                    upBinding = pendingBindings[0];
                    downBinding = pendingBindings[1];
                    leftBinding = pendingBindings[2];
                    rightBinding = pendingBindings[3];
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_stick, (dialog, which) ->
                        virtualController.removeVirtualStick(KeyboardAnalogStick.this))
                .show();
    }

    private void showListBindingDialog() {
        MappedInputButton.Binding[] choices = MappedInputButton.getKeyboardBindings();
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        Spinner upSpinner = addDirectionSpinner(content, R.string.osc_direction_up,
                choices, upBinding);
        Spinner downSpinner = addDirectionSpinner(content, R.string.osc_direction_down,
                choices, downBinding);
        Spinner leftSpinner = addDirectionSpinner(content, R.string.osc_direction_left,
                choices, leftBinding);
        Spinner rightSpinner = addDirectionSpinner(content, R.string.osc_direction_right,
                choices, rightBinding);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_keyboard_stick)
                .setView(content)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    releaseInput();
                    upBinding = choices[upSpinner.getSelectedItemPosition()];
                    downBinding = choices[downSpinner.getSelectedItemPosition()];
                    leftBinding = choices[leftSpinner.getSelectedItemPosition()];
                    rightBinding = choices[rightSpinner.getSelectedItemPosition()];
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_stick, (dialog, which) ->
                        virtualController.removeVirtualStick(KeyboardAnalogStick.this))
                .show();
    }

    @Override
    public void releaseInput() {
        if (leftDown) virtualController.mappedKeyboardEvent(leftBinding.code, false);
        if (rightDown) virtualController.mappedKeyboardEvent(rightBinding.code, false);
        if (upDown) virtualController.mappedKeyboardEvent(upBinding.code, false);
        if (downDown) virtualController.mappedKeyboardEvent(downBinding.code, false);
        leftDown = rightDown = upDown = downDown = false;
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("UP_BINDING", upBinding.id);
        configuration.put("DOWN_BINDING", downBinding.id);
        configuration.put("LEFT_BINDING", leftBinding.id);
        configuration.put("RIGHT_BINDING", rightBinding.id);

        String legacyMapping = MAPPING_CUSTOM;
        if ("key_w".equals(upBinding.id) && "key_s".equals(downBinding.id) &&
                "key_a".equals(leftBinding.id) && "key_d".equals(rightBinding.id)) {
            legacyMapping = MAPPING_WASD;
        }
        else if ("key_up".equals(upBinding.id) && "key_down".equals(downBinding.id) &&
                "key_left".equals(leftBinding.id) && "key_right".equals(rightBinding.id)) {
            legacyMapping = MAPPING_ARROWS;
        }
        configuration.put("KEY_MAPPING", legacyMapping);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        if (configuration.has("UP_BINDING")) {
            upBinding = loadBinding(configuration, "UP_BINDING", upBinding);
            downBinding = loadBinding(configuration, "DOWN_BINDING", downBinding);
            leftBinding = loadBinding(configuration, "LEFT_BINDING", leftBinding);
            rightBinding = loadBinding(configuration, "RIGHT_BINDING", rightBinding);
        }
        else {
            setPreset(configuration.optString("KEY_MAPPING", MAPPING_WASD));
        }
    }
}
