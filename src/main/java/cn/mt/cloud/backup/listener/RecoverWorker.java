package cn.mt.cloud.backup.listener;

import cn.mt.cloud.backup.entity.BackupProperties;
import cn.mt.cloud.backup.utils.FileUtils;
import cn.mt.cloud.backup.utils.MosHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.MosSdk;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.utils.executor.MtExecutor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @Author Martin
 * @Date 2021/1/22
 */
@Component
@Slf4j
public class RecoverWorker {
	@Autowired
	private MosSdk mosSdk;
	@Autowired
	private BackupProperties backupProperties;
	@Autowired
	private MosHelper mosHelper;
	private final MtExecutor<DownloadTask> recoverExecutor = new MtExecutor<DownloadTask>("recoverFiles", 3) {
		@Override
		public void doJob(DownloadTask task) {
			mosHelper.waitUtilMosAlive();
			DirAndResource dirAndResource = task.getDirAndResource();
			String pathname = dirAndResource.getPath();
			try {
				mosSdk.downloadFile(pathname, task.getDesFile(), true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	};
	
	//	@Scheduled(fixedDelay = 3600 * 1000L)
	@Scheduled(fixedDelay = 30 * 1000L)
	public void recover() {
		List<BackupProperties.PathConfig> recoverPaths = backupProperties.getRecoverPaths();
		if (CollectionUtils.isEmpty(recoverPaths)) {
			return;
		}
		for (BackupProperties.PathConfig recoverPath : recoverPaths) {
			String path = recoverPath.getSrcPath();
			String desPath = recoverPath.getDesPath();
			recover(path, new File(desPath), 1, 10);
		}
	}
	
	@Data
	public static class DownloadTask {
		private File desFile;
		private DirAndResource dirAndResource;
		
		@Override
		public int hashCode() {
			return Objects.hash(desFile.getAbsolutePath(), dirAndResource);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof DownloadTask)) {
				return false;
			}
			DownloadTask target = (DownloadTask) o;
			return desFile != null
					&& target.getDesFile() != null
					&& Objects.equals(desFile.getAbsolutePath(), target.getDesFile().getAbsolutePath())
					&& Objects.equals(dirAndResource, target.dirAndResource);
		}
	}
	
	private void recover(String path, File desPath, int pageNum, int pageSize) {
		try {
			mosHelper.waitUtilMosAlive();
			PageInfo<DirAndResource> pageInfo = mosSdk.list(path, null, pageNum, pageSize);
			List<DirAndResource> list = pageInfo.getList();
			if (CollectionUtils.isNotEmpty(list)) {
				list.forEach(dirAndResource -> {
					if (dirAndResource.getIsDir()) {
						String dirPath = dirAndResource.getPath();
						try {
							String relaPath = FileUtils.getRelaPath(new File(dirPath), path);
							recover(dirPath, new File(desPath, relaPath), 1, pageSize);
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					} else {
						DownloadTask downloadTask = new DownloadTask();
						File desFile = new File(desPath, dirAndResource.getFileName());
						downloadTask.setDesFile(desFile);
						downloadTask.setDirAndResource(dirAndResource);
						String pathname = dirAndResource.getPath();
						try {
							if (!recoverExecutor.contains(downloadTask) && mosSdk.isFileModified(pathname, desFile)) {
								recoverExecutor.submit(downloadTask);
							}
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
				});
			}
			int nextPage = pageInfo.getNextPage();
			if (nextPage > 0) {
				recover(path, desPath, nextPage, pageSize);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
