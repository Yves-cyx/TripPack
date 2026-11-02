package com.chenyixuan.trippack;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;

public class MainActivity extends Activity {
    private EditText destination, days;
    private Spinner transport;
    private RadioGroup purpose;
    private CheckBox electronics;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        destination = findViewById(R.id.destination);
        days = findViewById(R.id.days);
        transport = findViewById(R.id.transport);
        purpose = findViewById(R.id.purpose);
        electronics = findViewById(R.id.electronics);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"火车", "飞机", "自驾"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transport.setAdapter(adapter);
        findViewById(R.id.generate).setOnClickListener(v -> generate());
        findViewById(R.id.resume).setOnClickListener(v -> {
            if (!getSharedPreferences("trip", MODE_PRIVATE).contains("plan")) {
                Toast.makeText(this, "还没有保存的清单，请先生成", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, ResultActivity.class));
        });
        findViewById(R.id.reset).setOnClickListener(v -> {
            destination.setText(""); days.setText("");
            destination.setError(null); days.setError(null);
            transport.setSelection(0); purpose.check(R.id.leisure); electronics.setChecked(false);
            Toast.makeText(this, "表单已重置，已保存清单不受影响", Toast.LENGTH_SHORT).show();
        });
    }

    private void generate() {
        String place = destination.getText().toString().trim();
        if (place.isEmpty()) {
            destination.setError("请输入目的地"); destination.requestFocus(); return;
        }
        int count;
        try {
            count = Integer.parseInt(days.getText().toString().trim());
            if (count < 1 || count > 30) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            days.setError("请输入1至30的整数"); days.requestFocus(); return;
        }
        // 显式Intent：指定目标Activity并传递本次行程参数。
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("new_plan", true);
        intent.putExtra("destination", place);
        intent.putExtra("days", count);
        intent.putExtra("transport", transport.getSelectedItem().toString());
        intent.putExtra("business", purpose.getCheckedRadioButtonId() == R.id.business);
        intent.putExtra("electronics", electronics.isChecked());
        if (getSharedPreferences("trip", MODE_PRIVATE).contains("plan")) {
            new AlertDialog.Builder(this).setTitle("生成新清单")
                .setMessage("新清单会替换上次保存的清单，是否继续？")
                .setNegativeButton("取消", null)
                .setPositiveButton("生成", (dialog, which) -> startActivity(intent)).show();
        } else startActivity(intent);
    }
}
