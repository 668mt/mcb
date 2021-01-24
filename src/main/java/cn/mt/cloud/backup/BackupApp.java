package cn.mt.cloud.backup;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author Martin
 * @Date 2021/1/15
 */
@SpringBootApplication
@EnableScheduling
public class BackupApp {
	public static void main(String[] args) {
		new SpringApplicationBuilder(BackupApp.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
