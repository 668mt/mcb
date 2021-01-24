package cn.mt.cloud.backup.utils;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.MosSdk;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author Martin
 * @Date 2021/1/24
 */
@Component
@Slf4j
public class MosHelper {
	private final MosSdk mosSdk;
	
	public MosHelper(MosSdk mosSdk) {
		this.mosSdk = mosSdk;
	}
	
	@Scheduled(fixedDelay = 30 * 1000L)
	public void checkMosAlive() {
		if (isMosAlive()) {
			log.debug("checked mos is alive, releasing all wait threads...");
			synchronized (mosSdk) {
				mosSdk.notifyAll();
			}
		}
	}
	
	public void waitUtilMosAlive() {
		boolean mosAlive = isMosAlive();
		if (mosAlive) {
			synchronized (mosSdk) {
				mosSdk.notifyAll();
			}
		} else {
			try {
				synchronized (mosSdk) {
					mosSdk.wait();
				}
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	public boolean isMosAlive() {
		try {
			mosSdk.list("/", null, 1, 1);
			return true;
		} catch (IOException e) {
			log.error("mos服务器连接失败：{}", e.getMessage());
			return false;
		}
	}
}
