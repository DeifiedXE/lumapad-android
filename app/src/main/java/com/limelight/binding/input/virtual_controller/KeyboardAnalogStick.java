package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;

import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevListener;

import org.json.JSONException;
import org.json.JSONObject;

/** Maps an analog touch stick to four keyboard directions, including diagonals. */
@SuppressLint("ViewConstructor")
public class KeyboardAnalogStick extends AnalogStick {
    private static final float PRESS_THRESHOLD = 0.34f;
    private static final String MAPPING_WASD = "wasd";
    private static final String MAPPING_ARROWS = "arrows";

    private final EvdevListener inputSink;
    private String mapping = MAPPING_WASD;
    private boolean upDown;
    private boolean downDown;
    private boolean leftDown;
    private boolean rightDown;

    public KeyboardAnalogStick(VirtualController controller, Context context, int elementId) {
        super(controller, context, elementId);
        inputSink = controller.getInputSink();

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

    private short keyCode(int wasd, int arrows) {
        return (short) (MAPPING_WASD.equals(mapping) ? wasd : arrows);
    }

    private void updateKeyStates(float x, float y) {
        boolean nextLeft = x < -PRESS_THRESHOLD;
        boolean nextRight = x > PRESS_THRESHOLD;
        boolean nextUp = y > PRESS_THRESHOLD;
        boolean nextDown = y < -PRESS_THRESHOLD;

        leftDown = sendIfChanged(leftDown, nextLeft,
                keyCode(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT));
        rightDown = sendIfChanged(rightDown, nextRight,
                keyCode(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT));
        upDown = sendIfChanged(upDown, nextUp,
                keyCode(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP));
        downDown = sendIfChanged(downDown, nextDown,
                keyCode(KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN));
    }

    private boolean sendIfChanged(boolean oldState, boolean newState, short keyCode) {
        if (oldState != newState && inputSink != null) {
            inputSink.keyboardEvent(newState, keyCode);
        }
        return newState;
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    @Override
    protected void showBindingDialog() {
        String[] choices = new String[] { "WASD", "Arrow keys" };
        int selected = MAPPING_WASD.equals(mapping) ? 0 : 1;
        new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.osc_binding_title, "MOVE"))
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    releaseInput();
                    mapping = which == 0 ? MAPPING_WASD : MAPPING_ARROWS;
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void releaseInput() {
        if (inputSink != null) {
            if (leftDown) inputSink.keyboardEvent(false, keyCode(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT));
            if (rightDown) inputSink.keyboardEvent(false, keyCode(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT));
            if (upDown) inputSink.keyboardEvent(false, keyCode(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP));
            if (downDown) inputSink.keyboardEvent(false, keyCode(KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN));
        }
        leftDown = rightDown = upDown = downDown = false;
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("KEY_MAPPING", mapping);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        String savedMapping = configuration.optString("KEY_MAPPING", MAPPING_WASD);
        mapping = MAPPING_ARROWS.equals(savedMapping) ? MAPPING_ARROWS : MAPPING_WASD;
    }
}
