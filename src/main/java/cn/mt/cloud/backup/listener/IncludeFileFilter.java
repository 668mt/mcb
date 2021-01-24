package cn.mt.cloud.backup.listener;

import cn.mt.cloud.backup.entity.BackupProperties;
import cn.mt.cloud.backup.utils.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import static cn.mt.cloud.backup.utils.FileUtils.match;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
public class IncludeFileFilter implements FileFilter {
	private final BackupProperties.PathConfig pathConfig;
	
	public IncludeFileFilter(BackupProperties.PathConfig pathConfig) {
		this.pathConfig = pathConfig;
	}
	
	@Override
	public boolean accept(File file) {
		try {
			String srcPath = pathConfig.getSrcPath();
			List<String> includes = pathConfig.getIncludes();
			List<String> excludes = pathConfig.getExcludes();
			String relaPath = FileUtils.getRelaPath(file, srcPath);
			return match(relaPath, includes, excludes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
