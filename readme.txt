----------------------------------------------------------------------
使用方法
java -jar backuper-<version>-jar-with-dependencies.jar from.dir to.dir [differ.dir]

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
