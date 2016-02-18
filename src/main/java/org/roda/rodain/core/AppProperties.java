package org.roda.rodain.core;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.filechooser.FileSystemView;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.roda.rodain.utils.FolderBasedUTF8Control;
import org.roda.rodain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 28/12/2015.
 */
public class AppProperties {
  private static final String ENV_VARIABLE = "RODAIN_HOME";
  private static final String CONFIGFOLDER = "roda-in";
  public static final Path rodainPath = getRodainPath();
  private static final Logger log = LoggerFactory.getLogger(AppProperties.class.getName());
  private static PropertiesConfiguration style = load("styles"), config = load("config"), ext_config,
    descLevels = load("roda-description-levels-hierarchy");
  private static ResourceBundle resourceBundle;
  public static Locale locale;

  private AppProperties() {

  }

  /**
   * Creates the external properties files if they don't exist. Loads the
   * external properties files.
   */
  public static void initialize() {
    Path configPath = rodainPath.resolve("config.properties");

    try {
      // create folder in home if it doesn't exist
      if (!Files.exists(rodainPath)) {
        rodainPath.toFile().mkdir();
      }
      // copy config file
      if (!Files.exists(configPath)) {
        Files.copy(ClassLoader.getSystemResourceAsStream("properties/config.properties"), configPath);
      } else { // if the file already exists, we need to check if it's missing
               // any properties
        PropertiesConfiguration internal = load("config");
        PropertiesConfiguration external = new PropertiesConfiguration();
        external.load(new FileReader(configPath.toFile()));
        boolean store = false;
        Iterator<String> keys = internal.getKeys();
        while (keys.hasNext()) {
          String key = keys.next();
          if (!external.containsKey(key)) {
            external.addProperty(key, internal.getString(key));
            store = true;
          }
        }
        if (store) {
          OutputStream out = new FileOutputStream(configPath.toFile());
          external.save(out);
        }
      }
      // copy metadata templates
      String templatesRaw = config.getString("metadata.templates");
      String[] templates = templatesRaw.split(",");
      for (String templ : templates) {
        String templateName = "metadata.template." + templ.trim() + ".file";
        String fileName = config.getString(templateName);
        // if (!Files.exists(rodainPath.resolve(fileName)))
        Files.copy(ClassLoader.getSystemResourceAsStream("templates/" + fileName), rodainPath.resolve(fileName),
          StandardCopyOption.REPLACE_EXISTING);

        String schemaName = "metadata.template." + templ.trim() + ".schema";
        String schemaFileName = config.getString(schemaName);
        // if (!Files.exists(rodainPath.resolve(fileName)))
        Files.copy(ClassLoader.getSystemResourceAsStream("templates/" + schemaFileName),
          rodainPath.resolve(schemaFileName), StandardCopyOption.REPLACE_EXISTING);
      }

      // load config
      ext_config = new PropertiesConfiguration();
      ext_config.load(new FileInputStream(configPath.toFile()));

      String appLanguage = getConfig("app.language");
      if (appLanguage != null) {
        locale = Locale.forLanguageTag(appLanguage);
      } else {
        locale = Locale.getDefault();
      }

      resourceBundle = ResourceBundle.getBundle("properties/lang", locale, new FolderBasedUTF8Control());
    } catch (IOException e) {
      log.error("Error copying config file", e);
    } catch (MissingResourceException e) {
      locale = Locale.forLanguageTag("en");
      resourceBundle = ResourceBundle.getBundle("properties/lang", locale, new FolderBasedUTF8Control());
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }

  private static Path getRodainPath() {
    String envString = System.getenv(ENV_VARIABLE);
    if (envString != null) {
      Path envPath = Paths.get(envString);
      if (Files.exists(envPath) && Files.isDirectory(envPath)) {
        return envPath.resolve(CONFIGFOLDER);
      }
    }
    String documentsString = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
    Path documentsPath = Paths.get(documentsString);
    return documentsPath.resolve(CONFIGFOLDER);
  }

  private static PropertiesConfiguration load(String fileName) {
    PropertiesConfiguration result = null;
    try {
      result = new PropertiesConfiguration("properties/" + fileName + ".properties");
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static String getMetadataFile(String propertyName) {
    String completeKey = "metadata.template." + propertyName + ".file";
    return getFile(completeKey);
  }

  public static String getSchemaFile(String propertyName) {
    String completeKey = "metadata.template." + propertyName + ".schema";
    return getFile(completeKey);
  }

  public static Path getSchemaPath(String propertyName) {
    String completeKey = "metadata.template." + propertyName + ".schema";
    if (ext_config.containsKey(completeKey)) {
      Path filePath = rodainPath.resolve(ext_config.getString(completeKey));
      if (Files.exists(filePath)) {
        return filePath;
      }
    }
    String fileName = config.getString(completeKey);
    URL temp = ClassLoader.getSystemResource("templates/" + fileName);
    if (temp != null)
      return Paths.get(temp.getPath());
    else
      return null;
  }

  private static String getFile(String completeKey) {
    try {
      if (ext_config.containsKey(completeKey)) {
        Path filePath = rodainPath.resolve(ext_config.getString(completeKey));
        if (Files.exists(filePath)) {
          return Utils.readFile(filePath.toString(), Charset.defaultCharset());
        }
      }
      String fileName = config.getString(completeKey);
      URL temp = ClassLoader.getSystemResource("templates/" + fileName);
      if (temp == null) {
        return "";
      }
      InputStream contentStream = temp.openStream();
      return Utils.convertStreamToString(contentStream);
    } catch (IOException e) {
      log.error("Error reading metadata file", e);
    }
    return "";
  }

  /**
   * @param key
   *          The name of the property (style)
   * @return The value of the property (style)
   */
  public static String getStyle(String key) {
    return style.getString(key);
  }

  /**
   * @param key
   *          The name of the property (config)
   * @return The value of the property (config)
   */
  public static String getConfig(String key) {
    Object res;
    if (ext_config.containsKey(key)) {
      res = ext_config.getProperty(key);
    } else {
      res = config.getProperty(key);
    }
    if (res instanceof String) {
      return (String) res;
    }
    // if it isn't a string then it must be a list Ex: a,b,c,d
    return String.join(",", config.getStringArray(key));
  }

  /**
   * @param key
   *          The name of the property (description levels hierarchy)
   * @return The value of the property (description levels hierarchy)
   */
  public static String getDescLevels(String key) {
    Object res = descLevels.getProperty(key);
    if (res instanceof String) {
      return (String) res;
    }
    // if it isn't a string then it must be a list Ex: a,b,c,d
    return String.join(",", descLevels.getStringArray(key));
  }

  /**
   * Uses ResourceBundle to get the language specific string
   *
   * @param key
   *          The name of the property
   * @return The value of the property using
   */
  public static String getLocalizedString(String key) {
    return resourceBundle.getString(key);
  }

  /**
   * Sets the value of a configuration.
   * 
   * @param key
   *          The key of the property.
   * @param value
   *          The value of the property
   */
  public static void setConfig(String key, String value) {
    ext_config.setProperty(key, value);
  }

  public static void saveConfig() {
    try {
      ext_config.save(rodainPath.resolve("config.properties").toFile());
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
  }
}
