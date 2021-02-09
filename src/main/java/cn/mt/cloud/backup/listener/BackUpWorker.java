package cn.mt.cloud.backup.listener;

import cn.mt.cloud.backup.entity.BackupProperties;
import cn.mt.cloud.backup.utils.MosHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.CollectionUtils;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.utils.executor.MtExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.mt.cloud.backup.utils.FileUtils.getPathname;

/**
 * @Author Martin
 * @Date 2021/1/15
 */
@Slf4j
@Component
public class BackUpWorker implements SmartLifecycle {
	private final MosSdk mosSdk;
	private final BackupProperties backupProperties;
	@Autowired
	private MosHelper mosHelper;
	@Autowired
	private FileRecorder fileRecorder;
	
	private final MtExecutor<Runnable> addFilesExecutor = new MtExecutor<Runnable>("addFiles", 1) {
		@Override
		public void doJob(Runnable task) {
			if (!loaded.get()) {
				synchronized (loaded) {
					try {
						loaded.wait();
					} catch (InterruptedException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			mosHelper.waitUtilMosAlive();
			task.run();
		}
	};
	
	public BackUpWorker(MosSdk mosSdk, BackupProperties backupProperties) {
		this.mosSdk = mosSdk;
		this.backupProperties = backupProperties;
	}
	
	private volatile boolean running;
	
	@Override
	public void stop() {
		running = false;
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}
	
	@Override
	public void start() {
		if (running) {
			return;
		}
		try {
			List<BackupProperties.PathConfig> backupPaths = backupProperties.getBackupPaths();
			if (CollectionUtils.isEmpty(backupPaths)) {
				log.warn("没有配置备份路径");
				return;
			}
			for (BackupProperties.PathConfig backupPath : backupPaths) {
				String srcPath = backupPath.getSrcPath();
				String desPath = backupPath.getDesPath();
				log.info("start listen: {}", srcPath);
				loadFiles(backupPath);
				FileAlterationObserver observer = new FileAlterationObserver(new File(srcPath), new IncludeFileFilter(backupPath));
				observer.addListener(new BackupFileListener(fileRecorder, addFilesExecutor, mosSdk, srcPath, desPath));
				FileAlterationMonitor monitor = new FileAlterationMonitor();
				monitor.addObserver(observer);
				monitor.start();
			}
			running = true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	private final AtomicBoolean loaded = new AtomicBoolean(false);
	private MtExecutor<Runnable> loadFilesExecutor;
	
	private synchronized void initLoadFilesExecutor() {
		if (loadFilesExecutor != null) {
			return;
		}
		loadFilesExecutor = new MtExecutor<Runnable>("loadFiles", 2) {
			@Override
			public void doJob(Runnable task) {
				mosHelper.waitUtilMosAlive();
				task.run();
			}
		};
		loadFilesExecutor.setEvent(new MtExecutor.Event<Runnable>() {
			@Override
			public void onTaskFinished(MtExecutor<Runnable> mtExecutor) {
				mtExecutor.shutdown();
				synchronized (loaded) {
					loaded.set(true);
					loaded.notifyAll();
				}
				fileRecorder.flush();
			}
		});
	}
	
	private void loadFiles(BackupProperties.PathConfig backupPath) {
		initLoadFilesExecutor();
		String srcPath = backupPath.getSrcPath();
		List<String> includes = backupPath.getIncludes();
		List<String> excludes = backupPath.getExcludes();
		File file = new File(srcPath);
		FileUtils.listFiles(file, new IOFileFilter() {
			@SneakyThrows
			@Override
			public boolean accept(File file) {
				String relaPath = cn.mt.cloud.backup.utils.FileUtils.getRelaPath(file, srcPath);
				return cn.mt.cloud.backup.utils.FileUtils.match(relaPath, includes, excludes);
			}
			
			@Override
			public boolean accept(File dir, String name) {
				return false;
			}
		}, new IOFileFilter() {
			@Override
			public boolean accept(File file) {
				return true;
			}
			
			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		}).forEach(file1 -> loadFilesExecutor.submit(new CheckFileTask(file1, backupPath)));
	}
	
	public class CheckFileTask implements Runnable {
		private final File file;
		private final BackupProperties.PathConfig pathConfig;
		
		public CheckFileTask(File file, BackupProperties.PathConfig pathConfig) {
			this.file = file;
			this.pathConfig = pathConfig;
		}
		
		@Override
		public void run() {
			try {
				String srcPath = pathConfig.getSrcPath();
				String desPath = pathConfig.getDesPath();
				String pathname = getPathname(file, srcPath, desPath);
				if (!fileRecorder.isUploaded(file)) {
					if (mosSdk.isFileModified(pathname, file)) {
						log.info("上传文件{}", file);
						mosSdk.uploadFile(file, new UploadInfo(pathname, true));
					}
					fileRecorder.markUploaded(file);
				} else {
					log.info("{}已经上传过，跳过上传", file);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}
}
