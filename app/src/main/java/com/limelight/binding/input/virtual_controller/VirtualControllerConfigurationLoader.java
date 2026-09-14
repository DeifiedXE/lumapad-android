/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.preferences.PreferenceConfiguration;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class VirtualControllerConfigurationLoader {
    public static final String OSC_PREFERENCE = "OSC";
    private static final String MAPPED_BUTTON_IDS = "MAPPED_BUTTON_IDS";
    private static final String STICK_IDS = "STICK_IDS";

    private static int getPercent(
            int percent,
            int total) {
        return (int) (((float) total / (float) 100) * (float) percent);
    }

    // The default controls are specified using a grid of 128*72 cells at 16:9
    private static int screenScale(int units, int height) {
        return (int) (((float) height / (float) 72) * (float) units);
    }

    private static DigitalPad createDigitalPad(
            final VirtualController controller,
            final Context context) {

        DigitalPad digitalPad = new DigitalPad(controller, context);
        digitalPad.addDigitalPadListener(new DigitalPad.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();

                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    inputContext.inputMap |= ControllerPacket.LEFT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.LEFT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    inputContext.inputMap |= ControllerPacket.RIGHT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.RIGHT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    inputContext.inputMap |= ControllerPacket.UP_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.UP_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
                    inputContext.inputMap |= ControllerPacket.DOWN_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.DOWN_FLAG;
                }

                controller.sendControllerInputContext();
            }
        });

        return digitalPad;
    }

    private static DigitalButton createDigitalButton(
            final int elementId,
            final int keyShort,
            final int keyLong,
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyShort;

                controller.sendControllerInputContext();
            }

            @Override
            public void onLongClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyLong;

                controller.sendControllerInputContext();
            }

            @Override
            public void onRelease() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap &= ~keyShort;
                inputContext.inputMap &= ~keyLong;

                controller.sendControllerInputContext();
            }
        });

        return button;
    }

    private static DigitalButton createLeftTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        LeftTrigger button = new LeftTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        return button;
    }

    private static DigitalButton createRightTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        RightTrigger button = new RightTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        return button;
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context) {
        return new LeftAnalogStick(controller, context);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context) {
        return new RightAnalogStick(controller, context);
    }

    private static MappedInputButton createMappedButton(
            final int elementId,
            final String bindingId,
            final int layer,
            final VirtualController controller,
            final Context context) {
        return new MappedInputButton(controller, elementId, layer, bindingId, context);
    }


    private static final int TRIGGER_L_BASE_X = 1;
    private static final int TRIGGER_R_BASE_X = 92;
    private static final int TRIGGER_DISTANCE = 23;
    private static final int TRIGGER_BASE_Y = 31;
    private static final int TRIGGER_WIDTH = 12;
    private static final int TRIGGER_HEIGHT = 9;

    // Face buttons are defined based on the Y button (button number 9)
    private static final int BUTTON_BASE_X = 106;
    private static final int BUTTON_BASE_Y = 1;
    private static final int BUTTON_SIZE = 10;

    private static final int DPAD_BASE_X = 4;
    private static final int DPAD_BASE_Y = 41;
    private static final int DPAD_SIZE = 30;

    private static final int ANALOG_L_BASE_X = 6;
    private static final int ANALOG_L_BASE_Y = 4;
    private static final int ANALOG_R_BASE_X = 98;
    private static final int ANALOG_R_BASE_Y = 42;
    private static final int ANALOG_SIZE = 26;

    private static final int L3_R3_BASE_Y = 60;

    private static final int START_X = 83;
    private static final int BACK_X = 34;
    private static final int START_BACK_Y = 64;
    private static final int START_BACK_WIDTH = 12;
    private static final int START_BACK_HEIGHT = 7;

    // Make the Guide Menu be in the center of START and BACK menu
    private static final int GUIDE_X = START_X-BACK_X;
    private static final int GUIDE_Y = START_BACK_Y;

    public static void createDefaultLayout(final VirtualController controller, final Context context) {

        if (controller.isKeyboardMouseMode()) {
            createKeyboardMouseLayout(controller, context);
            return;
        }

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        // Displace controls on the right by this amount of pixels to account for different aspect ratios
        int rightDisplacement = screen.widthPixels - screen.heightPixels * 16 / 9;

        int height = screen.heightPixels;

        // NOTE: Some of these getPercent() expressions seem like they can be combined
        // into a single call. Due to floating point rounding, this isn't actually possible.

        if (!config.onlyL3R3)
        {
            controller.addElement(createDigitalPad(controller, context),
                    screenScale(DPAD_BASE_X, height),
                    screenScale(DPAD_BASE_Y, height),
                    screenScale(DPAD_SIZE, height),
                    screenScale(DPAD_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_A,
                    !config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    !config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + 2 * BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_B,
                    config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X + BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_X,
                    !config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    !config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X - BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_Y,
                    config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createLeftTrigger(
                    1, "LT", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createRightTrigger(
                    1, "RT", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_LB,
                    ControllerPacket.LB_FLAG, 0, 1, "LB", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X + TRIGGER_DISTANCE, height),
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RB,
                    ControllerPacket.RB_FLAG, 0, 1, "RB", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createLeftStick(controller, context),
                    screenScale(ANALOG_L_BASE_X, height),
                    screenScale(ANALOG_L_BASE_Y, height),
                    screenScale(ANALOG_SIZE, height),
                    screenScale(ANALOG_SIZE, height)
            );

            controller.addElement(createRightStick(controller, context),
                    screenScale(ANALOG_R_BASE_X, height) + rightDisplacement,
                    screenScale(ANALOG_R_BASE_Y, height),
                    screenScale(ANALOG_SIZE, height),
                    screenScale(ANALOG_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_BACK,
                    ControllerPacket.BACK_FLAG, 0, 2, "BACK", -1, controller, context),
                    screenScale(BACK_X, height),
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_START,
                    ControllerPacket.PLAY_FLAG, 0, 3, "START", -1, controller, context),
                    screenScale(START_X, height) + rightDisplacement,
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }
        else {
            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_LSB,
                    ControllerPacket.LS_CLK_FLAG, 0, 1, "L3", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RSB,
                    ControllerPacket.RS_CLK_FLAG, 0, 1, "R3", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );
        }

        if(config.showGuideButton){
            controller.addElement(createDigitalButton(VirtualControllerElement.EID_GDB,
                            ControllerPacket.SPECIAL_BUTTON_FLAG, 0, 1, "GUIDE", -1, controller, context),
                    screenScale(GUIDE_X, height)+ rightDisplacement,
                    screenScale(GUIDE_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }

        controller.setOpacity(config.oscOpacity);
    }

    /**
     * LumaPad's default layout for PC games without native controller support.
     * The left stick emits WASD, the right stick emits relative mouse movement,
     * and every round button can be rebound while streaming.
     */
    private static void createKeyboardMouseLayout(final VirtualController controller,
                                                   final Context context) {
        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);
        int rightDisplacement = screen.widthPixels - screen.heightPixels * 16 / 9;
        int height = screen.heightPixels;

        controller.addElement(new KeyboardAnalogStick(controller, context,
                        VirtualControllerElement.EID_KEYBOARD_LS),
                screenScale(3, height), screenScale(37, height),
                screenScale(32, height), screenScale(32, height));

        controller.addElement(new MouseAimZone(controller, context,
                        VirtualControllerElement.EID_MOUSE_RS),
                screenScale(68, height) + rightDisplacement, screenScale(5, height),
                screenScale(58, height), screenScale(62, height));

        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_LMB,
                "mouse_left", 92, 27, 14, 9, rightDisplacement);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_RMB,
                "mouse_right", 109, 27, 14, 9, rightDisplacement);

        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_Q,
                "key_q", 70, 15, 10, 10, rightDisplacement);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_W,
                "key_w", 82, 15, 10, 10, rightDisplacement);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_E,
                "key_e", 94, 15, 10, 10, rightDisplacement);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_R,
                "key_r", 106, 15, 10, 10, rightDisplacement);

        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_1,
                "key_1", 38, 2, 9, 9, 0);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_2,
                "key_2", 49, 2, 9, 9, 0);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_3,
                "key_3", 60, 2, 9, 9, 0);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_4,
                "key_4", 71, 2, 9, 9, 0);
        addMappedButton(controller, context, VirtualControllerElement.EID_MAPPED_5,
                "key_5", 82, 2, 9, 9, 0);

        controller.addElement(new DisplaySwitchButton(controller,
                        VirtualControllerElement.EID_DISPLAY_SWITCH, 10, context),
                screenScale(94, height), screenScale(2, height),
                screenScale(12, height), screenScale(9, height));

        controller.setOpacity(config.oscOpacity);
    }

    private static void addMappedButton(final VirtualController controller,
                                        final Context context,
                                        int elementId,
                                        String bindingId,
                                        int x, int y, int width, int height,
                                        int xDisplacement) {
        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int screenHeight = screen.heightPixels;
        controller.addElement(createMappedButton(elementId, bindingId, 10, controller, context),
                screenScale(x, screenHeight) + xDisplacement,
                screenScale(y, screenHeight),
                screenScale(width, screenHeight),
                screenScale(height, screenHeight));
    }

    public static void saveProfile(final VirtualController controller,
                                   final Context context) {
        SharedPreferences.Editor prefEditor = context.getSharedPreferences(
                controller.getProfilePreferenceName(), Activity.MODE_PRIVATE).edit();
        JSONArray mappedButtonIds = new JSONArray();
        JSONArray stickIds = new JSONArray();

        // Each profile is a complete snapshot. Clearing first ensures deleted controls and
        // stale custom button IDs cannot reappear on a later stream.
        prefEditor.clear();

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;
            if (element instanceof MappedInputButton || element instanceof DisplaySwitchButton) {
                mappedButtonIds.put(element.elementId);
            }
            else if (element instanceof KeyboardAnalogStick || element instanceof MouseAimZone) {
                stickIds.put(element.elementId);
            }
            try {
                prefEditor.putString(prefKey, element.getConfiguration().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (controller.isKeyboardMouseMode()) {
            prefEditor.putString(MAPPED_BUTTON_IDS, mappedButtonIds.toString());
            prefEditor.putString(STICK_IDS, stickIds.toString());
        }

        prefEditor.apply();
    }

    public static void loadFromPreferences(final VirtualController controller, final Context context) {
        SharedPreferences pref = context.getSharedPreferences(
                controller.getProfilePreferenceName(), Activity.MODE_PRIVATE);

        String savedStickIds = pref.getString(STICK_IDS, null);
        if (controller.isKeyboardMouseMode() && savedStickIds != null) {
            try {
                JSONArray stickIds = new JSONArray(savedStickIds);
                controller.removeKeyboardMouseSticks();

                DisplayMetrics screen = context.getResources().getDisplayMetrics();
                for (int i = 0; i < stickIds.length(); i++) {
                    int elementId = stickIds.getInt(i);
                    String jsonConfig = pref.getString(Integer.toString(elementId), null);
                    if (jsonConfig == null) {
                        continue;
                    }

                    try {
                        JSONObject configuration = new JSONObject(jsonConfig);
                        VirtualControllerElement stick;
                        int defaultWidth;
                        int defaultHeight;
                        int defaultX;
                        int defaultY;
                        if (elementId == VirtualControllerElement.EID_KEYBOARD_LS) {
                            stick = new KeyboardAnalogStick(controller, context, elementId);
                            defaultWidth = defaultHeight = (int) (screen.heightPixels * 0.44f);
                            defaultX = (int) (screen.heightPixels * 0.04f);
                            defaultY = (int) (screen.heightPixels * 0.48f);
                        }
                        else if (elementId == VirtualControllerElement.EID_MOUSE_RS) {
                            stick = new MouseAimZone(controller, context, elementId);
                            defaultWidth = screenScale(58, screen.heightPixels);
                            defaultHeight = screenScale(62, screen.heightPixels);
                            defaultX = screen.widthPixels - screenScale(60, screen.heightPixels);
                            defaultY = screenScale(5, screen.heightPixels);
                        }
                        else {
                            continue;
                        }

                        controller.addElement(stick,
                                configuration.optInt("LEFT", Math.max(0, defaultX)),
                                configuration.optInt("TOP", defaultY),
                                configuration.optInt("WIDTH", defaultWidth),
                                configuration.optInt("HEIGHT", defaultHeight));
                    }
                    catch (JSONException e) {
                        pref.edit().remove(Integer.toString(elementId)).apply();
                    }
                }
            }
            catch (JSONException e) {
                // Keep the default sticks if the saved inventory itself is corrupt.
                pref.edit().remove(STICK_IDS).apply();
            }
        }

        String savedMappedButtonIds = pref.getString(MAPPED_BUTTON_IDS, null);
        if (controller.isKeyboardMouseMode() && savedMappedButtonIds != null) {
            try {
                JSONArray mappedButtonIds = new JSONArray(savedMappedButtonIds);
                controller.removeMappedButtons();

                DisplayMetrics screen = context.getResources().getDisplayMetrics();
                int defaultSize = (int) (screen.heightPixels * 0.13f);
                for (int i = 0; i < mappedButtonIds.length(); i++) {
                    int elementId = mappedButtonIds.getInt(i);
                    String jsonConfig = pref.getString(Integer.toString(elementId), null);
                    if (jsonConfig == null) {
                        continue;
                    }

                    try {
                        JSONObject configuration = new JSONObject(jsonConfig);
                        VirtualControllerElement button;
                        if (configuration.optBoolean("DISPLAY_SWITCH", false)) {
                            button = new DisplaySwitchButton(controller, elementId, 10, context);
                        }
                        else {
                            String bindingId = configuration.optString("BINDING", "key_space");
                            button = createMappedButton(elementId, bindingId, 10,
                                    controller, context);
                        }
                        controller.addElement(button,
                                configuration.optInt("LEFT", (screen.widthPixels - defaultSize) / 2),
                                configuration.optInt("TOP", (screen.heightPixels - defaultSize) / 2),
                                configuration.optInt("WIDTH", defaultSize),
                                configuration.optInt("HEIGHT", defaultSize));
                    }
                    catch (JSONException e) {
                        // Skip only the corrupt button while preserving the rest of the profile.
                        pref.edit().remove(Integer.toString(elementId)).apply();
                    }
                }
            }
            catch (JSONException e) {
                // Keep the default buttons if the saved inventory itself is corrupt.
                pref.edit().remove(MAPPED_BUTTON_IDS).apply();
            }
        }

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;

            String jsonConfig = pref.getString(prefKey, null);
            if (jsonConfig != null) {
                try {
                    element.loadConfiguration(new JSONObject(jsonConfig));
                } catch (JSONException e) {
                    e.printStackTrace();

                    // Remove the corrupt element from the preferences
                    pref.edit().remove(prefKey).apply();
                }
            }
        }
    }
}
