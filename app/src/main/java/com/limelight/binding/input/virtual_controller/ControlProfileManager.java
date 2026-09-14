package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.content.SharedPreferences;

import com.limelight.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stores named, manually selected touch-control profiles independently of the streamed app. */
public final class ControlProfileManager {
    private static final String REGISTRY_PREFERENCE = "OSC_MANUAL_PROFILE_REGISTRY";
    private static final String PROFILES_PREFIX = "PROFILES_";
    private static final String CURRENT_PREFIX = "CURRENT_";
    private static final String SKIP_LEGACY_IMPORTS = "SKIP_LEGACY_IMPORTS";

    static final class Profile {
        final String id;
        String name;

        Profile(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final Context context;
    private final String inputMode;
    private final SharedPreferences registry;
    private final List<Profile> profiles = new ArrayList<>();
    private String currentProfileId;

    ControlProfileManager(Context context, String inputMode, String legacyProfileName) {
        this.context = context;
        this.inputMode = inputMode;
        this.registry = context.getSharedPreferences(REGISTRY_PREFERENCE, Context.MODE_PRIVATE);
        readProfiles();

        boolean changed = importLegacyProfile(legacyProfileName);
        if (profiles.isEmpty()) {
            profiles.add(new Profile("default", context.getString(R.string.osc_profile_default)));
            changed = true;
        }

        currentProfileId = registry.getString(CURRENT_PREFIX + inputMode, null);
        if (findProfile(currentProfileId) == null) {
            currentProfileId = profiles.get(0).id;
            changed = true;
        }
        if (changed) saveRegistry();
    }

    private void readProfiles() {
        profiles.clear();
        String encoded = registry.getString(PROFILES_PREFIX + inputMode, null);
        if (encoded == null) return;

        try {
            JSONArray array = new JSONArray(encoded);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String id = object.optString("id", "");
                String name = object.optString("name", "").trim();
                if (!id.isEmpty() && !name.isEmpty() && findProfile(id) == null) {
                    profiles.add(new Profile(id, name));
                }
            }
        }
        catch (JSONException ignored) {
            profiles.clear();
        }
    }

    private boolean importLegacyProfile(String legacyProfileName) {
        if (registry.getBoolean(SKIP_LEGACY_IMPORTS, false)) return false;

        String stableLegacyId = legacyProfileName == null ? "desktop" :
                Integer.toHexString(legacyProfileName.hashCode());
        String profileId = "legacy_" + stableLegacyId;
        if (findProfile(profileId) != null) return false;

        String legacyPreferenceName = VirtualControllerConfigurationLoader.OSC_PREFERENCE +
                "_v2_" + stableLegacyId + "_" + inputMode;
        SharedPreferences legacyPreferences = context.getSharedPreferences(
                legacyPreferenceName, Context.MODE_PRIVATE);
        if (legacyPreferences.getAll().isEmpty()) return false;

        String baseName = legacyProfileName == null || legacyProfileName.trim().isEmpty() ?
                context.getString(R.string.osc_profile_imported) : legacyProfileName.trim();
        Profile imported = new Profile(profileId, makeUniqueName(baseName));
        profiles.add(imported);
        copyPreferences(legacyPreferences, getPreferences(imported.id));
        if (profiles.size() == 1) currentProfileId = imported.id;
        return true;
    }

    private void saveRegistry() {
        JSONArray array = new JSONArray();
        for (Profile profile : profiles) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", profile.id);
                object.put("name", profile.name);
                array.put(object);
            }
            catch (JSONException ignored) {
            }
        }
        registry.edit()
                .putString(PROFILES_PREFIX + inputMode, array.toString())
                .putString(CURRENT_PREFIX + inputMode, currentProfileId)
                .apply();
    }

    List<Profile> getProfiles() {
        return new ArrayList<>(profiles);
    }

    Profile getCurrentProfile() {
        Profile current = findProfile(currentProfileId);
        return current != null ? current : profiles.get(0);
    }

    String getCurrentPreferenceName() {
        return getPreferenceName(inputMode, getCurrentProfile().id);
    }

    boolean isCurrent(String profileId) {
        return getCurrentProfile().id.equals(profileId);
    }

    void setCurrent(String profileId) {
        if (findProfile(profileId) == null) return;
        currentProfileId = profileId;
        saveRegistry();
    }

    boolean nameExists(String name, String exceptProfileId) {
        String normalized = name.trim();
        for (Profile profile : profiles) {
            if (!profile.id.equals(exceptProfileId) && profile.name.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    Profile createProfile(String name, String sourcePreferenceName) {
        String id = "p_" + UUID.randomUUID().toString().replace("-", "");
        Profile profile = new Profile(id, name.trim());
        profiles.add(profile);
        if (sourcePreferenceName != null) {
            copyPreferences(context.getSharedPreferences(sourcePreferenceName, Context.MODE_PRIVATE),
                    getPreferences(id));
        }
        currentProfileId = id;
        saveRegistry();
        return profile;
    }

    void renameCurrent(String name) {
        getCurrentProfile().name = name.trim();
        saveRegistry();
    }

    boolean canDeleteCurrent() {
        return profiles.size() > 1;
    }

    Profile deleteCurrent() {
        Profile current = getCurrentProfile();
        if (profiles.size() <= 1) return current;
        profiles.remove(current);
        getPreferences(current.id).edit().clear().apply();
        currentProfileId = profiles.get(0).id;
        saveRegistry();
        return profiles.get(0);
    }

    private Profile findProfile(String id) {
        if (id == null) return null;
        for (Profile profile : profiles) {
            if (profile.id.equals(id)) return profile;
        }
        return null;
    }

    private String makeUniqueName(String baseName) {
        String name = baseName;
        int suffix = 2;
        while (nameExists(name, null)) {
            name = baseName + " (" + suffix++ + ")";
        }
        return name;
    }

    private SharedPreferences getPreferences(String profileId) {
        return context.getSharedPreferences(getPreferenceName(inputMode, profileId),
                Context.MODE_PRIVATE);
    }

    private static String getPreferenceName(String inputMode, String profileId) {
        return VirtualControllerConfigurationLoader.OSC_PREFERENCE + "_manual_v1_" +
                inputMode + "_" + profileId;
    }

    @SuppressWarnings("unchecked")
    private static void copyPreferences(SharedPreferences source, SharedPreferences destination) {
        SharedPreferences.Editor editor = destination.edit().clear();
        for (Map.Entry<String, ?> entry : source.getAll().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Set) {
                editor.putStringSet(entry.getKey(), (Set<String>) value);
            }
        }
        editor.apply();
    }

    public static void clearAllLayouts(Context context) {
        SharedPreferences registry = context.getSharedPreferences(
                REGISTRY_PREFERENCE, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : registry.getAll().entrySet()) {
            if (!entry.getKey().startsWith(PROFILES_PREFIX) || !(entry.getValue() instanceof String)) {
                continue;
            }
            String inputMode = entry.getKey().substring(PROFILES_PREFIX.length());
            try {
                JSONArray array = new JSONArray((String) entry.getValue());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    if (object != null) {
                        String id = object.optString("id", "");
                        if (!id.isEmpty()) {
                            context.getSharedPreferences(getPreferenceName(inputMode, id),
                                    Context.MODE_PRIVATE).edit().clear().apply();
                        }
                    }
                }
            }
            catch (JSONException ignored) {
            }
        }
        registry.edit().putBoolean(SKIP_LEGACY_IMPORTS, true).apply();
        context.getSharedPreferences(VirtualControllerConfigurationLoader.OSC_PREFERENCE,
                Context.MODE_PRIVATE).edit().clear().apply();
    }
}
