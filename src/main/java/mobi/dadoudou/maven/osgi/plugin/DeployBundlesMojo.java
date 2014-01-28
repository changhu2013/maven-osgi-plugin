package mobi.dadoudou.maven.osgi.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.GetMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * 将Riambsoft Framework 依赖的OSGI Bundle部署到Framework框架中
 * 
 * @goal deployBundles
 * 
 * @phase test
 */
public class DeployBundlesMojo extends GetMojo {
	
	private static final String FRAMEWORK_BUNDLES_PROPERTIES_KEY = "scanning.bundle.autostart";
	
	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @component
	 */
	protected ArtifactFactory artifactFactory;

	/**
	 * @component
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * @parameter expression="${localRepository}"
	 */
	private ArtifactRepository localRepository;

	/**
	 * @component
	 */
	private ArtifactMetadataSource source;

	/**
	 * @parameter expression="${frameworkConfigurationFile}"
	 * @required
	 */
	private File frameworkConfigurationFile;

	/**
	 * @parameter expression="${osgiPlatformDirectory}"
	 * @required
	 */
	private String osgiPlatformDirectory;

	/**
	 * @parameter expression="${excludeDependency}"
	 * @required
	 */
	private String excludeDependency;

	/**
	 * 
	 * @parameter expression="${bundles}"
	 */
	@SuppressWarnings("rawtypes")
	private List<Map> bundles;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 */
	private List<ArtifactRepository> pomRemoteRepositories;

	/**
	 * 日志
	 */
	private Log logger = getLog();

