package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A configurable velocity-based mouse stick. Holding the stick away from center continuously moves
 * the cursor.
 */
@SuppressLint("ViewConstructor")
public class MouseAnalogStick extends AnalogStick {
    private static final long FRAME_DELAY_MS = 16;
    private static final float MAX_PIXELS_PER_FRAME = 28f;
    private static final int MIN_SENSITIVITY_PERCENT = 25;
    private static final int MAX_SENSITIVITY_PERCENT = 300;

    private final EvdevListener inputSink;
    private float velocityX;
    private float velocityY;
    private float sensitivity = 1f;
    private boolean invertX;
    private boolean invertY;
    private boolean running;

    private final Runnable mouseMoveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            float xDirection = invertX ? -1f : 1f;
            float yDirection = invertY ? -1f : 1f;
            int deltaX = Math.round(velocityX * MAX_PIXELS_PER_FRAME * sensitivity * xDirection);
            int deltaY = Math.round(-velocityY * MAX_PIXELS_PER_FRAME * sensitivity * yDirection);
            if (inputSink != null && (deltaX != 0 || deltaY != 0)) {
                inputSink.mouseMove(deltaX, deltaY);
            }
            virtualController.getHandler().postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public MouseAnalogStick(VirtualController controller, Context context, int elementId) {
        super(controller, context, elementId);
        inputSink = controller.getInputSink();

        addAnalogStickListener(new AnalogStickListener() {
            @Override
            public void onMovement(float x, float y) {
                velocityX = x;
                velocityY = y;
                if (!running && (x != 0f || y != 0f)) {
                    running = true;
                    virtualController.getHandler().post(mouseMoveRunnable);
                }
            }

            @Override public void onClick() { }
            @Override public void onDoubleClick() { }

            @Override
            public void onRevoke() {
                releaseInput();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateSensitivityLabel(TextView label, int percent) {
        label.setText(getContext().getString(R.string.osc_mouse_sensitivity, percent));
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    @Override
    protected void showBindingDialog() {
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        TextView sensitivityLabel = new TextView(getContext());
        int currentPercent = Math.round(sensitivity * 100f);
        updateSensitivityLabel(sensitivityLabel, currentPercent);
        content.addView(sensitivityLabel);

        SeekBar sensitivityBar = new SeekBar(getContext());
        sensitivityBar.setMax(MAX_SENSITIVITY_PERCENT - MIN_SENSITIVITY_PERCENT);
        sensitivityBar.setProgress(currentPercent - MIN_SENSITIVITY_PERCENT);
        sensitivityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityLabel(sensitivityLabel, progress + MIN_SENSITIVITY_PERCENT);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        content.addView(sensitivityBar);

        CheckBox invertXCheckBox = new CheckBox(getContext());
        invertXCheckBox.setText(R.string.osc_invert_mouse_x);
        invertXCheckBox.setChecked(invertX);
        content.addView(invertXCheckBox);

        CheckBox invertYCheckBox = new CheckBox(getContext());
        invertYCheckBox.setText(R.string.osc_invert_mouse_y);
        invertYCheckBox.setChecked(invertY);
        content.addView(invertYCheckBox);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_mouse_stick)
                .setView(content)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    releaseInput();
                    sensitivity = (sensitivityBar.getProgress() + MIN_SENSITIVITY_PERCENT) / 100f;
                    invertX = invertXCheckBox.isChecked();
                    invertY = invertYCheckBox.isChecked();
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_stick, (dialog, which) ->
                        virtualController.removeVirtualStick(MouseAnalogStick.this))
                .show();
    }

    @Override
    public void releaseInput() {
        running = false;
        velocityX = velocityY = 0f;
        virtualController.getHandler().removeCallbacks(mouseMoveRunnable);
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("MOUSE_SENSITIVITY", sensitivity);
        configuration.put("INVERT_X", invertX);
        configuration.put("INVERT_Y", invertY);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        sensitivity = (float) configuration.optDouble("MOUSE_SENSITIVITY", 1.0);
        sensitivity = Math.max(MIN_SENSITIVITY_PERCENT / 100f,
                Math.min(MAX_SENSITIVITY_PERCENT / 100f, sensitivity));
        invertX = configuration.optBoolean("INVERT_X", false);
        invertY = configuration.optBoolean("INVERT_Y", false);
    }
}
