package cn.mt.cloud.backup.listener;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.utils.executor.MtExecutor;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

import static cn.mt.cloud.backup.utils.FileUtils.getPathname;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@Slf4j
public class BackupFileListener implements FileAlterationListener {
	private final MosSdk mosSdk;
	private final String srcPath;
	private final String desPath;
	private final MtExecutor<Runnable> executor;
	private final FileRecorder fileRecorder;
	
	public BackupFileListener(FileRecorder fileRecorder, MtExecutor<Runnable> executor, MosSdk mosSdk, String srcPath, String desPath) {
		this.fileRecorder = fileRecorder;
		this.executor = executor;
		this.mosSdk = mosSdk;
		this.srcPath = srcPath;
		this.desPath = desPath;
	}
	
	@Override
	public void onStart(FileAlterationObserver observer) {
	
	}
	
	@Override
	public void onDirectoryCreate(File directory) {
	
	}
	
	@Override
	public void onDirectoryChange(File directory) {
	
	}
	
	@Override
	public void onDirectoryDelete(File directory) {
		executor.submit(new DeleteDirTask(directory));
	}
	
	@Override
	public void onFileCreate(File file) {
		executor.submit(new UploadFileTask(file));
	}
	
	@Override
	public void onFileChange(File file) {
		executor.submit(new UploadFileTask(file));
	}
	
	@Override
	public void onFileDelete(File file) {
		executor.submit(new DeleteFileTask(file));
	}
	
	@Override
	public void onStop(FileAlterationObserver observer) {
	
	}
	
	public class UploadFileTask implements Runnable {
		private final File file;
		
		public UploadFileTask(File file) {
			this.file = file;
		}
		
		@SneakyThrows
		@Override
		public void run() {
			if (!fileRecorder.isUploaded(file)) {
				String pathname = getPathname(file, srcPath, desPath);
				pathname = mosSdk.getSafelyPathname(pathname);
				mosSdk.uploadFile(this.file, new UploadInfo(pathname, true));
				fileRecorder.markUploaded(file);
			}
		}
	}
	
	public class DeleteFileTask implements Runnable {
		private final File file;
		
		public DeleteFileTask(File file) {
			this.file = file;
		}
		
		@SneakyThrows
		@Override
		public void run() {
			String pathname = getPathname(file, srcPath, desPath);
			pathname = mosSdk.getSafelyPathname(pathname);
			if (mosSdk.isExists(pathname)) {
				log.info("删除文件:{}", pathname);
				mosSdk.deleteFile(pathname);
				fileRecorder.markDeleted(file);
			}
		}
	}
	
	public class DeleteDirTask implements Runnable {
		private final File directory;
		
		public DeleteDirTask(File directory) {
			this.directory = directory;
		}
		
		@SneakyThrows
		@Override
		public void run() {
			String pathname = getPathname(directory, srcPath, desPath);
			log.info("删除文件夹:{}", pathname);
			String safelyPathname = mosSdk.getSafelyPathname(pathname);
			mosSdk.deleteDir(safelyPathname);
		}
	}
}
