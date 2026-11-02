package com.chenyixuan.trippack;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import org.json.*;
import java.util.ArrayList;

public class ResultActivity extends Activity {
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<Boolean> packed = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView list;
    private String title;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_result);
        try {
            Intent data = getIntent();
            if (state == null && data.getBooleanExtra("new_plan", false)) {
                int days = data.getIntExtra("days", 0);
                String transport = data.getStringExtra("transport");
                boolean business = data.getBooleanExtra("business", false);
                String place = data.getStringExtra("destination");
                if (place == null || place.trim().isEmpty()) throw new IllegalArgumentException();
                title = place + " · " + days + "天\n" + transport + " / " + (business ? "商务出行" : "休闲旅行");
                names.addAll(PackingRules.generate(days, transport, business,
                        data.getBooleanExtra("electronics", false)));
                for (String ignored : names) packed.add(false);
            } else load();
        } catch (Exception e) {
            Toast.makeText(this, "清单无法读取，请返回重新生成", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        ((TextView)findViewById(R.id.summary)).setText(title);
        list = findViewById(R.id.items);
        adapter = new ArrayAdapter<>(this, R.layout.row_plan, names);
        list.setAdapter(adapter);
        refresh();
        list.setOnItemClickListener((parent, view, position, id) -> {
            packed.set(position, list.isItemChecked(position));
            updateProgress(); save();
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this).setTitle("删除物品")
                .setMessage("删除“" + names.get(position) + "”？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    names.remove(position); packed.remove(position); refresh();
                }).show();
            return true;
        });
        EditText custom = findViewById(R.id.custom_item);
        findViewById(R.id.add).setOnClickListener(v -> {
            String text = custom.getText().toString().trim();
            if (text.isEmpty()) { custom.setError("请输入物品名称"); return; }
            if (names.contains(text)) { custom.setError("清单中已有这件物品"); return; }
            if (names.size() >= 100) {
                Toast.makeText(this, "最多添加100件物品", Toast.LENGTH_SHORT).show(); return;
            }
            names.add(text); packed.add(false); custom.setText("");
            refresh(); list.smoothScrollToPosition(names.size() - 1);
        });
        findViewById(R.id.back).setOnClickListener(v -> { save(); finish(); });
    }

    // 删除会改变位置，需要清除ListView旧状态，再按数据重新勾选。
    private void refresh() {
        adapter.notifyDataSetChanged();
        list.clearChoices();
        for (int i = 0; i < packed.size(); i++) list.setItemChecked(i, packed.get(i));
        updateProgress(); save();
    }
    private void updateProgress() {
        int count = 0;
        for (boolean value : packed) if (value) count++;
        ((TextView)findViewById(R.id.progress_text)).setText(names.isEmpty()
            ? "清单为空，可以添加物品"
            : "已装包 " + count + " / " + names.size() + " 件" + (count == names.size() ? " · 准备完成" : ""));
        ((ProgressBar)findViewById(R.id.progress)).setProgress(names.isEmpty() ? 0 : count * 100 / names.size());
    }
    private void save() {
        try {
            JSONObject plan = new JSONObject();
            plan.put("title", title);
            JSONArray items = new JSONArray();
            for (int i = 0; i < names.size(); i++) {
                JSONObject item = new JSONObject();
                item.put("name", names.get(i)); item.put("packed", packed.get(i)); items.put(item);
            }
            plan.put("items", items);
            getSharedPreferences("trip", MODE_PRIVATE).edit().putString("plan", plan.toString()).apply();
        } catch (JSONException e) {
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_LONG).show();
        }
    }
    private void load() throws JSONException {
        JSONObject plan = new JSONObject(getSharedPreferences("trip", MODE_PRIVATE).getString("plan", ""));
        title = plan.getString("title");
        JSONArray items = plan.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            names.add(item.getString("name")); packed.add(item.getBoolean("packed"));
        }
    }
}
