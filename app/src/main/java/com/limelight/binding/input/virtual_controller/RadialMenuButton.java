package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A compact button that expands into a configurable gesture-driven radial input menu. */
@SuppressLint("ViewConstructor")
public class RadialMenuButton extends VirtualControllerElement {
    private static final int MIN_SECTORS = 2;
    private static final int MAX_SECTORS = 12;
    private static final int MIN_DEAD_ZONE_PERCENT = 15;
    private static final int MAX_DEAD_ZONE_PERCENT = 60;
    private static final int MAX_HYSTERESIS_DEGREES = 20;
    private static final float EXPANDED_RADIUS_MULTIPLIER = 2.7f;

    private static final class RadialItem {
        final List<MappedInputButton.Binding> bindings = new ArrayList<>();

        RadialItem(MappedInputButton.Binding binding) {
            bindings.add(binding);
        }

        RadialItem(RadialItem source) {
            bindings.addAll(source.bindings);
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final List<RadialItem> items = new ArrayList<>();
    private final List<MappedInputButton.Binding> heldBindings = new ArrayList<>();

    private float centerDeadZone = 0.30f;
    private float hysteresisDegrees = 8f;
    private boolean holdSelectedInput;
    private boolean selectionHaptics = true;
    private boolean hapticsEnabledForGesture;
    private boolean expanded;
    private int selectedIndex = -1;

    private int collapsedLeft;
    private int collapsedTop;
    private int collapsedWidth;
    private int collapsedHeight;
    private float centerRawX;
    private float centerRawY;
    private float expandedRadius;

    public RadialMenuButton(VirtualController controller, int elementId, Context context) {
        super(controller, context, elementId);
        addDefaultItem("key_i");
        addDefaultItem("key_m");
        addDefaultItem("key_c");
        addDefaultItem("key_p");
        addDefaultItem("key_tab");
        addDefaultItem("key_escape");
        addDefaultItem("key_f1");
        addDefaultItem("key_f2");
    }

    private void addDefaultItem(String bindingId) {
        MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(bindingId);
        if (binding == null) {
            binding = MappedInputButton.findBindingOrNull("key_space");
        }
        items.add(new RadialItem(binding));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getBindingSummary(List<MappedInputButton.Binding> bindings) {
        StringBuilder summary = new StringBuilder();
        for (MappedInputButton.Binding binding : bindings) {
            if (summary.length() != 0) summary.append('+');
            summary.append(binding.label);
        }
        return summary.toString();
    }

    private String getShortBindingSummary(List<MappedInputButton.Binding> bindings) {
        String summary = getBindingSummary(bindings);
        return summary.length() <= 10 ? summary : summary.substring(0, 9) + "…";
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        float strokeWidth = getDefaultStrokeWidth();

        if (!expanded) {
            float radius = Math.min(getWidth(), getHeight()) / 2f - strokeWidth;
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(isPressed() ? pressedColor : getDefaultColor());
            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.min(getWidth(), getHeight()) * 0.21f);
            canvas.drawText(getContext().getString(R.string.osc_radial_label),
                    centerX, centerY - (paint.ascent() + paint.descent()) / 2f, paint);
            return;
        }

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - strokeWidth;
        float sweep = 360f / items.size();
        arcBounds.set(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);

        for (int i = 0; i < items.size(); i++) {
            float startAngle = -90f - sweep / 2f + i * sweep;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == selectedIndex ? pressedColor : getDefaultColor());
            paint.setAlpha(i == selectedIndex ? 220 : 165);
            canvas.drawArc(arcBounds, startAngle, sweep, true, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(Color.WHITE);
            paint.setAlpha(180);
            canvas.drawArc(arcBounds, startAngle, sweep, true, paint);

            double labelAngle = Math.toRadians(-90f + i * sweep);
            float labelRadius = radius * 0.69f;
            float labelX = centerX + (float) Math.cos(labelAngle) * labelRadius;
            float labelY = centerY + (float) Math.sin(labelAngle) * labelRadius;
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
            paint.setTextSize(Math.max(dp(9), radius * 0.105f));
            canvas.drawText(getShortBindingSummary(items.get(i).bindings), labelX,
                    labelY - (paint.ascent() + paint.descent()) / 2f, paint);
        }

        float deadRadius = radius * centerDeadZone;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(24, 26, 29));
        paint.setAlpha(245);
        canvas.drawCircle(centerX, centerY, deadRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(Color.WHITE);
        paint.setAlpha(200);
        canvas.drawCircle(centerX, centerY, deadRadius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(deadRadius * 0.72f);
        canvas.drawText("×", centerX,
                centerY - (paint.ascent() + paint.descent()) / 2f, paint);
    }

    private void expandMenu() {
        if (expanded) return;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        collapsedLeft = params.leftMargin;
        collapsedTop = params.topMargin;
        collapsedWidth = getWidth();
        collapsedHeight = getHeight();

        int[] screenLocation = new int[2];
        getLocationOnScreen(screenLocation);
        centerRawX = screenLocation[0] + collapsedWidth / 2f;
        centerRawY = screenLocation[1] + collapsedHeight / 2f;

        expandedRadius = Math.max(collapsedWidth, collapsedHeight) *
                EXPANDED_RADIUS_MULTIPLIER;
        int diameter = Math.round(expandedRadius * 2f);
        float centerInParentX = collapsedLeft + collapsedWidth / 2f;
        float centerInParentY = collapsedTop + collapsedHeight / 2f;
        params.leftMargin = Math.round(centerInParentX - expandedRadius);
        params.topMargin = Math.round(centerInParentY - expandedRadius);
        params.width = diameter;
        params.height = diameter;
        params.rightMargin = 0;
        params.bottomMargin = 0;

        expanded = true;
        selectedIndex = -1;
        hapticsEnabledForGesture = selectionHaptics &&
                PreferenceConfiguration.readPreferences(getContext()).vibrateOsc;
        setPressed(true);
        bringToFront();
        requestLayout();
        invalidate();
    }

    private void collapseMenu() {
        if (!expanded) return;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.leftMargin = collapsedLeft;
        params.topMargin = collapsedTop;
        params.width = collapsedWidth;
        params.height = collapsedHeight;
        params.rightMargin = 0;
        params.bottomMargin = 0;
        expanded = false;
        selectedIndex = -1;
        setPressed(false);
        requestLayout();
        invalidate();
    }

    private int getCandidateIndex(float rawX, float rawY) {
        float dx = rawX - centerRawX;
        float dy = rawY - centerRawY;
        float distance = (float) Math.hypot(dx, dy);
        if (distance < expandedRadius * centerDeadZone) {
            return -1;
        }

        float sweep = 360f / items.size();
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0f) angle += 360f;
        int candidate = ((int) Math.floor((angle + sweep / 2f) / sweep)) % items.size();

        if (selectedIndex >= 0 && candidate != selectedIndex) {
            float selectedCenter = selectedIndex * sweep;
            float difference = Math.abs(((angle - selectedCenter + 540f) % 360f) - 180f);
            if (difference <= sweep / 2f + hysteresisDegrees) {
                return selectedIndex;
            }
        }
        return candidate;
    }

    private void releaseHeldBindings() {
        if (!heldBindings.isEmpty()) {
            MappedInputButton.emitBindings(virtualController, heldBindings, false);
            heldBindings.clear();
        }
    }

    private void updateSelection(float rawX, float rawY) {
        int nextIndex = getCandidateIndex(rawX, rawY);
        if (nextIndex == selectedIndex) return;

        if (holdSelectedInput) {
            releaseHeldBindings();
        }
        selectedIndex = nextIndex;
        if (holdSelectedInput && selectedIndex >= 0) {
            heldBindings.addAll(items.get(selectedIndex).bindings);
            MappedInputButton.emitBindings(virtualController, heldBindings, true);
        }
        if (selectedIndex >= 0 && hapticsEnabledForGesture) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        invalidate();
    }

    private void pulseBindings(List<MappedInputButton.Binding> bindings) {
        List<MappedInputButton.Binding> pulse = new ArrayList<>(bindings);
        MappedInputButton.emitBindings(virtualController, pulse, true);
        virtualController.getHandler().postDelayed(() ->
                MappedInputButton.emitBindings(virtualController, pulse, false), 35);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                expandMenu();
                updateSelection(event.getRawX(), event.getRawY());
                return true;

            case MotionEvent.ACTION_MOVE:
                updateSelection(event.getRawX(), event.getRawY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                updateSelection(event.getRawX(), event.getRawY());
                List<MappedInputButton.Binding> selectedBindings = selectedIndex >= 0 ?
                        new ArrayList<>(items.get(selectedIndex).bindings) : null;
                releaseHeldBindings();
                collapseMenu();
                if (!holdSelectedInput && selectedBindings != null) {
                    pulseBindings(selectedBindings);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                releaseInput();
                return true;

            default:
                return true;
        }
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    private void updateDeadZoneLabel(TextView label, int percent) {
        label.setText(getContext().getString(R.string.osc_radial_dead_zone, percent));
    }

    private void updateHysteresisLabel(TextView label, int degrees) {
        label.setText(getContext().getString(R.string.osc_radial_hysteresis, degrees));
    }

    private void rebuildItemRows(LinearLayout rows, List<RadialItem> pendingItems,
                                 Button addButton) {
        rows.removeAllViews();
        MappedInputButton.Binding[] choices = MappedInputButton.getBindings();
        for (int i = 0; i < pendingItems.size(); i++) {
            int itemIndex = i;
            RadialItem item = pendingItems.get(i);
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(2), 0, dp(2));

            TextView number = new TextView(getContext());
            number.setText(getContext().getString(R.string.osc_radial_sector, i + 1));
            number.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.addView(number, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));

            Button bindingButton = new Button(getContext());
            bindingButton.setAllCaps(false);
            bindingButton.setText(getBindingSummary(item.bindings));
            bindingButton.setOnClickListener(view -> BindingEditorDialog.show(getContext(),
                    getContext().getString(R.string.osc_radial_choose_sector, itemIndex + 1),
                    choices, item.bindings, true, false,
                    savedBindings -> {
                        item.bindings.clear();
                        item.bindings.addAll(savedBindings);
                        bindingButton.setText(getBindingSummary(item.bindings));
                    }, null));
            row.addView(bindingButton, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 2f));

            Button removeButton = new Button(getContext());
            removeButton.setText("−");
            removeButton.setContentDescription(getContext().getString(
                    R.string.osc_radial_remove_sector, i + 1));
            removeButton.setEnabled(pendingItems.size() > MIN_SECTORS);
            removeButton.setOnClickListener(view -> {
                pendingItems.remove(itemIndex);
                rebuildItemRows(rows, pendingItems, addButton);
            });
            row.addView(removeButton, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0.55f));
            rows.addView(row);
        }
        addButton.setEnabled(pendingItems.size() < MAX_SECTORS);
    }

