package cn.mt.cloud.backup.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/15
 */
@Data
@ConfigurationProperties(prefix = "cloud")
@Component
public class BackupProperties {
	private List<PathConfig> backupPaths;
	private List<PathConfig> recoverPaths;
	
	@Data
	public static class PathConfig {
		private String srcPath;
		private List<String> includes;
		private List<String> excludes;
		private String desPath;
	}
	
}
