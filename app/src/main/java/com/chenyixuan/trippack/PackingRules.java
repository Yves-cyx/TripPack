package com.chenyixuan.trippack;
import java.util.ArrayList;

/** 与界面分离的物品生成规则，可独立运行测试。 */
public final class PackingRules {
    public static ArrayList<String> generate(int days, String transport, boolean business, boolean electronics) {
        if (days < 1 || days > 30) throw new IllegalArgumentException("出行天数须为1至30天");
        ArrayList<String> items = new ArrayList<>();
        items.add("身份证件");
        items.add("手机与充电线");
        items.add("洗漱用品");
        items.add("换洗衣物（" + Math.min(days, 7) + "套参考）");
        items.add("纸巾");
        if ("飞机".equals(transport)) items.add("航班与登机信息");
        else if ("火车".equals(transport)) items.add("车次与座位信息");
        else if ("自驾".equals(transport)) {
            items.add("驾驶证与行驶证");
            items.add("车载充电器");
        } else throw new IllegalArgumentException("交通方式无效");
        if (business) {
            items.add("工作资料"); items.add("正式服装");
        } else {
            items.add("舒适步行鞋"); items.add("遮阳帽");
        }
        if (electronics) {
            items.add("平板或相机"); items.add("设备充电器");
        }
        if (days >= 7) items.add("洗衣袋");
        return items;
    }
}
