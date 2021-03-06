----------------------------------------------------------------------
使用方法
java -jar backuper-<version>-jar-with-dependencies.jar [options] from.dir to.dir [differ.dir]

----------------------------------------------------------------------
基本的な動作について

次のような判定基準を持っている。
+------------------+------+------+------+
|                  | same | touch| move |
+------------------+------+------+------+
| 相対ディレクトリ |  ○  |  ○  |  ×  |
| ファイル名       |  ○  |  ○  |  ×  |
| ファイル長       |  ○  |  ○  |  ○  |
| 更新時刻         |  ○  |  ×  |  ○  |
+------------------+------+------+------+

デフォルトでは、それぞれ、次の動作を行う。
same  : 特に何もしない
touch : 時刻のみ更新する
move  : 移動する

ただし、オプションにより、動作を変更できる。

----------------------------------------------------------------------
指定可能な option について

-e	実際にバックアップ処理を行う。このオプションを指定しなければ、
	比較などは行うが、コピーや移動などのファイル操作は行われない。

次のオプションで厳密なファイル比較することができる。
-s	same のもの
-t	touch  のもの
-m	move のもの

次のオプションを指定することで、
バックアップ対象からはずすファイル名／ディレクトリ名を指定する。
-R	FileName 排除するファイル名が入ったファイルを指定する
-r	Pattern  排除するファイル名を直接指定する

排除するファイル名には、ワイルドカードとして次のものが指定できる。
**	複数のディレクトリを含むすべての文字列
*	ディレクトリを含まないすべての文字列

----------------------------------------------------------------------
差分ファイルの保存方法

３番目の引数 differ.dir を指定すると、変更や削除されたファイルの元が保存される。
(移動されたファイルについては、保存されない。)

たとえば、次のように、フォルダを指定したとする。
	java -jar backuper.jar /orig/path /back/path /differ/path

そこに、次のようなファイルが変更された場合、
	/orig/path/dir1/file2.ext (更新された)
	/orig/path/dir1/file4.ext (新規追加)
	/orig/path/dir2/file1.ext (dir1から移動した)
	/back/path/dir1/file1.ext (dir2へ移動)
	/back/path/dir1/file2.ext (更新された)
	/back/path/dir1/file3.ext (もとにあったが、削除された)

次のように動作する。
	/differ/path/dir1/file2-20150101120000.ext
	/differ/path/dir1/file3-20150101090000.ext

ファイルのタイムスタンプは、そのファイルの以前の日時(YYYYMMDDHHMMSS)を示す (バックアップした日時ではない)
