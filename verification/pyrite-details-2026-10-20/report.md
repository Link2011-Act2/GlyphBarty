# Details拡充・旧Glyph方式遷移への移行（2026-10-20）

## 対象と配布

作業開始時のGlyph Bartyは`c92be99`（前回のPyrite移行を保存したコミット）でクリーンでした。既存の成果・テスト・検証記録は維持しています。

Pyriteは[`508b1cf5131679fee7581acbc7ea55a365b6ff2c`](https://github.com/Link2011-Act2/Pyrite/commit/508b1cf5131679fee7581acbc7ea55a365b6ff2c)を使用します。masterへ通常Push後、JitPackの匿名AAR/POM/sources取得と、POM-only独立consumer（1分36秒）を確認してからGlyphへ適用しました。AARは207,801 bytes、sourcesは25,094 bytesです。新たな依存・モジュール・CI・タグ・Releaseは追加していません。

## 共通化

| 対象 | 実装 |
| --- | --- |
| DetailsのCapture/Core parameters/Advanced/System | 4つのSettingsGroupへ移行。位置enum/手動gapを除去し、Pyriteが可視行の連結形状と2dp間隔を管理 |
| Slider設定 | SettingsSliderItem 8項目。値、範囲、callback、終了callback、説明・整形文字列は元のまま |
| 固有内容を持つカード | SettingsContentCard 5か所。開始/停止、メーター、visual dynamics補足/リセット、展開行、Import/Export等の内側はslotで元の配置を維持 |
| ボタン付き標準設定行 | PyriteにSettingsButtonItemを追加。カタログ/consumerで使用。Glyph固有の開始/停止ボタンを無理に単一ボタンAPIへ変更していない |
| ParameterSlider | app内の既存呼び出しは薄いadapterからSettingsSlider内容APIへ委譲。保存・同期処理は移さない |
| 設定/Scaffold/About | M3Eも共通行・ヘッダー・モザイクを使用。旧Material専用分岐を削除。Aboutの既知の問題・Support/ログ共有/削除/OSSもcontentカードで接続 |

カード面にクリックはなく、Button/Sliderはそれぞれ独立した公式コンポーネントです。Detailsの展開見出しだけは元のRow余白を維持し、そのRow自身にクリックを置いています。内部に別のButton/Sliderは置かず、親面との二重発火を避けています。

`ExperimentalDetailsScreenContent`の主処理（状態・各callback・reset確認等）は移行前とテキスト一致を確認しました。MainActivity、ViewModel、MainCoordinator、設定キー・保存、Glyph/音声/Spectrum/更新処理は今回変更していません。Details固有のタブ/メーター/デバイス制御UIの内容もアプリ側です。

## テーマとフォント

既存のstyle保存booleanをそのまま使い、falseをPyriteStyle.Material3Expressiveへ割り当てます。表示名だけをen/jaともMaterial 3 Expressiveへ変更しました。

M3EはPyriteのcolorScheme/typography/shapes/motionSchemeを継承し、内側のMaterialThemeで別配色へ戻さない構成です。Nothingは以前のNothingTypographyとShapesを維持します。NTypeは画面タイトル・アプリ名・Channel・既存device名等、NDotはAbout Versionに限定し、通常の設定・Slider名・説明はSansSerifのままです。

実フォントのTextLayoutResult、Nothing/M3E×明暗×Dynamic ON/OFFのカラースキーム一致をテストしています。フォントファイルやSDKをPyriteへ移していません。

## 標準遷移

PyriteScreenTransition自体を旧Glyph `53e568c`のscreen_transitionへ変更しました。右/左から幅1/5で入り、slide/fadeは公式既定spring、SizeTransformも既定のclip=trueです。固定300ms tween・40ms遅延等を除去し、Glyph側に独自遷移は追加していません。

SaveableStateHolder、contentKey、実際のinitial/targetに基づく方向判定は維持しました。Pyriteの参照比較で往復22組＋可変サイズ24組の46フレームが画素一致。Compose 1.7.2/1.12.0の既定値も公式sourcesで一致を確認しています。[Pyrite側の記録](https://github.com/Link2011-Act2/Pyrite/blob/508b1cf5131679fee7581acbc7ea55a365b6ff2c/docs/details-cards-and-motion.md)。旧APKとの同時実機撮影ではありません。

## 表示比較

API35 / Pixel_6a / 720×1280 / density320 / fontScale1。同じオフラインfixtureで、Live/Tune/Systemを撮影しました。比較領域はx=0..720、y=60..1196です。

Nothingの6組はすべて差分0です。[light Live](nothing-light-live-comparison.png)・[Tune](nothing-light-tune-comparison.png)・[System](nothing-light-system-comparison.png)、[dark Live](nothing-dark-live-comparison.png)・[Tune](nothing-dark-tune-comparison.png)・[System](nothing-dark-system-comparison.png)。

この6組は比較領域だけでなく、取得したCompose root全体の画素も一致しました。通常Switch行はPyriteの単一toggleable行となり、従来Switch単体だった操作領域が行全体へ広がります。状態・保存callbackは同じで、二重発火は検証しています。

Material→M3Eは意図したテーマ/行の変更を含みます。[light Live](material-light-live-comparison.png)・[Tune](material-light-tune-comparison.png)・[System](material-light-system-comparison.png)、[dark Live](material-dark-live-comparison.png)・[Tune](material-dark-tune-comparison.png)・[System](material-dark-system-comparison.png)。特にライトは背景surfaceContainerLow、カードsurfaceContainerLowestへ揃い、SwitchはM3E標準のアイコン付きになります。数値は[details-image-metrics.json](details-image-metrics.json)に保存しています。

## 検証の経緯

移行前Detailsの4 UIケースが成功。移行後は既存JVM 220件成功、APK/テストAPKとLint成功（エラー0、警告200、Hint 8）です。全UI14ケース中12件が初回成功し、追加した実Activityケース2件はSensitivityの保存直後の読み取りが早すぎました。

既存MainCoordinator.updateDeferredはUI反映後に保存・service同期を遅延します。保存方式は変更せず、テストだけを保存完了の条件待ちへ修正しています。固定sleepやスクロールpx補正は追加していません。最終結果・計測値は後述の集計ファイルに記録します。

確認範囲はオフラインfixture、実Activityの設定・ナビゲーション、既存自動テストです。実機Glyph/音声入力、実TalkBack、全端末・全文字倍率の確認は未実施です。

## 最終結果

| 検証 | 結果 |
| --- | --- |
| Pyrite | 公開AAR・カタログ・JVM・UI35ケース・Lint・Maven/JitPack独立consumerを確認 |
| Glyph APK | 最終ソースでcompileDebugKotlin/assembleDebug成功。末尾空行の整理後もビルド確認 |
| Glyph JVM | 220件、失敗0・エラー0 |
| Glyph UI | 全14ケースを確認。初回成功12件＋保存完了待ちを修正した実Activity2件が成功 |
| 実Activity | Nothing/M3EのAbout再生成2回、設定との往復、Detailsの保存・スクロール・展開・タブ復元を確認 |
| Glyph Lint | エラー0、警告200、Hint 8。baseline/抑制は追加せず |
| Glyph単独ビルド | ソース190ファイルの独立コピーから1分53秒で成功。Pyriteソース/project依存/隣接参照なし、固定SHAのJitPackを使用、--no-build-cache。Gradle全体の依存キャッシュは共用 |
| ShutUp更新シミュレーション | 独立コピーのみ新SHAへ向け、APK/テストAPK/Lint成功。配色4条件・通常/中断遷移・復元・Aboutの計9 UIケース成功。Lintエラー0、警告48 |

[集計・APKハッシュ](checks.json)、[配布ハッシュ](jitpack-artifacts.json)、[初回UI](ui-first-run.txt)、[実Activity最終UI](ui-restoration-final.txt)、[移行前UI](ui-before.txt)、[ShutUp UI](shutup-ui.txt)を保存しています。計測は[Nothing](geometry-nothing.txt)と[M3E](geometry-m3e.txt)です。Detailsの復元offsetはそれぞれ2292/2310px、viewportはともに(0,320)-(720,1280)で、画面往復と再生成で一致しました。保存値Sensitivity=2.0も維持しています。

ShutUpの実リポジトリは依存`43557c8`のままで、開始時の未コミット変更も維持しています。将来の更新では標準遷移がspringへ変わり、依存SHAとOSS表示の座標更新も必要になります。今回のシミュレーションでは製品コードや既存テストの修正は不要でした。

## 残した部分と作業状態

Details固有の中央Top App Bar・タブ・メーター・状態パネル・開始/停止の色やprogress表示・parameter file操作の内容はアプリ側に残しています。既存の他画面のパラメーター呼び出しはapp内adapter経由で共有Sliderへ委譲します。UI保存以外の制御・ViewModel・更新処理の移植は行っていません。

Pyriteだけをコミット・通常Push済みです。Glyphは`c92be99`からの未コミット・未Pushで保持しています。タグ・Release、履歴改変、Force Pushは行っていません。Pyriteの既存変更`docs/shutup-migration-plan.md`も保持し、コミットから除外しました。READMEは変更していません。

確認用APKは`app/build/outputs/apk/debug/app-universal-debug.apk`です。詳細ログ・元画像・独立コピーは`build/details-*`、再開用の変更内容と比較証跡は本ディレクトリにあります。
