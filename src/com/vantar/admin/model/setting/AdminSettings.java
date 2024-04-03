package com.vantar.admin.model.setting;

import com.vantar.admin.model.index.Admin;
import com.vantar.common.*;
import com.vantar.exception.FinishException;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.util.file.FileUtil;
import com.vantar.util.json.Json;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminSettings {

    public static void settingsReload(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SETTINGS_RELOAD, params, response, true);

        reloadSettings();
        Services.messaging.broadcast(VantarParam.MESSAGE_SETTINGS_UPDATED);
        ui.sleepMs(1000);

        ui.beginBox("config.properties");
        printProperties(ui, Settings.config);
        ui.blockEnd();
        ui.addEmptyLine(2);

        ui.beginBox("tune.properties");
        printProperties(ui, Settings.tune);
        ui.blockEnd();

        ui.finish();
    }

    private static void printProperties(WebUi ui, Settings.Common setting) {
        List<String> settings = new ArrayList<>(setting.propertyNames());
        Collections.sort(settings);
        String cat = null;
        for (String item : settings) {
            String itemCat = StringUtil.split(item, '.')[0];
            if (!itemCat.equals(cat)) {
                cat = itemCat;
                ui.addHeading(3, cat, "h3-margin");
            }
            ui.addKeyValue(item, setting.getProperty(item), "long");
        }
    }

    public static void reloadSettings() {
        Settings.config.reload();
        Settings.tune.reload();
    }

    public static void settingsEdit(Params params, HttpServletResponse response, Class<?> configInterface) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_SETTINGS_EDIT, params, response, true);

        String filename = getPropertiesFilename(configInterface);
        if (filename == null) {
            ui.addErrorMessage(VantarKey.ADMIN_SETTINGS_MSG1).finish();
            return;
        }

        org.aeonbits.owner.Accessible properties;
        Class<?> propertiesInterface;
        if (StringUtil.contains(filename, "config")) {
            properties = Settings.config;
            propertiesInterface = Settings.configClass;
            ui.addMessage(VantarKey.ADMIN_SETTINGS_MSG2);
        } else {
            properties = Settings.tune;
            propertiesInterface = Settings.tuneClass;
            ui.addMessage(VantarKey.ADMIN_SETTINGS_MSG3);
        }
        ui  .addEmptyLine()
            .addHeading(3, filename);

        if (params.isChecked("f")) {
            Map<String, String> settings = new HashMap<>(20, 1);
            for (String item : properties.propertyNames()) {
                ((org.aeonbits.owner.Mutable) properties).setProperty(item, params.getString(item));
                settings.put(item, properties.getProperty(item));
            }

            String json = Json.d.toJson(settings);
            if (updateProperties(json, properties, propertiesInterface)) {
                ui.addMessage(VantarKey.ADMIN_SETTINGS_UPDATED);
                reloadSettings();
                ui.addMessage(VantarKey.ADMIN_SETTINGS_LOADED);
                if (propertiesInterface.equals(Settings.configClass)) {
                    Services.messaging.broadcast(VantarParam.MESSAGE_UPDATE_SETTINGS, json);
                    ui.addMessage(VantarKey.ADMIN_SETTINGS_UPDATE_MSG_SENT);
                }
            } else {
                ui.addErrorMessage(VantarKey.ADMIN_SETTINGS_UPDATE_FAILED);
            }
        }

        ui.beginFormPost();

        List<String> settings = new ArrayList<>(properties.propertyNames());
        Collections.sort(settings);
        String cat = null;
        for (String item : settings) {
            String itemCat = StringUtil.split(item, '.')[0];
            if (!itemCat.equals(cat)) {
                cat = itemCat;
                ui.addHeading(3, cat, "h3-margin");
            }

            String value = properties.getProperty(item);

            if (value.contains("\n")) {
                ui.addTextArea(item, item, value, "ltr");
            } else {
                ui.addInput(item, item, value, null, null, "ltr");
            }
        }

        ui.addSubmit(VantarKey.ADMIN_EDIT).finish();
    }

    public static boolean updateProperties(String json, org.aeonbits.owner.Accessible properties, Class<?> propertiesInterface) {
        Map<String, String> updatedSettings = Json.d.mapFromJson(json, String.class, String.class);
        if (updatedSettings == null) {
            return false;
        }

        List<String> settings = new ArrayList<>(properties.propertyNames());
        Collections.sort(settings);

        Map<String, String> newSettings = new LinkedHashMap<>(20, 1);
        for (String item : settings) {
            ((org.aeonbits.owner.Mutable) properties).setProperty(item, updatedSettings.get(item));
            newSettings.put(item, properties.getProperty(item));
        }

        String filename = getPropertiesFilename(propertiesInterface);
        return filename != null && FileUtil.updateProperties(filename, newSettings);
    }

    private static String getPropertiesFilename(Class<?> propertiesInterface) {
        for (String value : propertiesInterface.getAnnotation(org.aeonbits.owner.Config.Sources.class).value()) {
            if (value.startsWith("file:")) {
                return StringUtil.remove(value, "file:");
            }
        }
        return null;
    }
}