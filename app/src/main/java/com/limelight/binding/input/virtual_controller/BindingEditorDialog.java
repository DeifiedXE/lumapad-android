package com.limelight.binding.input.virtual_controller;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared full-screen keyboard and legacy list editors for virtual-control bindings. */
final class BindingEditorDialog {
    interface SelectionListener {
        void onSelectionSaved(List<MappedInputButton.Binding> bindings);
    }

    private static final int KEY_COLOR = Color.rgb(45, 48, 52);
    private static final int KEY_SELECTED_COLOR = Color.rgb(24, 112, 191);

    private static final class KeySpec {
        final String id;
        final String text;
        final float width;

        KeySpec(String id, String text, float width) {
            this.id = id;
            this.text = text;
            this.width = width;
        }
    }

    private BindingEditorDialog() {
    }

    static boolean usesFullKeyboard(Context context) {
        return PreferenceConfiguration.ONSCREEN_BINDING_EDITOR_KEYBOARD.equals(
                PreferenceConfiguration.readPreferences(context).onscreenBindingEditor);
    }

    static void show(Context context, String title, MappedInputButton.Binding[] choices,
                     List<MappedInputButton.Binding> currentBindings, boolean requireBinding,
                     boolean singleSelection, SelectionListener listener, Runnable deleteAction) {
        if (usesFullKeyboard(context)) {
            showFullKeyboard(context, title, choices, currentBindings, requireBinding,
                    singleSelection, listener, deleteAction);
        }
        else {
            showList(context, title, choices, currentBindings, requireBinding,
                    singleSelection, listener, deleteAction);
        }
    }

    private static Set<String> getSelectedIds(List<MappedInputButton.Binding> currentBindings) {
        Set<String> selectedIds = new LinkedHashSet<>();
        for (MappedInputButton.Binding binding : currentBindings) {
            selectedIds.add(binding.id);
        }
        return selectedIds;
    }

    private static List<MappedInputButton.Binding> getSelectedBindings(
            MappedInputButton.Binding[] choices, Set<String> selectedIds) {
        List<MappedInputButton.Binding> selectedBindings = new ArrayList<>();
        for (MappedInputButton.Binding binding : choices) {
            if (selectedIds.contains(binding.id)) {
                selectedBindings.add(binding);
            }
        }
        return selectedBindings;
    }

    private static String getSummary(Context context, MappedInputButton.Binding[] choices,
                                     Set<String> selectedIds) {
        StringBuilder summary = new StringBuilder();
        for (MappedInputButton.Binding binding : choices) {
            if (selectedIds.contains(binding.id)) {
                if (summary.length() != 0) {
                    summary.append(" + ");
                }
                summary.append(binding.label);
            }
        }
        return summary.length() == 0 ? context.getString(R.string.osc_binding_none) :
                summary.toString();
    }

