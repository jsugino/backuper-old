----------------------------------------------------------------------
�g�p���@
java -jar backuper-<version>-jar-with-dependencies.jar from.dir to.dir [differ.dir]

----------------------------------------------------------------------
�����t�@�C���̕ۑ����@
(����) ���̋@�\�� 2016/1/1 ���݁A�܂���������Ă��Ȃ�

�R�Ԗڂ̈��� differ.dir ���w�肷��ƁA�ύX��폜���ꂽ�t�@�C���̌����ۑ������B
(�ړ����ꂽ���̂ɂ��ẮA�ۑ�����Ȃ��B)

���Ƃ��΁A���̂悤�ɁA�t�H���_���w�肵���Ƃ���B
	java -jar backuper.jar /orig/path /back/path /differ/path

�����ɁA���̂悤�ȃt�@�C�����ύX���ꂽ�ꍇ�A
	/orig/path/dir1/file2.ext (�X�V���ꂽ)
	/orig/path/dir1/file4.ext (�V�K�ǉ�)
	/orig/path/dir2/file1.ext (dir1����ړ�����)
	/back/path/dir1/file1.ext (dir2�ֈړ�)
	/back/path/dir1/file2.ext (�X�V���ꂽ)
	/back/path/dir1/file3.ext (���Ƃɂ��������A�폜���ꂽ)

���̂悤�ɓ��삷��B
	/differ/path/dir1/file2-20150101120000.ext
	/differ/path/dir1/file3-20150101090000.ext

�t�@�C���̃^�C���X�^���v�́A�u�ȑO�́v����(YYYYMMDDHHMMSS)������ (�o�b�N�A�b�v���������ł͂Ȃ�)

