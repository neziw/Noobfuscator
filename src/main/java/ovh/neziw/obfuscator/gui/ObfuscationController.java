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

import java.io.File;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Setter;
import ovh.neziw.obfuscator.config.JsonConfig;

public class ObfuscationController {

    private static final Logger LOGGER = Logger.getLogger(ObfuscationController.class.getName());

    private final ConfigManager configManager;
    private final ButtonPanel buttonPanel;
    @Setter
    private java.awt.Component parentComponent;

    public ObfuscationController(final ConfigManager configManager, final ButtonPanel buttonPanel) {
        this.configManager = configManager;
        this.buttonPanel = buttonPanel;
    }

    public void startObfuscation(final JsonConfig config) {
        if (config.getInputJarName().isEmpty()) {
            JOptionPane.showMessageDialog(this.parentComponent,
                "Please specify input JAR file!",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (config.getOutputJarName().isEmpty()) {
            JOptionPane.showMessageDialog(this.parentComponent,
                "Please specify output JAR file!",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final File inputFile = new File(config.getInputJarName());
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this.parentComponent,
                "Input JAR file does not exist!",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.configManager.saveConfig(false);
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    this.buttonPanel.setStartButtonEnabled(false);
                    this.buttonPanel.setStartButtonText("Obfuscating...");
                });
                final ovh.neziw.obfuscator.obfuscator.JarProcessor processor =
                    new ovh.neziw.obfuscator.obfuscator.JarProcessor(config);
                processor.processJar(config.getInputJarName(), config.getOutputJarName());

                final ovh.neziw.obfuscator.obfuscator.stats.ObfuscationStatsCollector stats = processor.getStats();

                SwingUtilities.invokeLater(() -> {
                    this.buttonPanel.setStartButtonEnabled(true);
                    this.buttonPanel.setStartButtonText("Start Obfuscation");
                    final StringBuilder statsMsg = new StringBuilder();
                    statsMsg.append("Obfuscation completed successfully!\n\n");
                    statsMsg.append("Input: ").append(config.getInputJarName()).append("\n");
                    statsMsg.append("Output: ").append(config.getOutputJarName()).append("\n\n");
                    statsMsg.append("Statistics:\n");
                    statsMsg.append("- Classes processed: ").append(stats.getClassesProcessed()).append("\n");
                    statsMsg.append("- Classes obfuscated: ").append(stats.getClassesObfuscated()).append("\n");
                    if (config.isObfuscateClassNames() || config.isObfuscatePackages()) {
                        statsMsg.append("- Class names obfuscated: ").append(stats.getClassNameStats().getClassesObfuscated()).append("\n");
                    }
                    if (config.isObfuscateVariables()) {
                        statsMsg.append("- Variables obfuscated: ").append(stats.getVariablesObfuscated()).append("\n");
                    }
                    if (config.isObfuscateStrings()) {
                        statsMsg.append("- Strings obfuscated: ").append(stats.getStringStats().getStringsObfuscated()).append("\n");
                        statsMsg.append("- Classes with obfuscated strings: ").append(stats.getStringStats().getClassesWithObfuscatedStrings()).append("\n");
                    }
                    if (config.isObfuscateMethodNames()) {
                        statsMsg.append("- Method names obfuscated: ").append(stats.getMethodNameStats().getMethodsObfuscated()).append("\n");
                    }
                    if (config.isChangeMethodsOrders()) {
                        statsMsg.append("- Methods reordered: ").append(stats.getMethodOrderStats().getMethodsReordered()).append("\n");
                    }
                    final String flowObf = config.getFlowObfuscation();
                    if (flowObf != null && !flowObf.equals("NONE")) {
                        statsMsg.append("- Flow obfuscated: ").append(stats.getFlowStats().getMethodsObfuscated()).append(" methods, ").append(stats.getFlowStats().getInstructionsAdded()).append(" instructions added\n");
                    }
                    JOptionPane.showMessageDialog(this.parentComponent, statsMsg.toString(),
                        "Obfuscation Complete", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (final Exception exception) {
                LOGGER.severe("Error during obfuscation: " + exception.getMessage());
                LOGGER.throwing(ObfuscationController.class.getName(), "startObfuscation", exception);
                SwingUtilities.invokeLater(() -> {
                    this.buttonPanel.setStartButtonEnabled(true);
                    this.buttonPanel.setStartButtonText("Start Obfuscation");

                    JOptionPane.showMessageDialog(this.parentComponent,
                        "Error during obfuscation:\n" + exception.getMessage() + "\n\n" +
                            "Stack trace: " + this.getStackTrace(exception),
                        "Obfuscation Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private String getStackTrace(final Exception exception) {
        final java.io.StringWriter sw = new java.io.StringWriter();
        final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}