	public void execute() throws MojoExecutionException, MojoFailureException {

		File f = outputDirectory;

		if (!f.exists()) {
			f.mkdirs();
		}

		logger.debug("frameworkConfigurationFile : "
				+ frameworkConfigurationFile);
		logger.debug("osgiPlatformDirectory : " + osgiPlatformDirectory);

		Map<String, Artifact> artifacts = new HashMap<String, Artifact>();
		Map<String, Boolean> autoStarts = new HashMap<String, Boolean>();
		Map<String, String> startLevels = new HashMap<String, String>();
		
		
		// 将依赖的bundle拷贝到指定目录
		for (@SuppressWarnings("rawtypes")
		Map bundle : bundles) {

			String groupId = (String) bundle.get("groupId");
			String artifactId = (String) bundle.get("artifactId");
			String version = (String) bundle.get("version");
			String type = (String) bundle.get("type");
			type = (type == null || "".equals(type)) ? "jar" : type;
			String classifier = (String) bundle.get("classifier");
			String startLevel = (String)bundle.get("startLevel");
			Boolean autoStart = new Boolean(!"false".equals(bundle
					.get("autoStart")));

			String msg = "bundle[" + groupId + ":" + artifactId + ":" + version
					+ ":" + classifier + "] autoStart : " + autoStart;
			logger.debug(msg);

			Artifact artifact = convert(groupId, artifactId, version, type,
					classifier);
			
			//根据配置设置启动级别
			if(startLevel != null && !"".equals(startLevel.trim())){
				startLevels.put(generateKey(artifact), startLevel);
			}
			
			autoStarts.put(generateKey(artifact), autoStart);

			if ("pom".equals(artifact.getType())) {

				deployPom(artifact, artifacts);
			} else {

				deployJar(artifact, artifacts);
			}
		}

		StringBuffer buf = new StringBuffer();
		for (Iterator<String> iter = artifacts.keySet().iterator(); iter
				.hasNext();) {
			String key = iter.next();
			Artifact arti = artifacts.get(key);

			// 将artifacts的文件拷贝到指定目录
			File src = arti.getFile();
			File dest = new File(osgiPlatformDirectory + "/features/"
					+ src.getName());
			logger.info("拷贝" + src.getAbsolutePath() + "至"
					+ dest.getAbsolutePath());
			try {
				FileUtils.copyFile(src, dest);
			} catch (IOException e) {
				throw new MojoExecutionException("无法下载 "
						+ src.getAbsolutePath() + "至" + dest.getAbsolutePath()
						+ " : " + e.getMessage(), e);
			}

			
			String  startLevel = startLevels.get(generateKey(arti));
			Boolean autoStart = autoStarts.get(generateKey(arti));
			
			String temp = "";
			if(startLevel != null){
				if(autoStart != null && autoStart.booleanValue() == false){
					temp = "@" + startLevel;
				}else {
					temp = "@" + startLevel + ":start"; 
				}
			}else {
				if(!(autoStart != null && autoStart.booleanValue() == false)){
					temp = "@start"; 
				}
			}
			
			// 用于生成启动设置
			buf.append("reference:file:features/" + dest.getName() + temp + ",");
		}

		// 生成自启动Bundle 的脚本配置文件
		if (artifacts.size() > 0) {
			File temp = null;
			try {
				temp = File.createTempFile("framework_", ".properties",
						outputDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}

			Properties result = new Properties();
			result.setProperty(FRAMEWORK_BUNDLES_PROPERTIES_KEY,
					buf.substring(0, buf.length() - 1));

			OutputStream out = null;
			try {
				out = new FileOutputStream(temp);
				result.store(out, "WWW.RIAMBSOFT.COM");
			} catch (Exception e) {
				logger.warn("写入framework.properties配置文件发生异常", e);
			} finally {
				if (out != null) {
					try {
						out.flush();
						out.close();
					} catch (IOException e) {
					}
				}
			}

			try {
				FileUtils.copyFile(temp, frameworkConfigurationFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// 删除临时文件
			temp.delete();
		}
	}

	// 根据groupId等信息找到artifact
	private Artifact convert(String groupId, String artifactId, String version,
			String type, String classifier) throws MojoExecutionException {

		Artifact artifact = classifier == null ? artifactFactory
				.createBuildArtifact(groupId, artifactId, version, type)
				: artifactFactory.createArtifactWithClassifier(groupId,
						artifactId, version, type, classifier);

		Artifact dummyOriginatingArtifact = artifactFactory
				.createBuildArtifact("org.apache.maven.plugins",
						"maven-downloader-plugin", "1.0", "jar");

		List<ArtifactRepository> repoList = new ArrayList<ArtifactRepository>();

		if (pomRemoteRepositories != null) {
			repoList.addAll(pomRemoteRepositories);
		}

		try {
			artifactResolver.resolveTransitively(
					Collections.singleton(artifact), dummyOriginatingArtifact,
					repoList, localRepository, source);

		} catch (AbstractArtifactResolutionException e) {
			throw new MojoExecutionException("Couldn't download artifact: "
					+ e.getMessage(), e);
		}

		return artifact;
	}

	private void deployPom(Artifact artifact, Map<String, Artifact> artifacts) {

		try {
			// 根据POM文件文件将该artifact依赖的其他artifacts拷贝进来
			// 根据POM里将模块也拷贝进来
			MavenXpp3Reader mavenreader = new MavenXpp3Reader();
			Model model = mavenreader.read(new FileReader(artifact.getFile()));
			MavenProject project = new MavenProject(model);

			String groupId = project.getGroupId();
			String version = project.getVersion();

			@SuppressWarnings("unchecked")
			List<Dependency> dependencies = (List<Dependency>) project
					.getDependencies();

			for (Dependency deps : dependencies) {
				String scope = deps.getScope();

				// 如果不是排除的并且不是特殊范围(test, provided, system, import,
				// runtime)的,则将该依赖添加安装
				if (!isExcludeDependency(deps) && !"test".equals(scope)
						&& !"provided".equals(scope) && !"system".equals(scope)
						&& !"import".equals(scope) && !"runtime".equals(scope)) {

					Artifact temp = convert(deps.getGroupId(),
							deps.getArtifactId(), deps.getVersion(),
							deps.getType(), deps.getClassifier());
					deployJar(temp, artifacts);
				}
			}

			@SuppressWarnings("unchecked")
			List<String> modules = (List<String>) project.getModules();

			for (String module : modules) {

				Artifact temp = convert(groupId, module, version, "jar", null);

				deployJar(temp, artifacts);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void deployJar(Artifact artifact, Map<String, Artifact> artifacts)
			throws MojoExecutionException {

		// 将该artifact添加到集合中
		String key = generateKey(artifact);
		artifacts.put(key, artifact);

		// 将该artifact的依赖项添加到集合
		Artifact pom = convert(artifact.getGroupId(), artifact.getArtifactId(),
				artifact.getVersion(), "pom", artifact.getClassifier());

		deployPom(pom, artifacts);
	}

	private String generateKey(Artifact artifact) {
		return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
				+ artifact.getType() + ":" + artifact.getVersion();
	}

	private boolean isExcludeDependency(Dependency deps) {
		if (excludeDependency != null && !"".equals(excludeDependency.trim())) {
			String arti = deps.getArtifactId();
			String temp = excludeDependency.trim();
			String[] excs = temp.split(",");
			for (String ex : excs) {
				if (ex != null && !"".equals(ex)) {
					ex = ex.replace("*", "");
					if (arti.indexOf(ex) > -1) {
						logger.debug("排除依赖 " + deps.getGroupId() + ":"
								+ deps.getArtifactId() + ":"
								+ deps.getVersion());
						return true;
					}
				}
			}
		}
		return false;
	}

}
