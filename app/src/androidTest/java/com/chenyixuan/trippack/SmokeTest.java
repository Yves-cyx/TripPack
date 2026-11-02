package com.chenyixuan.trippack;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.graphics.Bitmap;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;

/** 不依赖第三方测试库的设备端检查。运行结果通过am instrument输出。 */
public class SmokeTest extends Instrumentation {
    private final StringBuilder log = new StringBuilder();
    private String mode;
    @Override public void onCreate(Bundle args) { super.onCreate(args); mode=args==null?null:args.getString("mode"); start(); }
    private void check(boolean value,String message) {
        if(!value) throw new AssertionError(message);
        log.append("PASS ").append(message).append('\n');
    }
    private Intent page(Class<?> c) {
        return new Intent(getTargetContext(),c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    private void pause() { waitForIdleSync(); SystemClock.sleep(700); }
    private void dismissSystemAnr() {
        AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root==null || root.findAccessibilityNodeInfosByText("System UI isn't responding").isEmpty())return;
        for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Close app")) {
            AccessibilityNodeInfo n=node;
            for(int i=0;i<4 && n!=null;i++,n=n.getParent()) {
                if(n.isClickable()) {n.performAction(AccessibilityNodeInfo.ACTION_CLICK);SystemClock.sleep(1500);return;}
            }
        }
    }
    private void shot(String name) throws Exception {
        dismissSystemAnr();pause();Bitmap b=getUiAutomation().takeScreenshot();
        if(b==null)throw new IOException("screenshot unavailable");
        try(FileOutputStream f=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,f);}
    }
    private void dialogButton(String text) {
        dismissSystemAnr();pause();AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root!=null)for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text)) {
            if(text.contentEquals(n.getText()==null?"":n.getText()) && "android.widget.Button".contentEquals(n.getClassName())) {
                if(n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {pause();return;}
            }
        }
        throw new AssertionError("dialog button "+text);
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            if ("restore".equals(mode)) {
                ResultActivity restored=(ResultActivity)startActivitySync(page(ResultActivity.class));pause();
                check(((ListView)restored.findViewById(R.id.items)).getCount()==12,"saved list persists across process restart");
                check(((TextView)restored.findViewById(R.id.summary)).getText().toString().contains("上海 · 7天"),"saved title persists across process restart");
                runOnMainSync(restored::finish);
                result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);return;
            }
            getTargetContext().getSharedPreferences("trip",0).edit().clear().commit();
            MainActivity main=(MainActivity)startActivitySync(page(MainActivity.class));pause();
            EditText dest=main.findViewById(R.id.destination),days=main.findViewById(R.id.days);
            runOnMainSync(()->main.findViewById(R.id.generate).performClick());
            check(dest.getError()!=null,"empty destination blocked");
            runOnMainSync(()->{dest.setText("杭州");days.setText("0");main.findViewById(R.id.generate).performClick();});
            check(days.getError()!=null,"invalid days blocked");
            runOnMainSync(()->{days.setText("3");dest.setError(null);days.setError(null);days.clearFocus();});shot("01-main.png");
            ActivityMonitor monitor=addMonitor(ResultActivity.class.getName(),null,false);
            runOnMainSync(()->main.findViewById(R.id.generate).performClick());
            ResultActivity page=(ResultActivity)waitForMonitorWithTimeout(monitor,30000);removeMonitor(monitor);
            check(page!=null,"Activity navigation");pause();
            ListView list=page.findViewById(R.id.items);
            check(list.getCount()==8,"3-day train leisure generates 8 items");
            check(((TextView)page.findViewById(R.id.summary)).getText().toString().contains("杭州 · 3天"),"Intent values received");
            shot("02-list.png");
            runOnMainSync(()->list.performItemClick(list.getChildAt(0),0,0));
            check(((TextView)page.findViewById(R.id.progress_text)).getText().toString().contains("1 / 8"),"packed progress");
            EditText custom=page.findViewById(R.id.custom_item);
            runOnMainSync(()->{custom.setText("雨伞");page.findViewById(R.id.add).performClick();});
            check(list.getCount()==9,"add item");
            runOnMainSync(()->{custom.setText("雨伞");page.findViewById(R.id.add).performClick();});
            check(list.getCount()==9 && custom.getError()!=null,"duplicate rejected");
            runOnMainSync(()->custom.setText(""));
            runOnMainSync(()->list.getOnItemLongClickListener().onItemLongClick(list,list.getChildAt(0),8,8));dialogButton("删除");
            check(list.getCount()==8,"delete item");
            check(((TextView)page.findViewById(R.id.progress_text)).getText().toString().contains("1 / 8"),"deletion preserves checked state");
            shot("03-progress.png");
            runOnMainSync(()->page.findViewById(R.id.back).performClick());pause();
            ResultActivity restored=(ResultActivity)startActivitySync(page(ResultActivity.class));pause();
            check(((TextView)restored.findViewById(R.id.progress_text)).getText().toString().contains("1 / 8"),"saved state restored in new Activity");
            runOnMainSync(restored::finish);pause();
            runOnMainSync(()->{dest.setText("上海");days.setText("7");((Spinner)main.findViewById(R.id.transport)).setSelection(2);((RadioGroup)main.findViewById(R.id.purpose)).check(R.id.business);((CheckBox)main.findViewById(R.id.electronics)).setChecked(true);});pause();
            ActivityMonitor m2=addMonitor(ResultActivity.class.getName(),null,false);
            runOnMainSync(()->main.findViewById(R.id.generate).performClick());dialogButton("生成");
            ResultActivity second=(ResultActivity)waitForMonitorWithTimeout(m2,30000);removeMonitor(m2);pause();
            check(second!=null && ((ListView)second.findViewById(R.id.items)).getCount()==12,"7-day driving business electronics generates 12 items");
            runOnMainSync(second::finish);pause();runOnMainSync(()->main.findViewById(R.id.reset).performClick());
            check(dest.getText().length()==0 && !((CheckBox)main.findViewById(R.id.electronics)).isChecked(),"ImageButton reset");
            check(getTargetContext().getSharedPreferences("trip",0).contains("plan"),"reset keeps saved list");
            runOnMainSync(main::finish);
            log.append("ALL DEVICE CHECKS PASSED\n");result.putString("stream",log.toString());finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            StringWriter w=new StringWriter();e.printStackTrace(new PrintWriter(w));
            result.putString("stream",log+"\nFAILED\n"+w);finish(Activity.RESULT_CANCELED,result);
        }
    }
}
