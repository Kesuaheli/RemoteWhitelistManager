package de.kesuaheli.remotewhitelistmanager;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
	public String host = "localhost";
	public int port = 5432;
	public String database = "data";
	public String user = "default";
	public String password = "";

	private Config(){}

	public static Config initConfig() throws IOException {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("remote-whitelist");

		if (!configDir.toFile().exists()) {
			if (!configDir.toFile().mkdirs()) {
				throw new IOException("Could not create config dir: " + configDir);
			}
		}
		File dbConfig = configDir.resolve("connection.yaml").toFile();
		Config config = new Config();
		if (dbConfig.exists()) {
			try (Reader reader = Files.newBufferedReader(dbConfig.toPath())){
				LoaderOptions loaderOptions = new LoaderOptions();
				TagInspector tagInspector = tag -> tag.getClassName().equals(Config.class.getName());
				loaderOptions.setTagInspector(tagInspector);

				config = new Yaml(new Constructor(Config.class, loaderOptions)).load(reader);
			} catch (YAMLException e) {
				throw new IOException("invalid connection config", e);
			}
		}

		config.save();
		return config;
	}

	public String SQLConnectionString() {
		return "jdbc:postgresql://"+this.host+":"+this.port+"/"+this.database;
	}
	public Properties SQLCredentials() {
		Properties credentials = new Properties();

		if (this.user != null) {
			credentials.put("user", this.user);
		}
		if (this.password != null) {
			credentials.put("password", this.password);
		}
		return credentials;
	}

	public void save() throws IOException {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("remote-whitelist");
		File dbConfig = configDir.resolve("connection.yaml").toFile();

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);

		Yaml yaml = new Yaml(options);

		try (Writer writer = Files.newBufferedWriter(dbConfig.toPath())) {
			yaml.dump(this, writer);
		}
	}
}