    @Override
    protected void showBindingDialog() {
        List<RadialItem> pendingItems = new ArrayList<>();
        for (RadialItem item : items) pendingItems.add(new RadialItem(item));

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        TextView deadZoneLabel = new TextView(getContext());
        int deadZonePercent = Math.round(centerDeadZone * 100f);
        updateDeadZoneLabel(deadZoneLabel, deadZonePercent);
        content.addView(deadZoneLabel);
        SeekBar deadZoneBar = new SeekBar(getContext());
        deadZoneBar.setMax(MAX_DEAD_ZONE_PERCENT - MIN_DEAD_ZONE_PERCENT);
        deadZoneBar.setProgress(deadZonePercent - MIN_DEAD_ZONE_PERCENT);
        deadZoneBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDeadZoneLabel(deadZoneLabel, progress + MIN_DEAD_ZONE_PERCENT);
            }
        });
        content.addView(deadZoneBar);

        TextView hysteresisLabel = new TextView(getContext());
        int currentHysteresis = Math.round(hysteresisDegrees);
        updateHysteresisLabel(hysteresisLabel, currentHysteresis);
        content.addView(hysteresisLabel);
        SeekBar hysteresisBar = new SeekBar(getContext());
        hysteresisBar.setMax(MAX_HYSTERESIS_DEGREES);
        hysteresisBar.setProgress(currentHysteresis);
        hysteresisBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateHysteresisLabel(hysteresisLabel, progress);
            }
        });
        content.addView(hysteresisBar);

        CheckBox hapticCheckBox = new CheckBox(getContext());
        hapticCheckBox.setText(R.string.osc_radial_haptics);
        hapticCheckBox.setChecked(selectionHaptics);
        content.addView(hapticCheckBox);

        CheckBox holdCheckBox = new CheckBox(getContext());
        holdCheckBox.setText(R.string.osc_radial_hold_mode);
        holdCheckBox.setChecked(holdSelectedInput);
        content.addView(holdCheckBox);

        TextView sectorTitle = new TextView(getContext());
        sectorTitle.setText(R.string.osc_radial_sectors);
        sectorTitle.setPadding(0, dp(8), 0, dp(4));
        content.addView(sectorTitle);

        LinearLayout rows = new LinearLayout(getContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        content.addView(rows);

        Button addButton = new Button(getContext());
        addButton.setAllCaps(false);
        addButton.setText(R.string.osc_radial_add_sector);
        addButton.setOnClickListener(view -> {
            MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull("key_space");
            pendingItems.add(new RadialItem(binding));
            rebuildItemRows(rows, pendingItems, addButton);
        });
        content.addView(addButton);
        rebuildItemRows(rows, pendingItems, addButton);

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(content);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.osc_edit_radial_menu)
                .setView(scrollView)
                .setPositiveButton(R.string.osc_save, (dialog, which) -> {
                    releaseInput();
                    centerDeadZone = (deadZoneBar.getProgress() +
                            MIN_DEAD_ZONE_PERCENT) / 100f;
                    hysteresisDegrees = hysteresisBar.getProgress();
                    selectionHaptics = hapticCheckBox.isChecked();
                    holdSelectedInput = holdCheckBox.isChecked();
                    items.clear();
                    for (RadialItem item : pendingItems) items.add(new RadialItem(item));
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                    invalidate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_radial_menu, (dialog, which) ->
                        virtualController.removeRadialMenuButton(RadialMenuButton.this))
                .show();
    }

    @Override
    public void releaseInput() {
        releaseHeldBindings();
        collapseMenu();
        setPressed(false);
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        if (expanded) {
            configuration.put("LEFT", collapsedLeft);
            configuration.put("TOP", collapsedTop);
            configuration.put("WIDTH", collapsedWidth);
            configuration.put("HEIGHT", collapsedHeight);
        }
        configuration.put("RADIAL_MENU", true);
        configuration.put("CENTER_DEAD_ZONE", centerDeadZone);
        configuration.put("HYSTERESIS_DEGREES", hysteresisDegrees);
        configuration.put("HOLD_SELECTED_INPUT", holdSelectedInput);
        configuration.put("SELECTION_HAPTICS", selectionHaptics);

        JSONArray itemArray = new JSONArray();
        for (RadialItem item : items) {
            JSONObject itemObject = new JSONObject();
            JSONArray bindingArray = new JSONArray();
            for (MappedInputButton.Binding binding : item.bindings) {
                bindingArray.put(binding.id);
            }
            itemObject.put("BINDINGS", bindingArray);
            itemArray.put(itemObject);
        }
        configuration.put("ITEMS", itemArray);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        centerDeadZone = (float) configuration.optDouble("CENTER_DEAD_ZONE", 0.30);
        centerDeadZone = Math.max(MIN_DEAD_ZONE_PERCENT / 100f,
                Math.min(MAX_DEAD_ZONE_PERCENT / 100f, centerDeadZone));
        hysteresisDegrees = (float) configuration.optDouble("HYSTERESIS_DEGREES", 8.0);
        hysteresisDegrees = Math.max(0f,
                Math.min(MAX_HYSTERESIS_DEGREES, hysteresisDegrees));
        holdSelectedInput = configuration.optBoolean("HOLD_SELECTED_INPUT", false);
        selectionHaptics = configuration.optBoolean("SELECTION_HAPTICS", true);

        JSONArray itemArray = configuration.optJSONArray("ITEMS");
        if (itemArray != null && itemArray.length() >= MIN_SECTORS) {
            List<RadialItem> loadedItems = new ArrayList<>();
            int itemCount = Math.min(itemArray.length(), MAX_SECTORS);
            for (int i = 0; i < itemCount; i++) {
                JSONArray bindingArray = itemArray.optJSONObject(i) != null ?
                        itemArray.optJSONObject(i).optJSONArray("BINDINGS") : null;
                List<MappedInputButton.Binding> loadedBindings = new ArrayList<>();
                if (bindingArray != null) {
                    for (int j = 0; j < bindingArray.length(); j++) {
                        MappedInputButton.Binding binding = MappedInputButton.findBindingOrNull(
                                bindingArray.optString(j));
                        if (binding != null) loadedBindings.add(binding);
                    }
                }
                if (!loadedBindings.isEmpty()) {
                    RadialItem item = new RadialItem(loadedBindings.get(0));
                    item.bindings.clear();
                    item.bindings.addAll(loadedBindings);
                    loadedItems.add(item);
                }
            }
            if (loadedItems.size() >= MIN_SECTORS) {
                items.clear();
                items.addAll(loadedItems);
            }
        }
    }

    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(SeekBar seekBar) { }
    }
}
