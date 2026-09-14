/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private final ControlProfileManager profileManager;
    private String profilePreferenceName;
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
    private Button buttonProfile = null;

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
        this.profileManager = new ControlProfileManager(context, inputMode, profileId);
        this.profilePreferenceName = profileManager.getCurrentPreferenceName();

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
        buttonAdd.setContentDescription(context.getString(R.string.osc_add_control));
        buttonAdd.setVisibility(View.GONE);
        buttonAdd.setOnClickListener(v -> showAddControlDialog());

        buttonProfile = new Button(context);
        buttonProfile.setAlpha(0.55f);
        buttonProfile.setFocusable(false);
        buttonProfile.setAllCaps(false);
        buttonProfile.setTextSize(10);
        updateProfileButtonLabel();
        buttonProfile.setOnClickListener(v -> showProfileDialog());

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

    private void updateProfileButtonLabel() {
        String name = profileManager.getCurrentProfile().name;
        String shortName = name.length() <= 8 ? name : name.substring(0, 7) + "…";
        buttonProfile.setText(shortName);
        buttonProfile.setContentDescription(context.getString(
                R.string.osc_profile_button_description, name));
    }

    private void showProfileDialog() {
        List<ControlProfileManager.Profile> profiles = profileManager.getProfiles();
        CharSequence[] choices = new CharSequence[profiles.size() + 3];
        for (int i = 0; i < profiles.size(); i++) {
            ControlProfileManager.Profile profile = profiles.get(i);
            choices[i] = (profileManager.isCurrent(profile.id) ? "✓ " : "") + profile.name;
        }
        choices[profiles.size()] = context.getString(R.string.osc_profile_new);
        choices[profiles.size() + 1] = context.getString(R.string.osc_profile_rename);
        choices[profiles.size() + 2] = context.getString(R.string.osc_profile_delete);

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.osc_profile_title,
                        profileManager.getCurrentProfile().name))
                .setItems(choices, (dialog, which) -> {
                    if (which < profiles.size()) {
                        switchProfile(profiles.get(which));
                    }
                    else if (which == profiles.size()) {
                        showCreateProfileDialog();
                    }
                    else if (which == profiles.size() + 1) {
                        showRenameProfileDialog();
                    }
                    else {
                        showDeleteProfileDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void switchProfile(ControlProfileManager.Profile profile) {
        if (profileManager.isCurrent(profile.id)) return;
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        profileManager.setCurrent(profile.id);
        profilePreferenceName = profileManager.getCurrentPreferenceName();
        currentMode = ControllerMode.Active;
        refreshLayout();
        Toast.makeText(context, context.getString(R.string.osc_profile_switched, profile.name),
                Toast.LENGTH_SHORT).show();
    }

    private interface ProfileNameListener {
        void onNameAccepted(String name);
    }

    private void showProfileNameDialog(int titleResource, String initialName,
                                       String exceptProfileId, ProfileNameListener listener) {
        EditText nameInput = new EditText(context);
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameInput.setText(initialName);
        nameInput.setSelectAllOnFocus(true);
        int padding = Math.round(20 * context.getResources().getDisplayMetrics().density);
        FrameLayout inputContainer = new FrameLayout(context);
        inputContainer.setPadding(padding, 0, padding, 0);
        inputContainer.addView(nameInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(titleResource)
                .setView(inputContainer)
                .setPositiveButton(R.string.osc_save, null)
                .setNegativeButton(android.R.string.cancel, null);
        if (titleResource == R.string.osc_profile_new_title) {
            builder.setMessage(R.string.osc_profile_new_summary);
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(context, R.string.osc_profile_name_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (profileManager.nameExists(name, exceptProfileId)) {
                        Toast.makeText(context, R.string.osc_profile_name_duplicate,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listener.onNameAccepted(name);
                    dialog.dismiss();
                }));
        dialog.show();
        nameInput.requestFocus();
    }

    private void showCreateProfileDialog() {
        showProfileNameDialog(R.string.osc_profile_new_title, "", null, name -> {
            VirtualControllerConfigurationLoader.saveProfile(this, context);
            profileManager.createProfile(name, profilePreferenceName);
            profilePreferenceName = profileManager.getCurrentPreferenceName();
            currentMode = ControllerMode.Active;
            refreshLayout();
            Toast.makeText(context, context.getString(R.string.osc_profile_created, name),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showRenameProfileDialog() {
        ControlProfileManager.Profile current = profileManager.getCurrentProfile();
        showProfileNameDialog(R.string.osc_profile_rename_title, current.name, current.id,
                name -> {
                    profileManager.renameCurrent(name);
                    updateProfileButtonLabel();
                    Toast.makeText(context, R.string.osc_profile_renamed,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteProfileDialog() {
        if (!profileManager.canDeleteCurrent()) {
            Toast.makeText(context, R.string.osc_profile_keep_one, Toast.LENGTH_SHORT).show();
            return;
        }

        String name = profileManager.getCurrentProfile().name;
        new AlertDialog.Builder(context)
                .setTitle(R.string.osc_profile_delete)
                .setMessage(context.getString(R.string.osc_profile_delete_confirm, name))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    profileManager.deleteCurrent();
                    profilePreferenceName = profileManager.getCurrentPreferenceName();
                    currentMode = ControllerMode.Active;
                    refreshLayout();
                    Toast.makeText(context, R.string.osc_profile_deleted,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
        buttonProfile.setVisibility(View.INVISIBLE);
    }

    public void show() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.VISIBLE);
        }

        buttonConfigure.setVisibility(View.VISIBLE);
        buttonProfile.setVisibility(View.VISIBLE);
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
        frame_layout.removeView(buttonProfile);
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
            if (element instanceof MappedInputButton || element instanceof DisplaySwitchButton ||
                    element instanceof RadialMenuButton ||
                    element instanceof BlankAreaModeButton) {
                mappedButtons.add(element);
            }
        }

        for (VirtualControllerElement element : mappedButtons) {
            element.releaseInput();
            frame_layout.removeView(element);
            elements.remove(element);
        }
    }

    void removeKeyboardMouseSticks() {
        List<VirtualControllerElement> sticks = new ArrayList<>();
        for (VirtualControllerElement element : elements) {
            if (element instanceof KeyboardAnalogStick || element instanceof MouseAimZone) {
                sticks.add(element);
            }
        }

        for (VirtualControllerElement element : sticks) {
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

    void removeDisplaySwitchButton(DisplaySwitchButton button) {
        if (!elements.remove(button)) {
            return;
        }

        button.releaseInput();
        frame_layout.removeView(button);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_display_switch_deleted, Toast.LENGTH_SHORT).show();
    }

    void removeRadialMenuButton(RadialMenuButton button) {
        if (!elements.remove(button)) {
            return;
        }

        button.releaseInput();
        frame_layout.removeView(button);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_radial_deleted, Toast.LENGTH_SHORT).show();
    }

    void removeBlankAreaModeButton(BlankAreaModeButton button) {
        if (!elements.remove(button)) {
            return;
        }

        button.releaseInput();
        frame_layout.removeView(button);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_blank_mode_deleted, Toast.LENGTH_SHORT).show();
    }

    void removeVirtualStick(VirtualControllerElement stick) {
        if (!(stick instanceof KeyboardAnalogStick) || !elements.remove(stick)) {
            return;
        }

        stick.releaseInput();
        frame_layout.removeView(stick);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_stick_deleted, Toast.LENGTH_SHORT).show();
    }

    void removeAimZone(MouseAimZone aimZone) {
        if (!elements.remove(aimZone)) {
            return;
        }

        aimZone.releaseInput();
        frame_layout.removeView(aimZone);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        Toast.makeText(context, R.string.osc_aim_zone_deleted, Toast.LENGTH_SHORT).show();
    }

    private boolean hasElement(int elementId) {
        for (VirtualControllerElement element : elements) {
            if (element.elementId == elementId) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDisplaySwitchButton() {
        for (VirtualControllerElement element : elements) {
            if (element instanceof DisplaySwitchButton) {
                return true;
            }
        }
        return false;
    }

    boolean hasBlankAreaModeButton() {
        for (VirtualControllerElement element : elements) {
            if (element instanceof BlankAreaModeButton) {
                return true;
            }
        }
        return false;
    }

    void syncBlankAreaInputSettings() {
        for (VirtualControllerElement element : elements) {
            if (element instanceof BlankAreaModeButton) {
                ((BlankAreaModeButton) element).applyActiveSettings();
                return;
            }
        }
    }

    private void showAddControlDialog() {
        if (!isKeyboardMouseMode()) {
            return;
        }

        List<CharSequence> choices = new ArrayList<>();
        List<Integer> choiceTypes = new ArrayList<>();
        choices.add(context.getString(R.string.osc_add_mapped_button));
        choiceTypes.add(0);
        if (!hasDisplaySwitchButton()) {
            choices.add(context.getString(R.string.osc_add_display_switch));
            choiceTypes.add(3);
        }
        if (!hasBlankAreaModeButton()) {
            choices.add(context.getString(R.string.osc_add_blank_mode_button));
            choiceTypes.add(5);
        }
        choices.add(context.getString(R.string.osc_add_radial_menu));
        choiceTypes.add(4);
        if (!hasElement(VirtualControllerElement.EID_KEYBOARD_LS)) {
            choices.add(context.getString(R.string.osc_add_keyboard_stick));
            choiceTypes.add(1);
        }
        if (!hasElement(VirtualControllerElement.EID_MOUSE_RS)) {
            choices.add(context.getString(R.string.osc_add_aim_zone));
            choiceTypes.add(2);
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.osc_add_control)
                .setItems(choices.toArray(new CharSequence[0]), (dialog, which) -> {
                    switch (choiceTypes.get(which)) {
                        case 1:
                            addKeyboardStick();
                            break;
                        case 2:
                            addAimZone();
                            break;
                        case 3:
                            addDisplaySwitchButton();
                            break;
                        case 4:
                            addRadialMenuButton();
                            break;
                        case 5:
                            addBlankAreaModeButton();
                            break;
                        default:
                            addMappedButton();
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addMappedButton() {
        if (!isKeyboardMouseMode()) {
            return;
        }

        int elementId = VirtualControllerElement.EID_MAPPED_CUSTOM_START;
        int mappedButtonCount = 0;
        for (VirtualControllerElement element : elements) {
            if (element instanceof MappedInputButton || element instanceof DisplaySwitchButton ||
                    element instanceof RadialMenuButton ||
                    element instanceof BlankAreaModeButton) {
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

    private void addDisplaySwitchButton() {
        if (!isKeyboardMouseMode() || hasDisplaySwitchButton()) {
            return;
        }

        int elementId = VirtualControllerElement.EID_MAPPED_CUSTOM_START;
        int buttonCount = 0;
        for (VirtualControllerElement element : elements) {
            if (element instanceof MappedInputButton || element instanceof DisplaySwitchButton ||
                    element instanceof RadialMenuButton ||
                    element instanceof BlankAreaModeButton) {
                buttonCount++;
                elementId = Math.max(elementId, element.elementId + 1);
            }
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int buttonSize = (int) (screen.heightPixels * 0.13f);
        int cascade = (buttonCount % 5) * (buttonSize / 5);
        int x = Math.max(0, (screen.widthPixels - buttonSize) / 2 + cascade);
        int y = Math.max(0, (screen.heightPixels - buttonSize) / 2 + cascade);

        DisplaySwitchButton button = new DisplaySwitchButton(this, elementId, 10, context);
        addElement(button, x, y, buttonSize, buttonSize);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        button.showBindingDialog();
    }

    private void addRadialMenuButton() {
        if (!isKeyboardMouseMode()) {
            return;
        }

        int elementId = VirtualControllerElement.EID_MAPPED_CUSTOM_START;
        int buttonCount = 0;
        for (VirtualControllerElement element : elements) {
            if (element instanceof MappedInputButton || element instanceof DisplaySwitchButton ||
                    element instanceof RadialMenuButton ||
                    element instanceof BlankAreaModeButton) {
                buttonCount++;
                elementId = Math.max(elementId, element.elementId + 1);
            }
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int buttonSize = (int) (screen.heightPixels * 0.13f);
        int cascade = (buttonCount % 5) * (buttonSize / 5);
        int x = Math.max(0, (screen.widthPixels - buttonSize) / 2 + cascade);
        int y = Math.max(0, (screen.heightPixels - buttonSize) / 2 + cascade);

        RadialMenuButton button = new RadialMenuButton(this, elementId, context);
        addElement(button, x, y, buttonSize, buttonSize);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        button.showBindingDialog();
    }

    private void addBlankAreaModeButton() {
        if (!isKeyboardMouseMode() || hasBlankAreaModeButton()) {
            return;
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int buttonSize = (int) (screen.heightPixels * 0.13f);
        BlankAreaModeButton button = new BlankAreaModeButton(this,
                VirtualControllerElement.EID_BLANK_AREA_MODE, 10, context);
        addElement(button, Math.max(0, (screen.widthPixels - buttonSize) / 2),
                Math.max(0, (screen.heightPixels - buttonSize) / 2),
                buttonSize, buttonSize);
        button.applyActiveSettings();
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        button.showBindingDialog();
    }

    private void addKeyboardStick() {
        if (hasElement(VirtualControllerElement.EID_KEYBOARD_LS)) {
            return;
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int stickSize = (int) (screen.heightPixels * 0.44f);
        KeyboardAnalogStick stick = new KeyboardAnalogStick(this, context,
                VirtualControllerElement.EID_KEYBOARD_LS);
        addElement(stick, (int) (screen.heightPixels * 0.04f),
                (int) (screen.heightPixels * 0.48f), stickSize, stickSize);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        stick.showBindingDialog();
    }

    private void addAimZone() {
        if (hasElement(VirtualControllerElement.EID_MOUSE_RS)) {
            return;
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int zoneWidth = (int) (screen.heightPixels * 58f / 72f);
        int zoneHeight = (int) (screen.heightPixels * 62f / 72f);
        int x = screen.widthPixels - (int) (screen.heightPixels * 60f / 72f);
        MouseAimZone aimZone = new MouseAimZone(this, context,
                VirtualControllerElement.EID_MOUSE_RS);
        addElement(aimZone, Math.max(0, x), (int) (screen.heightPixels * 5f / 72f),
                zoneWidth, zoneHeight);
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        aimZone.showBindingDialog();
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

        int profileButtonWidth = buttonSize * 2;
        FrameLayout.LayoutParams profileParams = new FrameLayout.LayoutParams(
                profileButtonWidth, buttonSize);
        profileParams.leftMargin = 15 + buttonSize + 8;
        profileParams.topMargin = 15;
        frame_layout.addView(buttonProfile, profileParams);
        updateProfileButtonLabel();

        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        addParams.leftMargin = profileParams.leftMargin + profileButtonWidth + 8;
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
