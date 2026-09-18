package com.ckun.reminder;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;

public final class SettingsActivity extends Activity {
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); render(); }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        scroll.setTag("appearance-settings"); scroll.setBackground(Appearance.background(this, 0xffFFF8ED));
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(16), dp(24), dp(32)); scroll.addView(page); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, i) -> { v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(), i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom()); return i; });
        Button back = new Button(this); back.setText("‹  返回"); back.setTag("settings-back");
        back.setTextColor(0xff4A3E35); back.setBackground(Appearance.shape(this, 0xffF5EBDD, 22));
        back.setOnClickListener(v -> finish()); page.addView(back, new LinearLayout.LayoutParams(dp(96), dp(48)));
        TextView title = label("设置", 30); title.setTypeface(null, Typeface.BOLD); page.addView(title);
        page.addView(label("外观模式", 19));
        page.addView(label("选择喜欢的风格，所有页面随心切换。", 14));
        RadioGroup modes = new RadioGroup(this); modes.setTag("appearance-modes");
        modes.setPadding(dp(16), dp(12), dp(16), dp(12));
        modes.setBackground(Appearance.shape(this, 0xffFFFCF7, 24));
        RadioButton simple = option("简约模式", "appearance-simple");
        RadioButton glass = option("毛玻璃模式", "appearance-glass");
        modes.addView(simple); modes.addView(glass);
        modes.check(Appearance.glass(this) ? glass.getId() : simple.getId());
        modes.setOnCheckedChangeListener((group, checked) -> {
            boolean enabled = checked == glass.getId();
            if (enabled != Appearance.glass(this)) { Appearance.setGlass(this, enabled); render(); }
        });
        page.addView(modes);
        TextView hint = label(Appearance.glass(this) ? "柔和光晕 · 半透明卡片 · 轻盈层次" : "温暖底色 · 纯色卡片 · 清晰简洁", 14);
        hint.setTag("appearance-description"); page.addView(hint);
        page.addView(label("选择自动保存，下次打开仍会沿用。", 13));
    }
    private TextView label(String value, int size) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(0xff4A3E35);
        text.setPadding(0, dp(16), 0, dp(16)); return text;
    }
    private RadioButton option(String value, String tag) {
        RadioButton button = new RadioButton(this); button.setId(View.generateViewId()); button.setTag(tag);
        button.setText(value); button.setTextSize(17); button.setTextColor(0xff4A3E35); button.setMinHeight(dp(64));
        button.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff657F73)); return button;
    }
}
