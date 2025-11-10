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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.Setter;
import ovh.neziw.obfuscator.config.JsonConfig;

public class FileSelectionPanel extends JPanel {

    private JTextField inputJarNameField;
    private JTextField outputJarNameField;
    @Setter
    private java.awt.Component parentComponent;

    public FileSelectionPanel() {
        this.initializePanel();
    }

    private void initializePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        this.add(new JLabel("Input JAR:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.inputJarNameField = new JTextField(20);
        this.add(this.inputJarNameField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        final JButton browseInputButton = new JButton("Browse...");
        browseInputButton.addActionListener(e -> this.browseInputJar());
        this.add(browseInputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        this.add(new JLabel("Output JAR:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.outputJarNameField = new JTextField(20);
        this.add(this.outputJarNameField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        final JButton browseOutputButton = new JButton("Browse...");
        browseOutputButton.addActionListener(e -> this.browseOutputJar());
        this.add(browseOutputButton, gbc);
    }

    private void browseInputJar() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
        fileChooser.setDialogTitle("Select Input JAR File");
        final java.awt.Component parent = this.parentComponent != null ? this.parentComponent : this;
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            this.inputJarNameField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void browseOutputJar() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
        fileChooser.setDialogTitle("Select Output JAR File");
        final java.awt.Component parent = this.parentComponent != null ? this.parentComponent : this;
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getAbsolutePath();
            if (!fileName.endsWith(".jar")) {
                fileName += ".jar";
            }
            this.outputJarNameField.setText(fileName);
        }
    }

    public void updateFromConfig(final JsonConfig config) {
        this.inputJarNameField.setText(config.getInputJarName());
        this.outputJarNameField.setText(config.getOutputJarName());
    }

    public void updateConfig(final JsonConfig config) {
        config.setInputJarName(this.inputJarNameField.getText());
        config.setOutputJarName(this.outputJarNameField.getText());
    }
}
