/*
 * This file is part of "Noobfuscator", licensed under MIT License.
 *
 *  Copyright (c) 2025 neziw
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package ovh.neziw.obfuscator.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import lombok.Getter;
import lombok.Setter;
import ovh.neziw.obfuscator.config.JsonConfig;

public class ConfigManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());

    private static final String CONFIG_FILE = "settings.json";
    private final Gson gson;
    @Setter
    @Getter
    private JsonConfig config;
    @Setter
    private java.awt.Component parentComponent;

    public ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = new JsonConfig();
    }

    public void loadConfig(final boolean showMessage) {
        final File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (final FileReader reader = new FileReader(configFile)) {
                this.config = this.gson.fromJson(reader, JsonConfig.class);
                if (this.config == null) {
                    this.config = new JsonConfig();
                }
                if (showMessage) {
                    JOptionPane.showMessageDialog(this.parentComponent, "Configuration loaded successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (final IOException exception) {
                LOGGER.warning("Error loading configuration: " + exception.getMessage());
                LOGGER.throwing(ConfigManager.class.getName(), "loadConfig", exception);
                if (showMessage) {
                    JOptionPane.showMessageDialog(this.parentComponent,
                        "Error loading configuration: " + exception.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
                this.config = new JsonConfig();
            }
        } else {
            this.config = new JsonConfig();
        }
    }

    public void saveConfig(final boolean showMessage) {
        final File configFile = new File(CONFIG_FILE);
        try (final FileWriter writer = new FileWriter(configFile)) {
            this.gson.toJson(this.config, writer);
            if (showMessage) {
                JOptionPane.showMessageDialog(this.parentComponent, "Configuration saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (final IOException exception) {
            LOGGER.warning("Error saving configuration: " + exception.getMessage());
            LOGGER.throwing(ConfigManager.class.getName(), "saveConfig", exception);
            if (showMessage) {
                JOptionPane.showMessageDialog(this.parentComponent,
                    "Error saving configuration: " + exception.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
