# Glyph Barty — Pyrite導入記録（2026-10-19）

## 配布と依存

- 移行元: Glyph Barty `53e568c`。既存の未コミット実装・テストを引き継ぎました。
- Pyrite: [`eeedbfe5005bf1ed781d0cf1ef2cbe2a0923e7dd`](https://github.com/Link2011-Act2/Pyrite/commit/eeedbfe5005bf1ed781d0cf1ef2cbe2a0923e7dd)。フォントAPIを追加し、masterへ通常Push済みです。
- `com.github.Link2011-Act2:Pyrite:eeedbfe5005bf1ed781d0cf1ef2cbe2a0923e7dd`をJitPackから固定SHAで取得します。project依存、includeBuild、隣接Pyriteディレクトリ参照はありません。
- 公開AAR 191,632 bytes、sources 23,446 bytes、POM 2,395 bytesを匿名取得しました。POMだけを使う独立consumerもビルド成功。Compose UI/Foundation/Animation 1.12.0、Material 3 1.5.0-beta01、Kotlin stdlib 2.2.10の推移依存を確認しています。
- GlyphのcompileSdkを36.1から37へ、Compose BOMによるUI 1.7.2 / Material 3 1.3.0を上記明示版へ揃えました。icons-extendedはPyriteに合わせ1.7.8です。minSdk 33、targetSdk 36、AGP 9.1.1、Gradle 9.3.1、Compose Compiler 2.2.10、アプリのversionCode/nameは維持しています。

## 共通化した範囲と残した範囲

| 対象 | 方針 |
| --- | --- |
| 設定画面 | `SettingsGroup`、Nothingの`SettingsNavigationItem` / `SettingsSwitchItem`。既存の14dp見出し間隔、表示条件、callback、ダイアログはアプリ側で維持 |
| About | Nothingの`AboutOverviewMosaic`、両表示の`AboutUpdateCard`。表示情報と更新処理はアプリが供給 |
| ヘッダー | Nothingは`PyriteScreenScaffold`。Materialは既存タイトルの書式を残すアプリ内Scaffold。戻るボタンは共通`PyriteBackButton` |
| 遷移 | ルート1か所を`PyriteScreenTransition`へ移行。既存Screen/ordinalによる方向判定と遷移先判断はアプリ側 |
| Material表示 | 独自のDynamic配色・Typography・設定行・Aboutモザイクを維持。PyriteのNothing配色で上書きしない |
| Aboutの固有カード | 既知の問題、Support→ログ共有/削除→OSSの連結カードは維持。`AboutActionGroup`は中間の2ボタンカードを表現できず、グループ分断を避けるため置換しない |
| その他の設定・機能画面 | 既存の共通ヘルパーを使う詳細画面、Visualizer、Spectrum Marker、Glyph、音声、ViewModel、設定保存、更新詳細・GitHub Releases処理は維持 |

NothingのSwitchはPyriteの単一toggleable行に統一され、Switchだけでなく行全体を押して切り替えられます。旧UIではSwitchのみが操作対象でした。二重callbackを検証対象にしています。

画面遷移は旧Glyphのspringから、共通の300ms・幅1/5スライド、220ms（40ms遅延）入場fade、140ms退場fadeへ変わります。二重のAnimatedContentは追加していません。

## フォント

利用アプリの既存フォントを渡し、フォントアセットはPyriteへ複製していません。

```kotlin
PyriteTheme(
    style = PyriteStyle.Nothing,
    darkTheme = darkTheme,
    fonts = PyriteFonts(
        title = NTypeFontFamily,
        accent = NothingDotFontFamily
    )
) { /* アプリの既存Typography/Shapesと画面 */ }
```

| 箇所 | フォント |
| --- | --- |
| Nothingの設定/About画面タイトル、Aboutアプリ名、Channel強調値 | NType |
| AboutのVersion強調値 | NDot |
| 設定項目・説明・見出し、Channel/Build/Versionラベル、リリース文字列、ダイアログ | 既存SansSerif |
| Material表示 | 従来のSansSerifと既存Typography |

Pyrite本体では未指定・空の`PyriteFonts`の同一条件画像比較、旧4引数呼び出し、役割ごとの部分指定、任意Typeface由来のFontFamilyを両スタイルで検証しています。既存29 UIテストが通過し、新規テストで見つかったbody-only指定のChannelへの継承を修正後、新規2件と配色1件の計3件が成功しました。AAR・カタログAPK・JVM・Lint・ローカルMaven・独立consumerも成功しています。詳細はPyriteの`docs/font-verification.md`を参照してください。

## 画面再生成の修正

移行前の実MainActivityで、再生成するとcontentが720×1280@[0,0]から720×1176@[0,56]になり、Aboutのscrollが1482→1422へ60px減少しました。systemBarsは前後ともtop56/bottom48で、viewportの原点が(40,220)→(40,164)へ変化していました。

起動時のdecorFitsSystemWindowsを明示してedge-to-edge初期化を揃え、onPostCreateで現在のWindowInsetsをcontentへ再配信します。固定px加算、遅延、scroll値の補正は行いません。設定保存・Glyph処理とは独立したWindow初期化の変更です。

## 検証条件

API 35 / Pixel_6a読み取り専用エミュレーター / 720×1280 / density320 / fontScale1。実機には操作していません。移行前後で同じオフラインfixtureを使い、Nothing/Material × light/dark、設定上部・更新設定・About上部・アクション・更新詳細を撮影します。更新通信や音声キャプチャは開始しません。

再現用コマンド（JDK 21、Android SDKを設定）:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --max-workers=2
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-universal-debug.apk
adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w -r -e migrationPhase after -e package jp.linkserver.glyphvisualizer jp.linkserver.glyphvisualizer.test/androidx.test.runner.AndroidJUnitRunner
```

PyriteのMIT/NOTICEをOSS画面へ追加しています。音声/Glyph処理ファイルの変更は、移行前から存在したLintのSuspiciousIndentationを修正する4行の空白だけです。

## 最終検証結果

| 検証 | 結果 |
| --- | --- |
| compileDebugKotlin / Debug APK / テストAPK | 成功。universal / arm64-v8a / x86_64 APKを生成 |
| 既存JVMテスト | 220件、失敗0・エラー0。音声、Spectrum、Glyph、設定、Serviceなど既存のテストを維持 |
| 最終UIテスト | 9件成功、37.368秒。既存Example 1件、比較/操作5件、実フォント/配色2件、実Activity 1件 |
| Lint | エラー0、警告201、Hint 8。抑制・baselineの追加なし |
| JitPack | 固定SHAのAAR / POM / sources取得、POM-only独立consumer成功。ハッシュは[jitpack-artifacts.json](jitpack-artifacts.json) |
| Glyph単独ビルド | ソース189ファイルだけの独立コピーで`assembleDebug`成功、2分11秒。`--no-build-cache`、Pyriteソース・project/includeBuild依存・隣接ディレクトリ参照なし。Gradleの全体依存キャッシュは共用。[結果](standalone-result.txt) |
| 保存・再生成 | 実Settingsで詳細Homeを選択してPreferencesへ保存し、2回のActivity再生成後も維持。設定↔About往復後のスクロールも維持 |
| 音声/Glyph実機 | エミュレーターでは実機SDK・音声キャプチャの動作は未検証。実機を操作していません |

初回の移行ビルドでAPKと220件のJVMテストは成功しましたが、Compose更新に伴う`LocalContextGetResourceValueCall`がAboutで5件検出されました。更新通信・結果処理の構造を変えず、文字列取得を`LocalResources.current`経由に限定して修正し、最終APK/UI/Lintを再検証しました。初回ログは`build/pyrite-migration/after-build.txt`、最終ビルドは`build/pyrite-migration/final-build.txt`です。

最終の[UIログ](ui-results.txt)、[集計](checks.json)、[再生成前の計測](geometry-before.txt)、[修正後の計測](geometry-after.txt)を保存しています。修正後はscroll 1483 / max 1527、viewport (40,220)-(680,1196)、content 720×1280@[0,0]、bars top56/bottom48が、再生成2回と設定画面往復の全条件で一致しました。移行前後でAbout本文量に1pxの差があるため、移行前の1482と修正後の1483を直接の復元比較には使っていません。

## 画像比較と残差

次の画像は左が移行前、右が最終APKです。各条件で20枚の元画像を比較し、撮影位置を変更する製品修正は行っていません。

| 表示 | 設定 | About |
| --- | --- | --- |
| Nothing light | [比較](nothing-light-settings-top-comparison.png) | [比較](nothing-light-about-top-comparison.png) |
| Nothing dark | [比較](nothing-dark-settings-top-comparison.png) | [比較](nothing-dark-about-top-comparison.png) |
| Material light | [比較](material-light-settings-top-comparison.png) | [比較](material-light-about-top-comparison.png) |
| Material dark | [比較](material-dark-settings-top-comparison.png) | [比較](material-dark-about-top-comparison.png) |

設定上部は4条件とも比較領域の画素差分0です。Nothingはlight背景`#EDEDED`・カード`#FFFFFF`、dark背景`#000000`・カード`#121212`を維持。Materialもlight背景`#FAF8FF`・カード`#E8E7EF`、dark背景`#121318`・カード`#292A2F`が一致しています。Material値は今回のAVDのDynamic Color条件での値です。

- Nothing Aboutのタイトルは、共通SettingsヘッダーのfontPadding/bottom paddingに揃うため上方2px（100–138→98–136）へ変化します。元の27spアプリ名、25sp Channel/VersionとNType/NDot、モザイク構造、配色を維持しています。下部のIntrinsicSize/heightInによる丸めと本文量で1px差があります。
- Build番号はソース内容と更新日時から自動生成されるため変わります。Nothing About上部の未整列差分は約1.92–1.93%、Materialは0.094%で、Build値や上記描画位置も含みます。
- Nothingの更新設定は、`performScrollTo`の対象が旧タイトル単体から新toggleable行へ変わり、撮影スクロール位置が165px変わりました。未整列では約31%差分ですが、同じカード領域を揃えると0.005%です。[light共通領域](nothing-light-settings-updates-aligned-comparison.png)、[dark共通領域](nothing-dark-settings-updates-aligned-comparison.png)。操作領域の統合に伴う意味のある差として記録し、撮影同期を深追いしていません。
- Aboutの無効なログボタンに明度差があります。設定項目・有効な操作の色を固定値で補正せず、更新後の公式Material 3ボタンを使用しています。[Nothing lightアクション](nothing-light-about-actions-comparison.png)、[dark](nothing-dark-about-actions-comparison.png)、[Material light](material-light-about-actions-comparison.png)、[dark](material-dark-about-actions-comparison.png)。
- 更新詳細画面のソース・GitHub Releases処理は維持しています。画像の未整列差分は約0.09–0.10%で、依存更新による描画差を記録しています。

全条件の数値は[image-metrics.json](image-metrics.json)です。画像比較はこのAVD・通常文字サイズの範囲であり、全実機・全フォントスケールの完全一致を意味しません。

## 作業状態

Glyph Bartyは未コミット・未Pushです。Pyriteのフォント変更だけをコミット・通常Pushし、タグ・Releaseは作成していません。Pyrite側で以前から変更されている`docs/shutup-migration-plan.md`はそのまま残し、今回のコミットから除外しています。ShutUp・Schedulerの変更、README編集、GitHub Actionsなどの追加はありません。

通常ビルドと独立コピーのAPKはコンテナーとしてのハッシュ・サイズが異なりますが、展開後の全163エントリーはSHA-256で一致しました。検証専用エミュレーターとGradle daemonは終了しています。

確認用APK: `app/build/outputs/apk/debug/app-universal-debug.apk`。独立コピーのAPKは`build/pyrite-migration/standalone-eeedbfe/app/build/outputs/apk/debug/app-universal-debug.apk`、全ログ・元画像は`build/pyrite-migration/`、持続的な比較証跡は本ディレクトリです。APKのハッシュも[checks.json](checks.json)に保存しています。追加した3つのUIテストとOSS帰属ファイルも未コミットで保持しています。
