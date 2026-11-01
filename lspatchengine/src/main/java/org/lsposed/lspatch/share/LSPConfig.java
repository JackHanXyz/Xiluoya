package org.lsposed.lspatch.share;

/**
 * Xiluoya: 从 LSPatch 0.6 (398) 的构建参数固化而来。
 * 原项目用 Gradle 模板注入，这里直接写死，避免额外的代码生成步骤。
 */
public class LSPConfig {

    public static final LSPConfig instance;

    public int API_CODE;
    public int VERSION_CODE;
    public String VERSION_NAME;
    public int CORE_VERSION_CODE;
    public String CORE_VERSION_NAME;

    private LSPConfig() {
    }

    static {
        instance = new LSPConfig();
        instance.API_CODE = 93;
        instance.VERSION_CODE = 398;
        instance.VERSION_NAME = "0.6";
        instance.CORE_VERSION_CODE = 692;
        instance.CORE_VERSION_NAME = "1.9.2";
    }
}
