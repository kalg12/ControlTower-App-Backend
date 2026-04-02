package com.controltower.app.settings.application;

import com.controltower.app.settings.domain.UserSetting;
import com.controltower.app.settings.domain.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String KEY_NOTIFICATION_PREFS = "notification_preferences";

    private static final Map<String, Object> DEFAULT_NOTIFICATION_PREFS = Map.of(
            "emailAlerts",   true,
            "ticketUpdates", true,
            "healthAlerts",  true,
            "licenseAlerts", true,
            "weeklyDigest",  false
    );

    private final UserSettingRepository settingRepository;
    private final ObjectMapper          objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationPreferences(UUID userId) {
        return settingRepository.findByUserIdAndKey(userId, KEY_NOTIFICATION_PREFS)
                .map(s -> parseJson(s.getValue()))
                .orElse(DEFAULT_NOTIFICATION_PREFS);
    }

    @Transactional
    public Map<String, Object> saveNotificationPreferences(UUID userId, Map<String, Object> prefs) {
        UserSetting setting = settingRepository
                .findByUserIdAndKey(userId, KEY_NOTIFICATION_PREFS)
                .orElseGet(() -> {
                    UserSetting s = new UserSetting();
                    s.setUserId(userId);
                    s.setKey(KEY_NOTIFICATION_PREFS);
                    return s;
                });
        setting.setValue(toJson(prefs));
        settingRepository.save(setting);
        return prefs;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return DEFAULT_NOTIFICATION_PREFS;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize settings", e);
        }
    }
}
