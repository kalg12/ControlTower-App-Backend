package com.controltower.app.settings.application;

import com.controltower.app.settings.domain.UserSetting;
import com.controltower.app.settings.domain.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

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

    // Jackson 3 (Spring Boot 4) — instantiated statically; no Spring injection needed.
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final UserSettingRepository settingRepository;

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

    private Map<String, Object> parseJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JacksonException e) {
            return DEFAULT_NOTIFICATION_PREFS;
        }
    }

    private String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize settings", e);
        }
    }
}
