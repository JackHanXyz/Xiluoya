package moe.shizuku.manager;

import moe.shizuku.manager.utils.MultiLocaleEntity;

public class Helps {

    public static final MultiLocaleEntity ADB = new MultiLocaleEntity();
    public static final MultiLocaleEntity ADB_ANDROID11 = new MultiLocaleEntity();
    public static final MultiLocaleEntity APPS = new MultiLocaleEntity();
    public static final MultiLocaleEntity HOME = new MultiLocaleEntity();
    public static final MultiLocaleEntity DOWNLOAD = new MultiLocaleEntity();
    public static final MultiLocaleEntity SUI = new MultiLocaleEntity();
    public static final MultiLocaleEntity RISH = new MultiLocaleEntity();
    public static final MultiLocaleEntity ADB_PERMISSION = new MultiLocaleEntity();

    static {
        ADB.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB.put("en", "https://github.com/JackHanXyz/Xiluoya#readme");

        ADB_ANDROID11.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB_ANDROID11.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB_ANDROID11.put("en", "https://github.com/JackHanXyz/Xiluoya#readme");

        APPS.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya");
        APPS.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya");
        APPS.put("en", "https://github.com/JackHanXyz/Xiluoya");

        HOME.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya");
        HOME.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya");
        HOME.put("en", "https://github.com/JackHanXyz/Xiluoya");

        DOWNLOAD.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya/releases");
        DOWNLOAD.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya/releases");
        DOWNLOAD.put("en", "https://github.com/JackHanXyz/Xiluoya/releases");

        ADB_PERMISSION.put("zh-CN", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB_PERMISSION.put("zh-TW", "https://github.com/JackHanXyz/Xiluoya#readme");
        ADB_PERMISSION.put("en", "https://github.com/JackHanXyz/Xiluoya#readme");

        SUI.put("en", "https://github.com/RikkaApps/Sui");

        RISH.put("en", "https://github.com/JackHanXyz/Xiluoya");
    }
}
