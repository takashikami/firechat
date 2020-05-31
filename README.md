# Various chat programs

## 概要

以下の４種類のチャットサーバーのサンプルプログラム

| プログラム名 | 説明 |
|-------------|------|
| NettyChatServer | Netty＋シングル構成 |
| FireChatServer | Netty＋Geodeでマルチ構成 |
| NioChatServer | Nio＋シングル構成 |
| ChatServer | Nio＋Geodeでマルチ構成 |

シングル構成は、チャットサーバーを１つだけ起動し、
みんながそこにアクセスしてくる（スター型）。
マルチ構成は、通常は複数台のサーバでチャットサーバー
を起動し、サーバー間で通信しているため、どのチャット
サーバにアクセスしても、全員とチャットできる。
（スター型でスター間通信による全員通信）

## 起動方法

build.gradleに記述してあるtask type:JavaExecを実行する
gradleは依存するライブラリをダウンロードしてくるので、
インターネット接続が必要。
社内の場合はproxy設定が必要。

```$shell
$ ./gradlew タスク名
```

| タスク名 | 説明 | ポート|
|---------|------|-------|
| nettyserver | NettyServer | 7000 |
| firechatserver | FireChatServer１つ目 | 7000 |
| firechatserver1 | FireChatServer２つ目 | 7001 |
| firechatserver2 | FireChatServer３つ目 | 7002 |
| nioserver | NioServer | 7000 |
| chatserver | ChatServer１つ目 | 7000 |
| chatserver1 | ChatServer２つ目 | 7001 |
| chatserver2 | ChatServer３つ目 | 7002 |

１台のサーバで複数サーバ起動できるように、
待ち受けポートは7000：１つ目、7001：２つ目、7002：３つ目
としている。

## 動作確認方法

例えば、FireChatServerを試す場合、ターミナルを３つ用意し、
それぞれで、FireChatServerを起動する。

```
./gradlew firechatserver　←ターミナル１
./gradlew firechatserver1　←ターミナル２
./gradlew firechatserver2　←ターミナル３
```

そして、もう３つのターミナルを用意し、
起動したFireChatServerの１つ目〜３つ目にncコマンドで接続し、
「hello」と入力すると、ターミナル４〜６に「hello」と出力される。

```shell script
nc localhost 7000　←ターミナル４
nc localhost 7001　←ターミナル５
nc localhost 7002　←ターミナル６
```

