package cn.mt.cloud.backup.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Martin
 * @Date 2021/1/26
 */
@Component
@Slf4j
public class DefaultFileRecorder implements FileRecorder, InitializingBean {
	private final Map<Object, Object> properties = new ConcurrentHashMap<>();
	private File recordFile;
	
	private File getRecordFile() {
		File tempDirectory = FileUtils.getTempDirectory();
		return new File(tempDirectory, "mcb/record.properties");
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Properties properties = new Properties();
		this.recordFile = getRecordFile();
		File parentFile = recordFile.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		log.info("recordFile:{}", recordFile);
		if (recordFile.exists() && recordFile.isFile()) {
			try (InputStream inputStream = new FileInputStream(recordFile)) {
				properties.load(inputStream);
				this.properties.putAll(properties);
			}
		}
	}
	
	private String getKey(File file) {
		return file.getAbsolutePath();
	}
	
	@Override
	public boolean isUploaded(File file) {
		try {
			String absolutePath = file.getCanonicalPath();
			String recordFileAbsolutePath = recordFile.getCanonicalPath();
			if (absolutePath.equals(recordFileAbsolutePath)) {
				return true;
			}
		} catch (Exception ignored) {
		}
		
		String property = properties.get(getKey(file)) + "";
		long lastModified = file.lastModified();
		return property.equals(lastModified + "");
	}
	
	@Override
	public void markUploaded(File file) {
		String key = getKey(file);
		properties.put(key, file.lastModified() + "");
	}
	
	@Override
	public void markDeleted(File file) {
		properties.remove(getKey(file));
	}
	
	@Scheduled(fixedDelay = 60_000)
	public void autoFlush() {
		flush();
	}
	
	@Override
	public void flush() {
		try (OutputStream outputStream = new FileOutputStream(recordFile)) {
			Properties properties = new Properties();
			properties.putAll(this.properties);
			properties.store(outputStream, null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
