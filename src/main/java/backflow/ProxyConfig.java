package backflow;


import org.andrejs.json.Json;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class ProxyConfig {

    private static final Logger log = Logger.getLogger(ProxyServer.class.getSimpleName());

    public static Json loadConfig() {
        Properties props = new Properties();
        Yaml yml = new Yaml();

        Path ymlfile = Paths.get("proxy.yml");
        Map<String, Object> config;

        if(Files.exists(ymlfile)) {
            try(InputStream is = Files.newInputStream(ymlfile)) {
                config = (Map<String, Object>) yml.load(is);
            } catch (IOException e) {
                log.error("Could not load " + ymlfile.toAbsolutePath(), e);
                throw new IOError(e);
            }
            log.info("Loaded config from " + ymlfile.toAbsolutePath() + ": " + config);
        } else {
            ClassLoader classLoader = ProxyConfig.class.getClassLoader();
            config = (Map<String, Object>) yml.load(classLoader.getResourceAsStream("proxy.yml"));
            log.info("Loaded config from classpath proxy.yml: " + config);
        }

//        flatten(config, props, "");
        return new Json(config);
    }

    private static void flatten(Map<String, Object> load, Properties props, String prefix) {
        for(String key: load.keySet()) {
            Object val = load.get(key);
            String pref = prefix.isEmpty() ? "" : prefix + ".";
            if(val instanceof Map)
                flatten((Map<String, Object>) val, props, pref + key);
            else
                props.put(pref + key, val.toString());
        }
    }
}
