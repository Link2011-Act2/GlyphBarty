<!--
### ⚠️このプロジェクトはまだ調整中です。早期に試したい方のためにIntDevバージョンのソースコードを公開していますが、バグが残っている可能性があります。<br>5月12日に予定されている正式リリースを待つことをおすすめします。
-->

# Glyph Barty

公式機能よりもかっこよく、多くの機種に対応するGlyphビジュアライザー



## 概要

Nothingのスマホは、Phone (3a)までは公式で音楽に合わせてGlyphライトが点滅する機能がありました

ただ、これはそれ以降の機種では削除されたうえ、公式の機能もランダムっぽい光り方で、あんまかっこよくなかったんです

なので、Phone (3)などの非対応機種でも使えるうえ、公式よりもかっこいいBetterなGlyphビジュアライザーを作りました



### 機能・特徴

* 現在再生中の音楽に合わせてGlyph Interfaceをリアルタイムに光らせる
* クイック設定タイルでON/OFF可能<br>ほぼ公式機能と同じように使えます！！！
* その他おまけ機能 (例: 充電状況をGlyphに表示)
* 画面録画の許可は不要

他のアプリでは音声を取得するためにいちいち画面録画の許可を求めてくるものもありますが、このアプリはそのような許可は不要です



## 対応機種

* Nothing Phone (2)
* Nothing Phone (2a)
* Nothing Phone (2a) Plus
* Nothing Phone (3a)
* Nothing Phone (3a) Pro
* Nothing Phone (3)
* Nothing Phone (4b)
* Nothing Phone (4a)
* Nothing Phone (4a) Pro

### 対応機種(条件付き)
* Nothing Phone (1)
  * Glyph Interfaceのデバッグモードをオンにする必要があります。adbコマンド ``` adb shell settings put global nt_glyph_interface_debug_enable 1 ``` を実行するか、Shizukuが使える場合はアプリ内から自動でオンにできます。
<br>

ほぼすべてのNothing Phoneに対応しています

Phone (3a) LiteはSDKが公開されていないため非対応です



## 使い方

1. releasesから最新のAPKをダウンロードし、インストールする
2. アプリから録音の権限を要求されるので、許可する
3. スタートボタンを押し、好きな音楽を再生する
4. 好きなパターンを選ぶ
5. パラメータを調整して、自分の好みの動き方にする



## 問題があったら

Issues、またはプルリクエストをしてくださいい

ただ、自分は学生をしながら趣味で開発しているため、すぐに対応できない場合があります



## ライセンス

MIT Licenseです

好きにいじくってもらって構いません



## スクリーンショット

日本語も対応しています
### UIモード: Nothingライク
<img width="200" alt="Screenshot_20260827-004135" src="https://github.com/user-attachments/assets/1d218638-f23f-4260-981e-8373c8572099" />
<img width="200" alt="Screenshot_20260827-004137" src="https://github.com/user-attachments/assets/647e8ce5-7b30-4866-ab2b-e7c908e590e8" />
<img width="200" alt="Screenshot_20260827-004141" src="https://github.com/user-attachments/assets/8f0cbad0-42d2-4c0b-9c39-63592ec9dc98" />



### UIモード: Material 3
<img width="200" alt="Screenshot_20260827-010432" src="https://github.com/user-attachments/assets/9922b02b-6b4d-4ed1-a2a5-d9e877ae4d7f" />
<img width="200" alt="Screenshot_20260827-010434" src="https://github.com/user-attachments/assets/9e9f03e3-c8f0-452e-b3cc-2c8e18d7e90d" />
<img width="200" alt="Screenshot_20260827-010439" src="https://github.com/user-attachments/assets/e2b23462-dbb4-4cb8-acbe-9ec2b77fdd81" />

