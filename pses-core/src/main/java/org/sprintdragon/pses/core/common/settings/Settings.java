package org.sprintdragon.pses.core.common.settings;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.common.Strings;
import org.sprintdragon.pses.core.common.unit.TimeValue;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sprintdragon.pses.core.common.unit.TimeValue.parseTimeValue;

/**
 * Created by wangdi on 17-8-2.
 */
@Component
public class Settings extends HashMap<String, String> {

    public static final Settings EMPTY = new Builder().build();
    private static final Pattern ARRAY_PATTERN = Pattern.compile("(.*)\\.\\d+$");

    public Settings() {
    }

    public Settings(Map<String, String> stringStringMap) {
        putAll(stringStringMap);
    }

    @PostConstruct
    public void init() throws Exception {
        put("bind_host", "0.0.0.0");
        put("network.host", "0.0.0.0");
        put("port", "9090");
        put("name", "psiitoy");
    }


    public static class Builder {

        public static final Settings EMPTY_SETTINGS = new Builder().build();

        private final Map<String, String> map = new LinkedHashMap<>();

        private Builder() {

        }

        public Map<String, String> internalMap() {
            return this.map;
        }

        /**
         * Removes the provided setting from the internal map holding the current list of settings.
         */
        public String remove(String key) {
            return map.remove(key);
        }

        /**
         * Returns a setting value based on the setting key.
         */
        public String get(String key) {
            String retVal = map.get(key);
            if (retVal != null) {
                return retVal;
            }
            // try camel case version
            return map.get(toCamelCase(key));
        }

        /**
         * Returns the setting value associated with the setting key. If it does not exists,
         * returns the default value provided.
         */
        public String get(String setting, String defaultValue) {
            String retVal = get(setting);
            return retVal == null ? defaultValue : retVal;
        }


        /**
         * Puts tuples of key value pairs of settings. Simplified version instead of repeating calling
         * put for each one.
         */
        public Builder put(Object... settings) {
            if (settings.length == 1) {
                // support cases where the actual type gets lost down the road...
                if (settings[0] instanceof Map) {
                    //noinspection unchecked
                    return put((Map) settings[0]);
                } else if (settings[0] instanceof Settings) {
                    return put((Settings) settings[0]);
                }
            }
            if ((settings.length % 2) != 0) {
                throw new IllegalArgumentException("array settings of key + value order doesn't hold correct number of arguments (" + settings.length + ")");
            }
            for (int i = 0; i < settings.length; i++) {
                put(settings[i++].toString(), settings[i].toString());
            }
            return this;
        }

        /**
         * Sets a setting with the provided setting key and value.
         *
         * @param key   The setting key
         * @param value The setting value
         * @return The builder
         */
        public Builder put(String key, String value) {
            map.put(key, value);
            return this;
        }

