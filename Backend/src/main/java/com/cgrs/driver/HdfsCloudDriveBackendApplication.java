package com.cgrs.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/*作用：Spring Boot 应用的入口，启动整个后端服务。

逻辑：运行它的 main 方法 → 启动内嵌 Tomcat（8080端口）
 → 扫描所有 @Component、@Controller、@Service 等注解的类并加载。*/

@SpringBootApplication
public class HdfsCloudDriveBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HdfsCloudDriveBackendApplication.class, args);
	}

}
