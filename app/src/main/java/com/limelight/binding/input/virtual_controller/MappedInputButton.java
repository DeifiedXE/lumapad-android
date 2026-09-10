package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevListener;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A touch button that can emit either a keyboard key or mouse button over GameStream.
 */
@SuppressLint("ViewConstructor")
public class MappedInputButton extends DigitalButton {
    enum Kind { KEYBOARD, MOUSE }

    static final class Binding {
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

    private final List<Binding> bindings = new ArrayList<>();
    private boolean inputDown;

    public MappedInputButton(VirtualController controller, int elementId, int layer,
                             String bindingId, Context context) {
        super(controller, elementId, layer, context);
        this.bindings.add(findBinding(bindingId));
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

    private static Binding findBinding(String id) {
        Binding binding = findBindingOrNull(id);
        return binding != null ? binding : BINDINGS[0];
    }

    static Binding findBindingOrNull(String id) {
        for (Binding candidate : BINDINGS) {
            if (candidate.id.equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    static Binding[] getKeyboardBindings() {
        List<Binding> keyboardBindings = new ArrayList<>();
        for (Binding binding : BINDINGS) {
            if (binding.kind == Kind.KEYBOARD) {
                keyboardBindings.add(binding);
            }
        }
        return keyboardBindings.toArray(new Binding[0]);
    }

    static Binding[] getBindings() {
        return BINDINGS.clone();
    }

    private static boolean isModifier(Binding binding) {
        if (binding.kind != Kind.KEYBOARD) {
            return false;
        }

        return binding.code == KeyEvent.KEYCODE_CTRL_LEFT ||
                binding.code == KeyEvent.KEYCODE_SHIFT_LEFT ||
                binding.code == KeyEvent.KEYCODE_ALT_LEFT ||
                binding.code == KeyEvent.KEYCODE_META_LEFT;
    }

    private static void emitBinding(VirtualController controller, Binding binding, boolean down) {
        if (binding.kind == Kind.KEYBOARD) {
            controller.mappedKeyboardEvent(binding.code, down);
        }
        else {
            controller.mappedMouseButtonEvent(binding.code, down);
        }
    }

    static void emitBindings(VirtualController controller, List<Binding> bindings, boolean down) {
        if (down) {
            // Press modifiers first so combinations such as Ctrl+Shift+Q are recognized.
            for (Binding binding : bindings) {
                if (isModifier(binding)) {
                    emitBinding(controller, binding, true);
                }
            }
            for (Binding binding : bindings) {
                if (binding.kind == Kind.KEYBOARD && !isModifier(binding)) {
                    emitBinding(controller, binding, true);
                }
            }
            for (Binding binding : bindings) {
                if (binding.kind == Kind.MOUSE) {
                    emitBinding(controller, binding, true);
                }
            }
        }
        else {
            // Release in the opposite order, leaving modifiers held until the end.
            for (int i = bindings.size() - 1; i >= 0; i--) {
                Binding binding = bindings.get(i);
                if (binding.kind == Kind.MOUSE) {
                    emitBinding(controller, binding, false);
                }
            }
            for (int i = bindings.size() - 1; i >= 0; i--) {
                Binding binding = bindings.get(i);
                if (binding.kind == Kind.KEYBOARD && !isModifier(binding)) {
                    emitBinding(controller, binding, false);
                }
            }
            for (int i = bindings.size() - 1; i >= 0; i--) {
                Binding binding = bindings.get(i);
                if (isModifier(binding)) {
                    emitBinding(controller, binding, false);
                }
            }
        }
    }

    private String getBindingSummary() {
        StringBuilder summary = new StringBuilder();
        for (Binding binding : bindings) {
            if (summary.length() != 0) {
                summary.append('+');
            }
            summary.append(binding.label);
        }
        return summary.toString();
    }

    private void updateLabel() {
        setText(getBindingSummary());
    }

    private void setInputDown(boolean down) {
        if (inputDown == down || virtualController.getInputSink() == null) {
            return;
        }

        inputDown = down;
        emitBindings(virtualController, bindings, down);
    }

    @Override
    protected boolean isBindingConfigurable() {
        return true;
    }

    @Override
    protected void showBindingDialog() {
        CharSequence[] labels = new CharSequence[BINDINGS.length];
        boolean[] selected = new boolean[BINDINGS.length];
        for (int i = 0; i < BINDINGS.length; i++) {
            labels[i] = BINDINGS[i].label;
            for (Binding activeBinding : bindings) {
                if (BINDINGS[i].id.equals(activeBinding.id)) {
                    selected[i] = true;
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.osc_binding_title, getBindingSummary()))
                .setMultiChoiceItems(labels, selected, (chooser, which, isChecked) ->
                        selected[which] = isChecked)
                .setPositiveButton(R.string.osc_binding_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.osc_delete_button, (chooser, which) ->
                        virtualController.removeMappedButton(MappedInputButton.this))
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    List<Binding> selectedBindings = new ArrayList<>();
                    for (int i = 0; i < BINDINGS.length; i++) {
                        if (selected[i]) {
                            selectedBindings.add(BINDINGS[i]);
                        }
                    }

                    if (selectedBindings.isEmpty()) {
                        Toast.makeText(getContext(), R.string.osc_binding_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    releaseInput();
                    bindings.clear();
                    bindings.addAll(selectedBindings);
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
        JSONArray bindingIds = new JSONArray();
        for (Binding binding : bindings) {
            bindingIds.put(binding.id);
        }
        configuration.put("BINDINGS", bindingIds);
        // Retain the first binding for compatibility with LumaPad v0.1 profiles.
        configuration.put("BINDING", bindings.get(0).id);
        return configuration;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        List<Binding> loadedBindings = new ArrayList<>();
        JSONArray bindingIds = configuration.optJSONArray("BINDINGS");
        if (bindingIds != null) {
            for (int i = 0; i < bindingIds.length(); i++) {
                Binding loadedBinding = findBindingOrNull(bindingIds.optString(i));
                if (loadedBinding != null) {
                    loadedBindings.add(loadedBinding);
                }
            }
        }

        if (loadedBindings.isEmpty()) {
            loadedBindings.add(findBinding(configuration.optString(
                    "BINDING", bindings.get(0).id)));
        }

        releaseInput();
        bindings.clear();
        bindings.addAll(loadedBindings);
        updateLabel();
    }
}