        /**
         * Sets a setting with the provided setting key and class as value.
         *
         * @param key   The setting key
         * @param clazz The setting class value
         * @return The builder
         */
        public Builder put(String key, Class clazz) {
            map.put(key, clazz.getName());
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the boolean value.
         *
         * @param setting The setting key
         * @param value   The boolean value
         * @return The builder
         */
        public Builder put(String setting, boolean value) {
            put(setting, String.valueOf(value));
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the int value.
         *
         * @param setting The setting key
         * @param value   The int value
         * @return The builder
         */
        public Builder put(String setting, int value) {
            put(setting, String.valueOf(value));
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the long value.
         *
         * @param setting The setting key
         * @param value   The long value
         * @return The builder
         */
        public Builder put(String setting, long value) {
            put(setting, String.valueOf(value));
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the float value.
         *
         * @param setting The setting key
         * @param value   The float value
         * @return The builder
         */
        public Builder put(String setting, float value) {
            put(setting, String.valueOf(value));
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the double value.
         *
         * @param setting The setting key
         * @param value   The double value
         * @return The builder
         */
        public Builder put(String setting, double value) {
            put(setting, String.valueOf(value));
            return this;
        }

        /**
         * Sets the setting with the provided setting key and the time value.
         *
         * @param setting The setting key
         * @param value   The time value
         * @return The builder
         */
        public Builder put(String setting, long value, TimeUnit timeUnit) {
            put(setting, timeUnit.toMillis(value) + "ms");
            return this;
        }

        /**
         * Sets the setting with the provided setting key and an array of values.
         *
         * @param setting The setting key
         * @param values  The values
         * @return The builder
         */
        public Builder putArray(String setting, String... values) {
            remove(setting);
            int counter = 0;
            while (true) {
                String value = map.remove(setting + '.' + (counter++));
                if (value == null) {
                    break;
                }
            }
            for (int i = 0; i < values.length; i++) {
                put(setting + "." + i, values[i]);
            }
            return this;
        }

        /**
         * Sets the setting as an array of values, but keeps existing elements for the key.
         */
        public Builder extendArray(String setting, String... values) {
            // check for a singular (non array) value
            String oldSingle = remove(setting);
            // find the highest array index
            int counter = 0;
            while (map.containsKey(setting + '.' + counter)) {
                ++counter;
            }
            if (oldSingle != null) {
                put(setting + '.' + counter++, oldSingle);
            }
            for (String value : values) {
                put(setting + '.' + counter++, value);
            }
            return this;
        }

        /**
         * Sets all the provided settings.
         */
        public Builder put(Settings settings) {
            removeNonArraysFieldsIfNewSettingsContainsFieldAsArray(settings);
            map.putAll(settings);
            return this;
        }

        /**
         * Sets all the provided settings.
         */
        public Builder put(Map<String, String> settings) {
            removeNonArraysFieldsIfNewSettingsContainsFieldAsArray(settings);
            map.putAll(settings);
            return this;
        }

        /**
         * Removes non array values from the existing map, if settings contains an array value instead
         * <p>
         * Example:
         * Existing map contains: {key:value}
         * New map contains: {key:[value1,value2]} (which has been flattened to {}key.0:value1,key.1:value2})
         * <p>
         * This ensure that that the 'key' field gets removed from the map in order to override all the
         * data instead of merging
         */
        private void removeNonArraysFieldsIfNewSettingsContainsFieldAsArray(Map<String, String> settings) {
            List<String> prefixesToRemove = new ArrayList<>();
            for (final Map.Entry<String, String> entry : settings.entrySet()) {
                final Matcher matcher = ARRAY_PATTERN.matcher(entry.getKey());
                if (matcher.matches()) {
                    prefixesToRemove.add(matcher.group(1));
                } else if (Iterables.any(map.keySet(), startsWith(entry.getKey() + "."))) {
                    prefixesToRemove.add(entry.getKey());
                }
            }
            for (String prefix : prefixesToRemove) {
                Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    if (entry.getKey().startsWith(prefix + ".") || entry.getKey().equals(prefix)) {
                        iterator.remove();
                    }
                }
            }
        }

        /**
         * Sets all the provided settings.
         */
        public Builder put(Properties properties) {
            for (Map.Entry entry : properties.entrySet()) {
                map.put((String) entry.getKey(), (String) entry.getValue());
            }
            return this;
        }

        public Builder loadFromDelimitedString(String value, char delimiter) {
            String[] values = Strings.splitStringToArray(value, delimiter);
            for (String s : values) {
                int index = s.indexOf('=');
                if (index == -1) {
                    throw new IllegalArgumentException("value [" + s + "] for settings loaded with delimiter [" + delimiter + "] is malformed, missing =");
                }
                map.put(s.substring(0, index), s.substring(index + 1));
            }
            return this;
        }


        /**
         * Puts all the properties with keys starting with the provided <tt>prefix</tt>.
         *
         * @param prefix     The prefix to filter property key by
         * @param properties The properties to put
         * @return The builder
         */
        public Builder putProperties(String prefix, Properties properties) {
            for (Object key1 : properties.keySet()) {
                String key = (String) key1;
                String value = properties.getProperty(key);
                if (key.startsWith(prefix)) {
                    map.put(key.substring(prefix.length()), value);
                }
            }
            return this;
        }

        /**
         * Puts all the properties with keys starting with the provided <tt>prefix</tt>.
         *
         * @param prefix     The prefix to filter property key by
         * @param properties The properties to put
         * @return The builder
         */
        public Builder putProperties(String prefix, Properties properties, String[] ignorePrefixes) {
            for (Object key1 : properties.keySet()) {
                String key = (String) key1;
                String value = properties.getProperty(key);
                if (key.startsWith(prefix)) {
                    boolean ignore = false;
                    for (String ignorePrefix : ignorePrefixes) {
                        if (key.startsWith(ignorePrefix)) {
                            ignore = true;
                            break;
                        }
                    }
                    if (!ignore) {
                        map.put(key.substring(prefix.length()), value);
                    }
                }
            }
            return this;
        }

//        /**
//         * Runs across all the settings set on this builder and replaces <tt>${...}</tt> elements in the
//         * each setting value according to the following logic:
//         * <p/>
//         * <p>First, tries to resolve it against a System property ({@link System#getProperty(String)}), next,
//         * tries and resolve it against an environment variable ({@link System#getenv(String)}), and last, tries
//         * and replace it with another setting already set on this builder.
//         */
//        public Builder replacePropertyPlaceholders() {
//            PropertyPlaceholder propertyPlaceholder = new PropertyPlaceholder("${", "}", false);
//            PropertyPlaceholder.PlaceholderResolver placeholderResolver = new PropertyPlaceholder.PlaceholderResolver() {
//                @Override
//                public String resolvePlaceholder(String placeholderName) {
//                    if (placeholderName.startsWith("env.")) {
//                        // explicit env var prefix
//                        return System.getenv(placeholderName.substring("env.".length()));
//                    }
//                    String value = System.getProperty(placeholderName);
//                    if (value != null) {
//                        return value;
//                    }
//                    value = System.getenv(placeholderName);
//                    if (value != null) {
//                        return value;
//                    }
//                    return map.get(placeholderName);
//                }
//
//                @Override
//                public boolean shouldIgnoreMissing(String placeholderName) {
//                    // if its an explicit env var, we are ok with not having a value for it and treat it as optional
//                    if (placeholderName.startsWith("env.") || placeholderName.startsWith("prompt.")) {
//                        return true;
//                    }
//                    return false;
//                }
//
//                @Override
//                public boolean shouldRemoveMissingPlaceholder(String placeholderName) {
//                    if (placeholderName.startsWith("prompt.")) {
//                        return false;
//                    }
//                    return true;
//                }
//            };
//            for (Map.Entry<String, String> entry : Maps.newHashMap(map).entrySet()) {
//                String value = propertyPlaceholder.replacePlaceholders(entry.getValue(), placeholderResolver);
//                // if the values exists and has length, we should maintain it  in the map
//                // otherwise, the replace process resolved into removing it
//                if (Strings.hasLength(value)) {
//                    map.put(entry.getKey(), value);
//                } else {
//                    map.remove(entry.getKey());
//                }
//            }
//            return this;
//        }

        /**
         * Checks that all settings in the builder start with the specified prefix.
         * <p>
         * If a setting doesn't start with the prefix, the builder appends the prefix to such setting.
         */
        public Builder normalizePrefix(String prefix) {
            Map<String, String> replacements = Maps.newHashMap();
            Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                if (entry.getKey().startsWith(prefix) == false) {
                    replacements.put(prefix + entry.getKey(), entry.getValue());
                    iterator.remove();
                }
            }
            map.putAll(replacements);
            return this;
        }

        /**
         * Builds a {@link Settings} (underlying uses {@link Settings}) based on everything
         * set on this builder.
         */
        public Settings build() {
            return new Settings(Collections.unmodifiableMap(map));
        }
    }

    public static String toCamelCase(String value) {
        return toCamelCase(value, null);
    }

    public static String toCamelCase(String value, StringBuilder sb) {
        boolean changed = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            //e.g. _name stays as-is, _first_name becomes _firstName
            if (c == '_' && i > 0) {
                if (!changed) {
                    if (sb != null) {
                        sb.setLength(0);
                    } else {
                        sb = new StringBuilder();
                    }
                    // copy it over here
                    for (int j = 0; j < i; j++) {
                        sb.append(value.charAt(j));
                    }
                    changed = true;
                }
                if (i < value.length() - 1) {
                    sb.append(Character.toUpperCase(value.charAt(++i)));
                }
            } else {
                if (changed) {
                    sb.append(c);
                }
            }
        }
        if (!changed) {
            return value;
        }
        return sb.toString();
    }

    public static String toUnderscoreCase(String value) {
        return toUnderscoreCase(value, null);
    }

    public static String toUnderscoreCase(String value, StringBuilder sb) {
        boolean changed = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!changed) {
                    if (sb != null) {
                        sb.setLength(0);
                    } else {
                        sb = new StringBuilder();
                    }
                    // copy it over here
                    for (int j = 0; j < i; j++) {
                        sb.append(value.charAt(j));
                    }
                    changed = true;
                    if (i == 0) {
                        sb.append(Character.toLowerCase(c));
                    } else {
                        sb.append('_');
                        sb.append(Character.toLowerCase(c));
                    }
                } else {
                    sb.append('_');
                    sb.append(Character.toLowerCase(c));
                }
            } else {
                if (changed) {
                    sb.append(c);
                }
            }
        }
        if (!changed) {
            return value;
        }
        return sb.toString();
    }

    private static StartsWithPredicate startsWith(String prefix) {
        return new StartsWithPredicate(prefix);
    }

    private static final class StartsWithPredicate implements Predicate<String> {

        private String prefix;

        public StartsWithPredicate(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean apply(String input) {
            return input.startsWith(prefix);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder to be used in order to build settings.
     */
    public static Builder settingsBuilder() {
        return new Builder();
    }


    /**
     * Returns group settings for the given setting prefix.
     */
    public Map<String, Settings> getGroups(String settingPrefix) throws SettingsException {
        return getGroups(settingPrefix, false);
    }

    /**
     * Returns group settings for the given setting prefix.
     */
    public Map<String, Settings> getGroups(String settingPrefix, boolean ignoreNonGrouped) throws SettingsException {
        if (!Strings.hasLength(settingPrefix)) {
            throw new IllegalArgumentException("illegal setting prefix " + settingPrefix);
        }
        if (settingPrefix.charAt(settingPrefix.length() - 1) != '.') {
            settingPrefix = settingPrefix + ".";
        }
        // we don't really care that it might happen twice
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        for (Object o : this.keySet()) {
            String setting = (String) o;
            if (setting.startsWith(settingPrefix)) {
                String nameValue = setting.substring(settingPrefix.length());
                int dotIndex = nameValue.indexOf('.');
                if (dotIndex == -1) {
                    if (ignoreNonGrouped) {
                        continue;
                    }
                    throw new SettingsException("Failed to get setting group for [" + settingPrefix + "] setting prefix and setting [" + setting + "] because of a missing '.'");
                }
                String name = nameValue.substring(0, dotIndex);
                String value = nameValue.substring(dotIndex + 1);
                Map<String, String> groupSettings = map.get(name);
                if (groupSettings == null) {
                    groupSettings = new LinkedHashMap<>();
                    map.put(name, groupSettings);
                }
                groupSettings.put(value, get(setting));
            }
        }
        Map<String, Settings> retVal = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            retVal.put(entry.getKey(), new Settings(Collections.unmodifiableMap(entry.getValue())));
        }
        return Collections.unmodifiableMap(retVal);
    }


    /**
     * Returns the setting value (as float) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public Float getAsFloat(String setting, Float defaultValue) {
        String sValue = get(setting);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse float setting [" + setting + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as float) associated with teh first setting key, if none
     * exists, returns the default value provided.
     */
    public Float getAsFloat(String[] settings, Float defaultValue) throws SettingsException {
        String sValue = get(settings);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse float setting [" + Arrays.toString(settings) + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as double) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public Double getAsDouble(String setting, Double defaultValue) {
        String sValue = get(setting);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse double setting [" + setting + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as double) associated with teh first setting key, if none
     * exists, returns the default value provided.
     */
    public Double getAsDouble(String[] settings, Double defaultValue) {
        String sValue = get(settings);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse double setting [" + Arrays.toString(settings) + "] with value [" + sValue + "]", e);
        }
    }


    /**
     * Returns the setting value (as int) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public Integer getAsInt(String setting, Integer defaultValue) {
        String sValue = get(setting);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse int setting [" + setting + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as int) associated with the first setting key. If it does not exists,
     * returns the default value provided.
     */
    public Integer getAsInt(String[] settings, Integer defaultValue) {
        String sValue = get(settings);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse int setting [" + Arrays.toString(settings) + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as long) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public Long getAsLong(String setting, Long defaultValue) {
        String sValue = get(setting);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse long setting [" + setting + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as long) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public Long getAsLong(String[] settings, Long defaultValue) {
        String sValue = get(settings);
        if (sValue == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(sValue);
        } catch (NumberFormatException e) {
            throw new SettingsException("Failed to parse long setting [" + Arrays.toString(settings) + "] with value [" + sValue + "]", e);
        }
    }

    /**
     * Returns the setting value (as time) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public TimeValue getAsTime(String setting, TimeValue defaultValue) {
        return parseTimeValue(get(setting), defaultValue, setting);
    }

    /**
     * Returns the setting value (as time) associated with the setting key. If it does not exists,
     * returns the default value provided.
     */
    public TimeValue getAsTime(String[] settings, TimeValue defaultValue) {
        // NOTE: duplicated from get(String[]) so we can pass which setting name was actually used to parseTimeValue:
        for (String setting : settings) {
            String retVal = get(setting);
            if (retVal != null) {
                parseTimeValue(get(settings), defaultValue, setting);
            }
        }
        return defaultValue;
    }

}
