/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualController {
    public static class ControllerInputContext {
        public short inputMap = 0x0000;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;
    }

    public enum ControllerMode {
        Active,
        MoveButtons,
        ResizeButtons,
        BindButtons
    }

    private static final boolean _PRINT_DEBUG_INFORMATION = false;

    private final ControllerHandler controllerHandler;
    private final EvdevListener inputSink;
    private final Context context;
    private final Handler handler;
    private final String profilePreferenceName;
    private final String inputMode;

    private final Runnable delayedRetransmitRunnable = new Runnable() {
        @Override
        public void run() {
            sendControllerInputContextInternal();
        }
    };

    private FrameLayout frame_layout = null;

    ControllerMode currentMode = ControllerMode.Active;
    ControllerInputContext inputContext = new ControllerInputContext();

    private Button buttonConfigure = null;
    private Button buttonAdd = null;

    private List<VirtualControllerElement> elements = new ArrayList<>();
    private final Map<Integer, Integer> mappedKeyboardRefCounts = new HashMap<>();
    private final Map<Integer, Integer> mappedMouseRefCounts = new HashMap<>();
    private int opacity = 100;

    public VirtualController(final ControllerHandler controllerHandler, final EvdevListener inputSink,
                             FrameLayout layout, final Context context, String profileId,
                             String inputMode) {
        this.controllerHandler = controllerHandler;
        this.inputSink = inputSink;
        this.frame_layout = layout;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.inputMode = inputMode;
        String stableProfileId = profileId == null ? "desktop" : Integer.toHexString(profileId.hashCode());
        this.profilePreferenceName = VirtualControllerConfigurationLoader.OSC_PREFERENCE +
                "_v2_" + stableProfileId + "_" + inputMode;

        buttonConfigure = new Button(context);
        buttonConfigure.setAlpha(0.25f);
        buttonConfigure.setFocusable(false);
        buttonConfigure.setBackgroundResource(R.drawable.ic_settings);
        buttonConfigure.setContentDescription(context.getString(R.string.osc_configure_controls));
        buttonConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;

                if (currentMode == ControllerMode.Active){
                    currentMode = ControllerMode.MoveButtons;
                    message = "Entering configuration mode (Move buttons)";
                } else if (currentMode == ControllerMode.MoveButtons) {
                    currentMode = ControllerMode.ResizeButtons;
                    message = context.getString(R.string.osc_mode_resize);
                } else if (currentMode == ControllerMode.ResizeButtons) {
                    currentMode = ControllerMode.BindButtons;
                    message = context.getString(R.string.osc_mode_bind);
                } else {
                    currentMode = ControllerMode.Active;
                    VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
                    message = context.getString(R.string.osc_mode_active);
                }

                if (currentMode == ControllerMode.MoveButtons) {
                    message = context.getString(R.string.osc_mode_move);
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                buttonConfigure.invalidate();
                updateAddButtonVisibility();

                for (VirtualControllerElement element : elements) {
                    element.invalidate();
                }
            }
        });

        buttonAdd = new Button(context);
        buttonAdd.setAlpha(0.55f);
        buttonAdd.setFocusable(false);
        buttonAdd.setText("+");
        buttonAdd.setContentDescription(context.getString(R.string.osc_add_button));
        buttonAdd.setVisibility(View.GONE);
        buttonAdd.setOnClickListener(v -> addMappedButton());

    }

    Handler getHandler() {
        return handler;
    }

    EvdevListener getInputSink() {
        return inputSink;
    }

    void mappedKeyboardEvent(int keyCode, boolean down) {
        updateMappedInputRefCount(mappedKeyboardRefCounts, keyCode, down, true);
    }

    void mappedMouseButtonEvent(int buttonCode, boolean down) {
        updateMappedInputRefCount(mappedMouseRefCounts, buttonCode, down, false);
    }

    private void updateMappedInputRefCount(Map<Integer, Integer> refCounts, int code,
                                           boolean down, boolean keyboard) {
        if (inputSink == null) {
            return;
        }

        int oldCount = refCounts.containsKey(code) ? refCounts.get(code) : 0;
        if (down) {
            refCounts.put(code, oldCount + 1);
            if (oldCount == 0) {
                if (keyboard) {
                    inputSink.keyboardEvent(true, (short) code);
                }
                else {
                    inputSink.mouseButtonEvent(code, true);
                }
            }
        }
        else if (oldCount > 0) {
            if (oldCount == 1) {
                refCounts.remove(code);
                if (keyboard) {
                    inputSink.keyboardEvent(false, (short) code);
                }
                else {
                    inputSink.mouseButtonEvent(code, false);
                }
            }
            else {
                refCounts.put(code, oldCount - 1);
            }
        }
    }

    String getProfilePreferenceName() {
        return profilePreferenceName;
    }

    boolean isKeyboardMouseMode() {
        return PreferenceConfiguration.ONSCREEN_INPUT_MODE_KEYBOARD_MOUSE.equals(inputMode);
    }

    public void hide() {
        for (VirtualControllerElement element : elements) {
            element.releaseInput();
            element.setVisibility(View.INVISIBLE);
        }

        buttonConfigure.setVisibility(View.INVISIBLE);
        buttonAdd.setVisibility(View.INVISIBLE);
    }

    public void show() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.VISIBLE);
        }

        buttonConfigure.setVisibility(View.VISIBLE);
        updateAddButtonVisibility();
    }

    public void removeElements() {
        for (VirtualControllerElement element : elements) {
            element.releaseInput();
            frame_layout.removeView(element);
        }
        elements.clear();

        frame_layout.removeView(buttonConfigure);
        frame_layout.removeView(buttonAdd);
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
        for (VirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }


    public void addElement(VirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);

        frame_layout.addView(element, layoutParams);
        element.setOpacity(opacity);
    }

    public List<VirtualControllerElement> getElements() {
        return elements;
    }

    void removeMappedButtons() {
        List<VirtualControllerElement> mappedButtons = new ArrayList<>();
        for (VirtualControllerElement element : elements) {
            if (element instanceof MappedInputButton) {
                mappedButtons.add(element);
            }
        }

        for (VirtualControllerElement element : mappedButtons) {
            element.releaseInput();
            frame_layout.removeView(element);
            elements.remove(element);
        }
    }

    void removeMappedButton(MappedInputButton button) {
        if (!elements.remove(button)) {
            return;
        }

        button.releaseInput();
        frame_layout.removeView(button);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_button_deleted, Toast.LENGTH_SHORT).show();
    }

    private void addMappedButton() {
        if (!isKeyboardMouseMode()) {
            return;
        }

        int elementId = VirtualControllerElement.EID_MAPPED_CUSTOM_START;
        int mappedButtonCount = 0;
        for (VirtualControllerElement element : elements) {
            if (element instanceof MappedInputButton) {
                mappedButtonCount++;
                elementId = Math.max(elementId, element.elementId + 1);
            }
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int buttonSize = (int) (screen.heightPixels * 0.13f);
        int cascade = (mappedButtonCount % 5) * (buttonSize / 5);
        int x = Math.max(0, (screen.widthPixels - buttonSize) / 2 + cascade);
        int y = Math.max(0, (screen.heightPixels - buttonSize) / 2 + cascade);

        MappedInputButton button = new MappedInputButton(this, elementId, 10,
                "key_space", context);
        addElement(button, x, y, buttonSize, buttonSize);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        button.showBindingDialog();
    }

    private void updateAddButtonVisibility() {
        buttonAdd.setVisibility(isKeyboardMouseMode() &&
                currentMode == ControllerMode.BindButtons ? View.VISIBLE : View.GONE);
    }

    private static final void _DBG(String text) {
        if (_PRINT_DEBUG_INFORMATION) {
            LimeLog.info("VirtualController: " + text);
        }
    }

    public void refreshLayout() {
        removeElements();

        DisplayMetrics screen = context.getResources().getDisplayMetrics();

        int buttonSize = (int)(screen.heightPixels*0.06f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        params.leftMargin = 15;
        params.topMargin = 15;
        frame_layout.addView(buttonConfigure, params);

        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        addParams.leftMargin = 15 + buttonSize + 8;
        addParams.topMargin = 15;
        frame_layout.addView(buttonAdd, addParams);
        updateAddButtonVisibility();

        // Start with the default layout
        VirtualControllerConfigurationLoader.createDefaultLayout(this, context);

        // Apply user preferences onto the default layout
        VirtualControllerConfigurationLoader.loadFromPreferences(this, context);
    }

    public ControllerMode getControllerMode() {
        return currentMode;
    }

    public ControllerInputContext getControllerInputContext() {
        return inputContext;
    }

    private void sendControllerInputContextInternal() {
        _DBG("INPUT_MAP + " + inputContext.inputMap);
        _DBG("LEFT_TRIGGER " + inputContext.leftTrigger);
        _DBG("RIGHT_TRIGGER " + inputContext.rightTrigger);
        _DBG("LEFT STICK X: " + inputContext.leftStickX + " Y: " + inputContext.leftStickY);
        _DBG("RIGHT STICK X: " + inputContext.rightStickX + " Y: " + inputContext.rightStickY);

        if (controllerHandler != null) {
            controllerHandler.reportOscState(
                    inputContext.inputMap,
                    inputContext.leftStickX,
                    inputContext.leftStickY,
                    inputContext.rightStickX,
                    inputContext.rightStickY,
                    inputContext.leftTrigger,
                    inputContext.rightTrigger
            );
        }
    }

    void sendControllerInputContext() {
        // Cancel retransmissions of prior gamepad inputs
        handler.removeCallbacks(delayedRetransmitRunnable);

        sendControllerInputContextInternal();

        // HACK: GFE sometimes discards gamepad packets when they are received
        // very shortly after another. This can be critical if an axis zeroing packet
        // is lost and causes an analog stick to get stuck. To avoid this, we retransmit
        // the gamepad state a few times unless another input event happens before then.
        handler.postDelayed(delayedRetransmitRunnable, 25);
        handler.postDelayed(delayedRetransmitRunnable, 50);
        handler.postDelayed(delayedRetransmitRunnable, 75);
    }
}
