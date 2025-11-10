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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import ovh.neziw.obfuscator.config.JsonConfig;

public class ObfuscationOptionsPanel extends JPanel {

    private JCheckBox obfuscateClassNamesCheck;
    private JCheckBox obfuscatePackagesCheck;
    private JCheckBox obfuscateVariablesCheck;
    private JCheckBox obfuscateStringsCheck;
    private JCheckBox obfuscateMethodNamesCheck;
    private JCheckBox changeMethodsOrdersCheck;
    private JCheckBox crashClassCheck;
    private JCheckBox generateMappingsCheck;
    private JComboBox<String> flowObfuscationCombo;
    private JTextField watermarkField;

    public ObfuscationOptionsPanel() {
        this.initializePanel();
    }

    private void initializePanel() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        this.add(new JLabel("Obfuscation Options:"), gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        this.obfuscateClassNamesCheck = new JCheckBox("Obfuscate Class Names");
        this.add(this.obfuscateClassNamesCheck, gbc);

        gbc.gridy = 2;
        this.obfuscatePackagesCheck = new JCheckBox("Obfuscate Packages");
        this.add(this.obfuscatePackagesCheck, gbc);

        gbc.gridy = 3;
        this.obfuscateVariablesCheck = new JCheckBox("Obfuscate Variables");
        this.add(this.obfuscateVariablesCheck, gbc);

        gbc.gridy = 4;
        this.obfuscateStringsCheck = new JCheckBox("Obfuscate Strings");
        this.add(this.obfuscateStringsCheck, gbc);

        gbc.gridy = 5;
        this.obfuscateMethodNamesCheck = new JCheckBox("Obfuscate Method Names");
        this.add(this.obfuscateMethodNamesCheck, gbc);

        gbc.gridy = 6;
        this.changeMethodsOrdersCheck = new JCheckBox("Change Methods Order");
        this.add(this.changeMethodsOrdersCheck, gbc);

        gbc.gridy = 7;
        this.crashClassCheck = new JCheckBox("Crash Class");
        this.add(this.crashClassCheck, gbc);

        gbc.gridy = 8;
        this.generateMappingsCheck = new JCheckBox("Generate Mappings");
        this.add(this.generateMappingsCheck, gbc);

        gbc.gridy = 9;
        gbc.gridwidth = 1;
        this.add(new JLabel("Flow Obfuscation:"), gbc);
        gbc.gridx = 1;
        this.flowObfuscationCombo = new JComboBox<>(new String[] {"NONE", "EASY", "HEAVY"});
        this.add(this.flowObfuscationCombo, gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 1;

        gbc.gridy = 10;
        this.add(new JLabel("Watermark:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.watermarkField = new JTextField(20);
        this.watermarkField.setToolTipText("Prefix for obfuscated names (e.g., TEST_)");
        this.add(this.watermarkField, gbc);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
    }

    public void updateFromConfig(final JsonConfig config) {
        this.obfuscateClassNamesCheck.setSelected(config.isObfuscateClassNames());
        this.obfuscatePackagesCheck.setSelected(config.isObfuscatePackages());
        this.obfuscateVariablesCheck.setSelected(config.isObfuscateVariables());
        this.obfuscateStringsCheck.setSelected(config.isObfuscateStrings());
        this.obfuscateMethodNamesCheck.setSelected(config.isObfuscateMethodNames());
        this.changeMethodsOrdersCheck.setSelected(config.isChangeMethodsOrders());
        this.crashClassCheck.setSelected(config.isCrashClass());
        this.generateMappingsCheck.setSelected(config.isGenerateMappings());
        String flowObf = config.getFlowObfuscation();
        if (flowObf == null || flowObf.isEmpty()) {
            flowObf = "NONE";
        }
        this.flowObfuscationCombo.setSelectedItem(flowObf);
        final String watermark = config.getWatermark();
        this.watermarkField.setText(watermark != null ? watermark : "");
    }

    public void updateConfig(final JsonConfig config) {
        config.setObfuscateClassNames(this.obfuscateClassNamesCheck.isSelected());
        config.setObfuscatePackages(this.obfuscatePackagesCheck.isSelected());
        config.setObfuscateVariables(this.obfuscateVariablesCheck.isSelected());
        config.setObfuscateStrings(this.obfuscateStringsCheck.isSelected());
        config.setObfuscateMethodNames(this.obfuscateMethodNamesCheck.isSelected());
        config.setChangeMethodsOrders(this.changeMethodsOrdersCheck.isSelected());
        config.setCrashClass(this.crashClassCheck.isSelected());
        config.setGenerateMappings(this.generateMappingsCheck.isSelected());
        config.setFlowObfuscation((String) this.flowObfuscationCombo.getSelectedItem());
        config.setWatermark(this.watermarkField.getText());
    }
}
