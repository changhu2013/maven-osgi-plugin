maven-osgi-plugin
================================================================

用于为OSGI Bundle的安装

================================================================

使用方式：

================================================================

    <plugin>
		<groupId>mobi.dadoudou</groupId>
		<artifactId>maven-osgi-plugin</artifactId>
		<version>0.0.1-SNAPSHOT</version>
		<configuration>
					
			<!-- 以下为框架启动的默认配置文件  -->
			<frameworkConfigurationFile>src/main/webapp/WEB-INF/classes/bundles.properties</frameworkConfigurationFile>
						
			<!-- 以下为OSGI容器的目录 -->
			<osgiPlatformDirectory>src/main/webapp/WEB-INF/eclipse</osgiPlatformDirectory>
						
			<!-- 以下为排除的OSGI Bundle -->
			<excludeDependency>framework.*,org.junit</excludeDependency>
			
			<bundles>
	
				<!-- 以下为blueprint所需bundle 将默认安装到OSGI容器中 -->
				<bundle>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-api</artifactId>
					<version>1.6.4.v20120130-2120</version>
					<startLevel>1</startLevel>
				</bundle>
	
				<bundle>
					<groupId>org.apache.aries.blueprint</groupId>
					<artifactId>org.apache.aries.blueprint</artifactId>
					<version>1.0.0</version>
					<startLevel>1</startLevel>
				</bundle>
	
				<bundle>
					<groupId>org.apache.aries.blueprint</groupId>
					<artifactId>org.apache.aries.blueprint.api</artifactId>
					<version>1.0.0</version>
					<startLevel>1</startLevel>
				</bundle>
	
				<bundle>
					<groupId>org.apache.aries.proxy</groupId>
					<artifactId>org.apache.aries.proxy.impl</artifactId>
					<version>1.0.0</version>
					<startLevel>1</startLevel>
				</bundle>
	
				<bundle>
					<groupId>org.eclipse.persistence</groupId>
					<artifactId>javax.persistence</artifactId>
					<version>2.0.0</version>
					<startLevel>1</startLevel>
				</bundle>
							
			</bundles>
		</configuration>
					
		<executions>
					    
			<!-- 以下为配置当运行test命令的时候运行deployBundles命令以安装Bundle -->
			<execution>
				<id>deploy bundles</id>
				<phase>test</phase>
				<goals>
					<goal>deployBundles</goal>
				</goals>
			</execution>
		</executions>
    </plugin>

  
