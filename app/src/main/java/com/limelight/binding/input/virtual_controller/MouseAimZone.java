package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A free-swipe aiming surface that sends relative mouse movement and can hold a configurable input
 * combination while touched.
 */
@SuppressLint("ViewConstructor")
public class MouseAimZone extends VirtualControllerElement {
    private static final int MIN_SENSITIVITY_PERCENT = 25;
    private static final int MAX_SENSITIVITY_PERCENT = 300;

    private final EvdevListener inputSink;
    private final Paint paint = new Paint();
    private final RectF outline = new RectF();
    private final List<MappedInputButton.Binding> holdBindings = new ArrayList<>();

    private float sensitivity = 1f;
    private boolean invertX;
    private boolean invertY;
    private boolean holdDown;
    private float lastX;
    private float lastY;
    private float pendingDeltaX;
    private float pendingDeltaY;

    public MouseAimZone(VirtualController controller, Context context, int elementId) {
        super(controller, context, elementId);
        inputSink = controller.getInputSink();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);

        float strokeWidth = getDefaultStrokeWidth();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(isPressed() ? pressedColor : getDefaultColor());
        outline.set(strokeWidth, strokeWidth, getWidth() - strokeWidth, getHeight() - strokeWidth);
        canvas.drawRoundRect(outline, dp(12), dp(12), paint);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float crosshairRadius = Math.min(getWidth(), getHeight()) * 0.055f;
        canvas.drawCircle(centerX, centerY, crosshairRadius, paint);
        canvas.drawLine(centerX - crosshairRadius * 1.6f, centerY,
                centerX + crosshairRadius * 1.6f, centerY, paint);
        canvas.drawLine(centerX, centerY - crosshairRadius * 1.6f,
                centerX, centerY + crosshairRadius * 1.6f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.min(getWidth(), getHeight()) * 0.07f);
        canvas.drawText(getContext().getString(R.string.osc_aim_zone_label), centerX,
                strokeWidth + paint.getTextSize() * 1.2f, paint);
    }

    private void setHoldDown(boolean down) {
        if (holdDown == down) {
            return;
        }

        holdDown = down;
        if (virtualController.getInputSink() != null) {
            MappedInputButton.emitBindings(virtualController, holdBindings, down);
        }
    }

    private void movePointer(float x, float y) {
        float xDirection = invertX ? -1f : 1f;
        float yDirection = invertY ? -1f : 1f;
        pendingDeltaX += (x - lastX) * sensitivity * xDirection;
        pendingDeltaY += (y - lastY) * sensitivity * yDirection;
        lastX = x;
        lastY = y;

        int deltaX = Math.round(pendingDeltaX);
        int deltaY = Math.round(pendingDeltaY);
        pendingDeltaX -= deltaX;
        pendingDeltaY -= deltaY;
        if (inputSink != null && (deltaX != 0 || deltaY != 0)) {
            inputSink.mouseMove(deltaX, deltaY);
        }
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                pendingDeltaX = pendingDeltaY = 0f;
                setPressed(true);
                setHoldDown(true);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getHistorySize(); i++) {
                    movePointer(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                movePointer(event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                releaseInput();
                return true;

            default:
                return true;
        }
    }

    private void updateSensitivityLabel(TextView label, int percent) {
        label.setText(getContext().getString(R.string.osc_mouse_sensitivity, percent));
    }

    private String getHoldSummary(List<MappedInputButton.Binding> selected) {
        StringBuilder summary = new StringBuilder();
        for (MappedInputButton.Binding binding : selected) {
            if (summary.length() != 0) {
                summary.append('+');
            }
            summary.append(binding.label);
        }
        return summary.length() == 0 ? getContext().getString(R.string.osc_no_hold_input) :
                summary.toString();
    }

    private void showHoldBindingDialog(List<MappedInputButton.Binding> selected,
                                       Button bindingButton) {
        BindingEditorDialog.show(getContext(),
                getContext().getString(R.string.osc_aim_hold_binding_title),
                MappedInputButton.getBindings(), selected, false, false,
                savedBindings -> {
                    selected.clear();
                    selected.addAll(savedBindings);
                    bindingButton.setText(getHoldSummary(selected));
                }, null);
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

        TextView holdInputLabel = new TextView(getContext());
        holdInputLabel.setText(R.string.osc_aim_hold_binding);
        holdInputLabel.setPadding(0, dp(8), 0, 0);
        content.addView(holdInputLabel);

        List<MappedInputButton.Binding> selectedBindings = new ArrayList<>(holdBindings);

        Button bindingButton = new Button(getContext());
        bindingButton.setAllCaps(false);
        bindingButton.setText(getHoldSummary(selectedBindings));
        bindingButton.setOnClickListener(view -> showHoldBindingDialog(
                selectedBindings, bindingButton));
        content.addView(bindingButton);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(content);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_aim_zone)
                .setView(scrollView)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    releaseInput();
                    sensitivity = (sensitivityBar.getProgress() + MIN_SENSITIVITY_PERCENT) / 100f;
                    invertX = invertXCheckBox.isChecked();
                    invertY = invertYCheckBox.isChecked();
                    holdBindings.clear();
                    holdBindings.addAll(selectedBindings);
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_aim_zone, (dialog, which) ->
                        virtualController.removeAimZone(MouseAimZone.this))
                .show();
    }

    @Override
    public void releaseInput() {
        setHoldDown(false);
        setPressed(false);
        pendingDeltaX = pendingDeltaY = 0f;
        invalidate();
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("AIM_ZONE", true);
        configuration.put("MOUSE_SENSITIVITY", sensitivity);
        configuration.put("INVERT_X", invertX);
        configuration.put("INVERT_Y", invertY);
        JSONArray bindingIds = new JSONArray();
        for (MappedInputButton.Binding binding : holdBindings) {
            bindingIds.put(binding.id);
        }
        configuration.put("HOLD_BINDINGS", bindingIds);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        // A v0.3 profile used this element ID for the old mouse stick. Ignore its small square
        // geometry so upgrades receive the full default aiming area automatically.
        if (configuration.optBoolean("AIM_ZONE", false)) {
            super.loadConfiguration(configuration);
        }

        sensitivity = (float) configuration.optDouble("MOUSE_SENSITIVITY", 1.0);
        sensitivity = Math.max(MIN_SENSITIVITY_PERCENT / 100f,
                Math.min(MAX_SENSITIVITY_PERCENT / 100f, sensitivity));
        invertX = configuration.optBoolean("INVERT_X", false);
        invertY = configuration.optBoolean("INVERT_Y", false);

        holdBindings.clear();
        JSONArray bindingIds = configuration.optJSONArray("HOLD_BINDINGS");
        if (bindingIds != null) {
            for (int i = 0; i < bindingIds.length(); i++) {
                MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(
                        bindingIds.optString(i));
                if (binding != null) {
                    holdBindings.add(binding);
                }
            }
        }
    }
}
