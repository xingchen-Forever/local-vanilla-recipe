package dev.icey.lri.config;

/**
 * 配置数据对象（POJO）。字段直接对应模组菜单里的开关。
 * 用 GSON 序列化为 config/localrecipeinject.json。
 */
public class LriConfig {

    /** 主开关：是否强制注入本地配方。关闭即原版行为。 */
    public boolean forceLocalRecipes = false;

    /** 是否包含模组添加的配方（关闭则只补原版）。 */
    public boolean includeModdedRecipes = true;

    /** 本地配方与服务器同 id 但内容不同时是否跳过，避免误导。 */
    public boolean markConflicts = true;

    /** 跳过没有固定展示的特殊/动态配方。 */
    public boolean skipSpecialRecipes = true;

    /** 每次进入世界时打印注入统计。 */
    public boolean logInjection = true;

    /** 拷贝一份，供配置界面“取消”时回滚。 */
    public LriConfig copy() {
        LriConfig c = new LriConfig();
        c.forceLocalRecipes = this.forceLocalRecipes;
        c.includeModdedRecipes = this.includeModdedRecipes;
        c.markConflicts = this.markConflicts;
        c.skipSpecialRecipes = this.skipSpecialRecipes;
        c.logInjection = this.logInjection;
        return c;
    }
}
