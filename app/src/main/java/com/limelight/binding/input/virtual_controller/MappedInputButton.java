package com.limelight.binding.input.virtual_controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.KeyEvent;

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

        bindings.add(new Binding("key_escape", "ESC", Kind.KEYBOARD, KeyEvent.KEYCODE_ESCAPE));
        bindings.add(new Binding("key_tab", "TAB", Kind.KEYBOARD, KeyEvent.KEYCODE_TAB));
        bindings.add(new Binding("key_caps_lock", "CAPS", Kind.KEYBOARD, KeyEvent.KEYCODE_CAPS_LOCK));
        bindings.add(new Binding("key_shift_left", "L SHIFT", Kind.KEYBOARD, KeyEvent.KEYCODE_SHIFT_LEFT));
        bindings.add(new Binding("key_shift_right", "R SHIFT", Kind.KEYBOARD, KeyEvent.KEYCODE_SHIFT_RIGHT));
        bindings.add(new Binding("key_ctrl_left", "L CTRL", Kind.KEYBOARD, KeyEvent.KEYCODE_CTRL_LEFT));
        bindings.add(new Binding("key_ctrl_right", "R CTRL", Kind.KEYBOARD, KeyEvent.KEYCODE_CTRL_RIGHT));
        bindings.add(new Binding("key_alt_left", "L ALT", Kind.KEYBOARD, KeyEvent.KEYCODE_ALT_LEFT));
        bindings.add(new Binding("key_alt_right", "R ALT", Kind.KEYBOARD, KeyEvent.KEYCODE_ALT_RIGHT));
        bindings.add(new Binding("key_meta_left", "L WIN", Kind.KEYBOARD, KeyEvent.KEYCODE_META_LEFT));
        bindings.add(new Binding("key_meta_right", "R WIN", Kind.KEYBOARD, KeyEvent.KEYCODE_META_RIGHT));
        bindings.add(new Binding("key_menu", "MENU", Kind.KEYBOARD, KeyEvent.KEYCODE_MENU));
        bindings.add(new Binding("key_space", "SPACE", Kind.KEYBOARD, KeyEvent.KEYCODE_SPACE));
        bindings.add(new Binding("key_enter", "ENTER", Kind.KEYBOARD, KeyEvent.KEYCODE_ENTER));
        bindings.add(new Binding("key_backspace", "BACK", Kind.KEYBOARD, KeyEvent.KEYCODE_DEL));
        bindings.add(new Binding("key_grave", "`", Kind.KEYBOARD, KeyEvent.KEYCODE_GRAVE));
        bindings.add(new Binding("key_minus", "-", Kind.KEYBOARD, KeyEvent.KEYCODE_MINUS));
        bindings.add(new Binding("key_equals", "=", Kind.KEYBOARD, KeyEvent.KEYCODE_EQUALS));
        bindings.add(new Binding("key_left_bracket", "[", Kind.KEYBOARD, KeyEvent.KEYCODE_LEFT_BRACKET));
        bindings.add(new Binding("key_right_bracket", "]", Kind.KEYBOARD, KeyEvent.KEYCODE_RIGHT_BRACKET));
        bindings.add(new Binding("key_backslash", "\\", Kind.KEYBOARD, KeyEvent.KEYCODE_BACKSLASH));
        bindings.add(new Binding("key_semicolon", ";", Kind.KEYBOARD, KeyEvent.KEYCODE_SEMICOLON));
        bindings.add(new Binding("key_apostrophe", "'", Kind.KEYBOARD, KeyEvent.KEYCODE_APOSTROPHE));
        bindings.add(new Binding("key_comma", ",", Kind.KEYBOARD, KeyEvent.KEYCODE_COMMA));
        bindings.add(new Binding("key_period", ".", Kind.KEYBOARD, KeyEvent.KEYCODE_PERIOD));
        bindings.add(new Binding("key_slash", "/", Kind.KEYBOARD, KeyEvent.KEYCODE_SLASH));

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

        bindings.add(new Binding("key_print_screen", "PRT SC", Kind.KEYBOARD, KeyEvent.KEYCODE_SYSRQ));
        bindings.add(new Binding("key_scroll_lock", "SCR LK", Kind.KEYBOARD, KeyEvent.KEYCODE_SCROLL_LOCK));
        bindings.add(new Binding("key_pause", "PAUSE", Kind.KEYBOARD, KeyEvent.KEYCODE_BREAK));
        bindings.add(new Binding("key_insert", "INS", Kind.KEYBOARD, KeyEvent.KEYCODE_INSERT));
        bindings.add(new Binding("key_home", "HOME", Kind.KEYBOARD, KeyEvent.KEYCODE_MOVE_HOME));
        bindings.add(new Binding("key_page_up", "PG UP", Kind.KEYBOARD, KeyEvent.KEYCODE_PAGE_UP));
        bindings.add(new Binding("key_delete", "DEL", Kind.KEYBOARD, KeyEvent.KEYCODE_FORWARD_DEL));
        bindings.add(new Binding("key_end", "END", Kind.KEYBOARD, KeyEvent.KEYCODE_MOVE_END));
        bindings.add(new Binding("key_page_down", "PG DN", Kind.KEYBOARD, KeyEvent.KEYCODE_PAGE_DOWN));
        bindings.add(new Binding("key_up", "UP", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_UP));
        bindings.add(new Binding("key_down", "DOWN", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_DOWN));
        bindings.add(new Binding("key_left", "LEFT", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_LEFT));
        bindings.add(new Binding("key_right", "RIGHT", Kind.KEYBOARD, KeyEvent.KEYCODE_DPAD_RIGHT));

        bindings.add(new Binding("key_num_lock", "NUM", Kind.KEYBOARD, KeyEvent.KEYCODE_NUM_LOCK));
        for (int i = 0; i <= 9; i++) {
            bindings.add(new Binding("key_numpad_" + i, "NUM " + i, Kind.KEYBOARD,
                    KeyEvent.KEYCODE_NUMPAD_0 + i));
        }
        bindings.add(new Binding("key_numpad_divide", "NUM /", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_DIVIDE));
        bindings.add(new Binding("key_numpad_multiply", "NUM *", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_MULTIPLY));
        bindings.add(new Binding("key_numpad_subtract", "NUM -", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_SUBTRACT));
        bindings.add(new Binding("key_numpad_add", "NUM +", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_ADD));
        bindings.add(new Binding("key_numpad_enter", "NUM ENTER", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_ENTER));
        bindings.add(new Binding("key_numpad_dot", "NUM .", Kind.KEYBOARD, KeyEvent.KEYCODE_NUMPAD_DOT));
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
        // Migrate profiles written before left/right modifier keys were distinguished.
        if ("key_shift".equals(id)) id = "key_shift_left";
        else if ("key_ctrl".equals(id)) id = "key_ctrl_left";
        else if ("key_alt".equals(id)) id = "key_alt_left";

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
                binding.code == KeyEvent.KEYCODE_CTRL_RIGHT ||
                binding.code == KeyEvent.KEYCODE_SHIFT_LEFT ||
                binding.code == KeyEvent.KEYCODE_SHIFT_RIGHT ||
                binding.code == KeyEvent.KEYCODE_ALT_LEFT ||
                binding.code == KeyEvent.KEYCODE_ALT_RIGHT ||
                binding.code == KeyEvent.KEYCODE_META_LEFT ||
                binding.code == KeyEvent.KEYCODE_META_RIGHT;
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
        BindingEditorDialog.show(getContext(),
                getContext().getString(R.string.osc_binding_title, getBindingSummary()),
                BINDINGS, bindings, true, false,
                selectedBindings -> {
                    releaseInput();
                    bindings.clear();
                    bindings.addAll(selectedBindings);
                    updateLabel();
                    VirtualControllerConfigurationLoader.saveProfile(virtualController, getContext());
                }, () -> virtualController.removeMappedButton(MappedInputButton.this));
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
