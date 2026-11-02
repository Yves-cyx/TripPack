import com.chenyixuan.trippack.PackingRules;
import java.util.ArrayList;
public class PackingRulesTest {
    static int checks=0;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);checks++;}
    public static void main(String[] args){
        ArrayList<String> leisure=PackingRules.generate(3,"火车",false,false);
        check(leisure.size()==8,"3-day train leisure has 8 items");
        check(leisure.contains("换洗衣物（3套参考）"),"clothing follows days");
        check(leisure.contains("车次与座位信息"),"train-specific item");
        check(!leisure.contains("工作资料"),"leisure excludes business item");
        ArrayList<String> business=PackingRules.generate(10,"自驾",true,true);
        check(business.contains("换洗衣物（7套参考）"),"clothing capped at 7");
        check(business.contains("驾驶证与行驶证"),"driving-specific item");
        check(business.contains("工作资料"),"business-specific item");
        check(business.contains("平板或相机"),"electronics option");
        check(business.contains("洗衣袋"),"long-trip item");
        check(PackingRules.generate(1,"飞机",false,false).contains("航班与登机信息"),"flight-specific item");
        for(int day:new int[]{0,31,-1}){boolean rejected=false;try{PackingRules.generate(day,"火车",false,false);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"reject days "+day);}
        check(PackingRules.generate(30,"火车",false,false).size()==9,"30-day boundary accepted");
        System.out.println("Passed "+checks+" checks.");
    }
}