    private static void showList(Context context, String title,
                                 MappedInputButton.Binding[] choices,
                                 List<MappedInputButton.Binding> currentBindings,
                                 boolean requireBinding, boolean singleSelection,
                                 SelectionListener listener, Runnable deleteAction) {
        CharSequence[] labels = new CharSequence[choices.length];
        boolean[] selected = new boolean[choices.length];
        Set<String> selectedIds = getSelectedIds(currentBindings);
        for (int i = 0; i < choices.length; i++) {
            labels[i] = choices[i].label;
            selected[i] = selectedIds.contains(choices[i].id);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMultiChoiceItems(labels, selected, (chooser, which, isChecked) -> {
                    if (singleSelection && isChecked) {
                        for (int i = 0; i < selected.length; i++) {
                            if (i != which && selected[i]) {
                                selected[i] = false;
                                ((AlertDialog) chooser).getListView().setItemChecked(i, false);
                            }
                        }
                    }
                    selected[which] = isChecked;
                })
                .setPositiveButton(R.string.osc_binding_save, null)
                .setNegativeButton(android.R.string.cancel, null);
        if (deleteAction != null) {
            builder.setNeutralButton(R.string.osc_delete_button, (chooser, which) ->
                    deleteAction.run());
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    Set<String> pendingIds = new LinkedHashSet<>();
                    for (int i = 0; i < choices.length; i++) {
                        if (selected[i]) pendingIds.add(choices[i].id);
                    }
                    if (requireBinding && pendingIds.isEmpty()) {
                        Toast.makeText(context, R.string.osc_binding_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listener.onSelectionSaved(getSelectedBindings(choices, pendingIds));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private static void showFullKeyboard(Context context, String title,
                                         MappedInputButton.Binding[] choices,
                                         List<MappedInputButton.Binding> currentBindings,
                                         boolean requireBinding, boolean singleSelection,
                                         SelectionListener listener, Runnable deleteAction) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        Set<String> selectedIds = getSelectedIds(currentBindings);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 6), dp(context, 4), dp(context, 6), dp(context, 4));
        root.setBackgroundColor(Color.rgb(17, 18, 20));

        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setSingleLine(true);
        heading.addView(titleView);

        TextView summaryView = new TextView(context);
        summaryView.setTextColor(Color.rgb(121, 190, 255));
        summaryView.setTextSize(12);
        summaryView.setSingleLine(true);
        heading.addView(summaryView);

        TextView hintView = new TextView(context);
        hintView.setText(R.string.osc_binding_keyboard_hint);
        hintView.setTextColor(Color.LTGRAY);
        hintView.setTextSize(10);
        hintView.setSingleLine(true);
        heading.addView(hintView);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button cancelButton = makeActionButton(context, context.getString(android.R.string.cancel));
        toolbar.addView(cancelButton);
        Button deleteButton = null;
        if (deleteAction != null) {
            deleteButton = makeActionButton(context, context.getString(R.string.osc_delete_button));
            toolbar.addView(deleteButton);
        }
        Button saveButton = makeActionButton(context, context.getString(R.string.osc_binding_save));
        toolbar.addView(saveButton);
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout keyboard = new LinearLayout(context);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        root.addView(keyboard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f));

        List<Button> keyButtons = new ArrayList<>();
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_escape", "Esc", 1.4f), k("key_f1", "F1"), k("key_f2", "F2"),
                k("key_f3", "F3"), k("key_f4", "F4"), k("key_f5", "F5"),
                k("key_f6", "F6"), k("key_f7", "F7"), k("key_f8", "F8"),
                k("key_f9", "F9"), k("key_f10", "F10"), k("key_f11", "F11"),
                k("key_f12", "F12"), k("key_print_screen", "PrtSc", 1.35f),
                k("key_scroll_lock", "ScrLk", 1.35f), k("key_pause", "Pause", 1.35f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_grave", "`"), k("key_1", "1"), k("key_2", "2"),
                k("key_3", "3"), k("key_4", "4"), k("key_5", "5"),
                k("key_6", "6"), k("key_7", "7"), k("key_8", "8"),
                k("key_9", "9"), k("key_0", "0"), k("key_minus", "-"),
                k("key_equals", "="), k("key_backspace", "Backspace", 2.2f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_tab", "Tab", 1.55f), k("key_q", "Q"), k("key_w", "W"),
                k("key_e", "E"), k("key_r", "R"), k("key_t", "T"),
                k("key_y", "Y"), k("key_u", "U"), k("key_i", "I"),
                k("key_o", "O"), k("key_p", "P"), k("key_left_bracket", "["),
                k("key_right_bracket", "]"), k("key_backslash", "\\", 1.65f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_caps_lock", "Caps", 1.85f), k("key_a", "A"), k("key_s", "S"),
                k("key_d", "D"), k("key_f", "F"), k("key_g", "G"),
                k("key_h", "H"), k("key_j", "J"), k("key_k", "K"),
                k("key_l", "L"), k("key_semicolon", ";"), k("key_apostrophe", "'"),
                k("key_enter", "Enter", 2.2f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_shift_left", "L Shift", 2.3f), k("key_z", "Z"), k("key_x", "X"),
                k("key_c", "C"), k("key_v", "V"), k("key_b", "B"),
                k("key_n", "N"), k("key_m", "M"), k("key_comma", ","),
                k("key_period", "."), k("key_slash", "/"),
                k("key_shift_right", "R Shift", 2.65f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_ctrl_left", "L Ctrl", 1.35f), k("key_meta_left", "L Win", 1.35f),
                k("key_alt_left", "L Alt", 1.35f), k("key_space", "Space", 5.4f),
                k("key_alt_right", "R Alt", 1.35f), k("key_meta_right", "R Win", 1.35f),
                k("key_menu", "Menu", 1.25f), k("key_ctrl_right", "R Ctrl", 1.35f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_insert", "Ins"), k("key_home", "Home"), k("key_page_up", "PgUp"),
                k("key_delete", "Del"), k("key_end", "End"), k("key_page_down", "PgDn"),
                k("key_left", "←"), k("key_up", "↑"), k("key_down", "↓"),
                k("key_right", "→"), k("mouse_left", "Mouse L", 1.45f),
                k("mouse_right", "Mouse R", 1.45f), k("mouse_middle", "Mouse M", 1.45f),
                k("mouse_x1", "Mouse 4", 1.45f), k("mouse_x2", "Mouse 5", 1.45f));
        addRow(context, keyboard, choices, selectedIds, summaryView, singleSelection, keyButtons,
                k("key_num_lock", "Num"), k("key_numpad_divide", "Num /"),
                k("key_numpad_multiply", "Num *"), k("key_numpad_subtract", "Num -"),
                k("key_numpad_7", "Num 7"), k("key_numpad_8", "Num 8"),
                k("key_numpad_9", "Num 9"), k("key_numpad_add", "Num +"),
                k("key_numpad_4", "Num 4"), k("key_numpad_5", "Num 5"),
                k("key_numpad_6", "Num 6"), k("key_numpad_1", "Num 1"),
                k("key_numpad_2", "Num 2"), k("key_numpad_3", "Num 3"),
                k("key_numpad_0", "Num 0"), k("key_numpad_dot", "Num ."),
                k("key_numpad_enter", "Num Enter", 1.45f));

        updateSummary(context, summaryView, choices, selectedIds);
        cancelButton.setOnClickListener(view -> dialog.dismiss());
        if (deleteButton != null) {
            deleteButton.setOnClickListener(view -> {
                deleteAction.run();
                dialog.dismiss();
            });
        }
        saveButton.setOnClickListener(view -> {
            if (requireBinding && selectedIds.isEmpty()) {
                Toast.makeText(context, R.string.osc_binding_required, Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onSelectionSaved(getSelectedBindings(choices, selectedIds));
            dialog.dismiss();
        });

        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private static Button makeActionButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        return button;
    }

    private static KeySpec k(String id, String text) {
        return new KeySpec(id, text, 1f);
    }

    private static KeySpec k(String id, String text, float width) {
        return new KeySpec(id, text, width);
    }

    private static void addRow(Context context, LinearLayout keyboard,
                               MappedInputButton.Binding[] choices, Set<String> selectedIds,
                               TextView summaryView, boolean singleSelection,
                               List<Button> allButtons, KeySpec... specs) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        keyboard.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f));

        for (KeySpec spec : specs) {
            if (!containsBinding(choices, spec.id)) {
                continue;
            }
            Button button = new Button(context);
            button.setTag(spec.id);
            button.setText(spec.text);
            button.setTextColor(Color.WHITE);
            button.setTextSize(9);
            button.setAllCaps(false);
            button.setGravity(Gravity.CENTER);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setPadding(0, 0, 0, 0);
            button.setStateListAnimator(null);
            updateKeyStyle(button, selectedIds.contains(spec.id));
            allButtons.add(button);
            button.setOnClickListener(view -> {
                String id = (String) view.getTag();
                if (selectedIds.contains(id)) {
                    selectedIds.remove(id);
                }
                else {
                    if (singleSelection) {
                        selectedIds.clear();
                        for (Button keyButton : allButtons) {
                            updateKeyStyle(keyButton, false);
                        }
                    }
                    selectedIds.add(id);
                }
                updateKeyStyle(button, selectedIds.contains(id));
                updateSummary(context, summaryView, choices, selectedIds);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, spec.width);
            int margin = dp(context, 1);
            params.setMargins(margin, margin, margin, margin);
            row.addView(button, params);
        }
    }

    private static boolean containsBinding(MappedInputButton.Binding[] choices, String id) {
        for (MappedInputButton.Binding binding : choices) {
            if (binding.id.equals(id)) return true;
        }
        return false;
    }

    private static void updateKeyStyle(Button button, boolean selected) {
        button.setBackgroundTintList(ColorStateList.valueOf(
                selected ? KEY_SELECTED_COLOR : KEY_COLOR));
    }

    private static void updateSummary(Context context, TextView summaryView,
                                      MappedInputButton.Binding[] choices,
                                      Set<String> selectedIds) {
        summaryView.setText(context.getString(R.string.osc_binding_selected,
                getSummary(context, choices, selectedIds)));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
