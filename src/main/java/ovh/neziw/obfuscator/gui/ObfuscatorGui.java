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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import ovh.neziw.obfuscator.config.JsonConfig;

public class ObfuscatorGui extends JFrame {

    private final ConfigManager configManager;
    private final ObfuscationOptionsPanel optionsPanel;
    private final FileSelectionPanel fileSelectionPanel;
    private final IncludeListPanel includeListPanel;
    private final ButtonPanel buttonPanel;
    private final ObfuscationController obfuscationController;

    public ObfuscatorGui() {
        this.configManager = new ConfigManager();
        this.optionsPanel = new ObfuscationOptionsPanel();
        this.fileSelectionPanel = new FileSelectionPanel();
        this.includeListPanel = new IncludeListPanel();
        this.buttonPanel = new ButtonPanel();
        this.obfuscationController = new ObfuscationController(this.configManager, this.buttonPanel);
        this.initializeGUI();
        this.configManager.setParentComponent(this);
        this.obfuscationController.setParentComponent(this);
        this.fileSelectionPanel.setParentComponent(this);
        this.setupEventHandlers();
        this.loadConfig();
    }

    private void initializeGUI() {
        this.setTitle("Noobfuscator - Java Obfuscator");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(600, 650);
        this.setLocationRelativeTo(null);
        final JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        final JPanel contentPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        contentPanel.add(this.optionsPanel, gbc);
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(this.fileSelectionPanel, gbc);
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        contentPanel.add(this.includeListPanel, gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(this.buttonPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
    }

    private void setupEventHandlers() {
        this.buttonPanel.getLoadButton().addActionListener(e -> this.loadConfig(true));
        this.buttonPanel.getSaveButton().addActionListener(e -> this.saveConfig());
        this.buttonPanel.getStartObfuscationButton().addActionListener(e -> this.startObfuscation());
    }

    private void loadConfig() {
        this.loadConfig(false);
    }

    private void loadConfig(final boolean showMessage) {
        this.configManager.loadConfig(showMessage);
        this.updateUIFromConfig();
    }

    private void saveConfig() {
        this.updateConfigFromUI();
        this.configManager.saveConfig(true);
    }

    private void updateUIFromConfig() {
        final JsonConfig config = this.configManager.getConfig();
        this.optionsPanel.updateFromConfig(config);
        this.fileSelectionPanel.updateFromConfig(config);
        this.includeListPanel.updateFromConfig(config);
    }

    private void updateConfigFromUI() {
        final JsonConfig config = this.configManager.getConfig();
        this.optionsPanel.updateConfig(config);
        this.fileSelectionPanel.updateConfig(config);
        this.includeListPanel.updateConfig(config);
    }

    private void startObfuscation() {
        this.updateConfigFromUI();
        final JsonConfig config = this.configManager.getConfig();
        this.obfuscationController.startObfuscation(config);
    }
}
