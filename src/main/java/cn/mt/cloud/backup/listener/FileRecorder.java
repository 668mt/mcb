package cn.mt.cloud.backup.listener;

import java.io.File;

/**
 * @Author Martin
 * @Date 2021/1/26
 */
public interface FileRecorder {
	
	/**
	 * 是否已经上传
	 *
	 * @param file 文件
	 * @return 是否已经上传
	 */
	boolean isUploaded(File file);
	
	/**
	 * 标记已经上传
	 *
	 * @param file 文件
	 */
	void markUploaded(File file);
	
	/**
	 * 标记删除
	 *
	 * @param file 文件
	 */
	void markDeleted(File file);
	
	void flush();
}
