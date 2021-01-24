package cn.mt.cloud.backup.utils;

import mt.spring.mos.base.utils.Assert;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
public class FileUtils {
	public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
	
	public static String getRelaPath(File file, String srcPath) throws IOException {
		String absolutePath = file.getCanonicalPath();
		String srcAbsolutePath = new File(srcPath).getCanonicalPath();
		Assert.state(absolutePath.startsWith(srcAbsolutePath), "路径校验错误");
		String path = absolutePath.substring(srcAbsolutePath.length()).replace("\\", "/");
		if (path.startsWith("/") && path.length() > 1) {
			path = path.substring(1);
		}
		return path;
	}
	
	public static boolean match(String relaPathname, List<String> includes, List<String> excludes) {
		boolean match = true;
		if (includes != null) {
			for (String include : includes) {
				if (!ANT_PATH_MATCHER.match(include, relaPathname)) {
					match = false;
					break;
				}
			}
		}
		if (match && excludes != null) {
			for (String exclude : excludes) {
				if (ANT_PATH_MATCHER.match(exclude, relaPathname)) {
					match = false;
					break;
				}
			}
		}
		return match;
	}
	
	public static String getPathname(File file, String srcPath, String desPath) throws IOException {
		String relaPath = FileUtils.getRelaPath(file, srcPath);
		File desPathFile = new File(desPath, relaPath);
		return desPathFile.getPath().replace("\\", "/");
	}
}
