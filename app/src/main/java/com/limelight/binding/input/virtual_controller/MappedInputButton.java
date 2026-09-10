package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;

import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A touch button that can emit either a keyboard key or mouse button over GameStream.
 */
@SuppressLint("ViewConstructor")
public class MappedInputButton extends DigitalButton {
    private enum Kind { KEYBOARD, MOUSE }

    private static final class Binding {
        final String id;
        final String label;
        final Kind kind;
        final int code;

        Binding(String id, String label, Kind kind, int code) {
            this.id = id;
            this.label = label;
            this.kind = kind;
            this.code = code;
        }
    }

    private static final Binding[] BINDINGS = createBindings();

    private static Binding[] createBindings() {
        List<Binding> bindings = new ArrayList<>();
        bindings.add(new Binding("mouse_left", "LMB", Kind.MOUSE, EvdevListener.BUTTON_LEFT));
        bindings.add(new Binding("mouse_right", "RMB", Kind.MOUSE, EvdevListener.BUTTON_RIGHT));
        bindings.add(new Binding("mouse_middle", "MMB", Kind.MOUSE, EvdevListener.BUTTON_MIDDLE));
        bindings.add(new Binding("mouse_x1", "M4", Kind.MOUSE, EvdevListener.BUTTON_X1));
        bindings.add(new Binding("mouse_x2", "M5", Kind.MOUSE, EvdevListener.BUTTON_X2));

        bindings.add(new Binding("key_space", "SPACE", Kind.KEYBOARD, KeyEvent.KEYCODE_SPACE));
        bindings.add(new Binding("key_shift", "SHIFT", Kind.KEYBOARD, KeyEvent.KEYCODE_SHIFT_LEFT));
        bindings.add(new Binding("key_ctrl", "CTRL", Kind.KEYBOARD, KeyEvent.KEYCODE_CTRL_LEFT));
        bindings.add(new Binding("key_alt", "ALT", Kind.KEYBOARD, KeyEvent.KEYCODE_ALT_LEFT));
        bindings.add(new Binding("key_tab", "TAB", Kind.KEYBOARD, KeyEvent.KEYCODE_TAB));
        bindings.add(new Binding("key_escape", "ESC", Kind.KEYBOARD, KeyEvent.KEYCODE_ESCAPE));
        bindings.add(new Binding("key_enter", "ENTER", Kind.KEYBOARD, KeyEvent.KEYCODE_ENTER));
        bindings.add(new Binding("key_backspace", "BACK", Kind.KEYBOARD, KeyEvent.KEYCODE_DEL));

        for (int i = 0; i <= 9; i++) {
            bindings.add(new Binding("key_" + i, Integer.toString(i), Kind.KEYBOARD,
                    KeyEvent.KEYCODE_0 + i));
        }
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            bindings.add(new Binding("key_" + Character.toLowerCase(letter),
                    Character.toString(letter), Kind.KEYBOARD, KeyEvent.KEYCODE_A + i));
        }
        for (int i = 0; i < 12; i++) {
            bindings.add(new Binding("key_f" + (i + 1), "F" + (i + 1), Kind.KEYBOARD,
                    KeyEvent.KEYCODE_F1 + i));
        }

        bindings.add(new Binding("key_up", "UP", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_UP));
        bindings.add(new Binding("key_down", "DOWN", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_DOWN));
        bindings.add(new Binding("key_left", "LEFT", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_LEFT));
        bindings.add(new Binding("key_right", "RIGHT", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_RIGHT));
        return bindings.toArray(new Binding[0]);
    }

    private final EvdevListener inputSink;
    private Binding binding;
    private boolean inputDown;

    public MappedInputButton(VirtualController controller, int elementId, int layer,
                             String bindingId, Context context) {
        super(controller, elementId, layer, context);
        this.inputSink = controller.getInputSink();
        this.binding = findBinding(bindingId);
        setText(this.binding.label);

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

    private static Binding findBinding(String id) {
        for (Binding candidate : BINDINGS) {
            if (candidate.id.equals(id)) {
                return candidate;
            }
        }
        return BINDINGS[0];
    }

    private void setInputDown(boolean down) {
        if (inputDown == down || inputSink == null) {
            return;
        }

        inputDown = down;
        if (binding.kind == Kind.KEYBOARD) {
            inputSink.keyboardEvent(down, (short) binding.code);
        }
        else {
            inputSink.mouseButtonEvent(binding.code, down);
        }
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    @Override
    protected void showBindingDialog() {
        CharSequence[] labels = new CharSequence[BINDINGS.length];
        int selected = 0;
        for (int i = 0; i < BINDINGS.length; i++) {
            labels[i] = BINDINGS[i].label;
            if (BINDINGS[i].id.equals(binding.id)) {
                selected = i;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.osc_binding_title, binding.label))
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    releaseInput();
                    binding = BINDINGS[which];
                    setText(binding.label);
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void releaseInput() {
        setInputDown(false);
        setPressed(false);
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject configuration = super.getConfiguration();
        configuration.put("BINDING", binding.id);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        binding = findBinding(configuration.optString("BINDING", binding.id));
        setText(binding.label);
    }
}
