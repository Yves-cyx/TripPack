# 轻装出行
陈翊轩 · 2406100303

原生Java和XML实现，应用离线运行。用Android Studio打开本目录，使用JDK 17和SDK 35同步。Windows执行`gradlew.bat assembleDebug`，其他系统执行`./gradlew assembleDebug`。

源码：MainActivity负责表单、校验、Intent传值；ResultActivity负责清单、进度、JSON保存恢复；PackingRules负责独立生成规则。两个activity布局均固定展示身份。

独立规则检查：
```sh
javac -encoding UTF-8 -d test-out app/src/main/java/com/chenyixuan/trippack/PackingRules.java PackingRulesTest.java
java -cp test-out PackingRulesTest
```

设备端检查位于app/src/androidTest，使用原生Instrumentation，不依赖第三方测试库。通过`gradlew.bat assembleDebugAndroidTest`构建测试APK，再在已连接的模拟器安装应用APK及测试APK，运行：
```sh
adb shell am instrument -w com.chenyixuan.trippack.test/com.chenyixuan.trippack.SmokeTest
```
测试会重置测试设备上本APP的最近清单，请在专门的模拟器中执行。普通APP APK不包含测试类。只保存最近一份行程，生成新行程前会询问是否替换。
