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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import ovh.neziw.obfuscator.config.JsonConfig;

public class IncludeListPanel extends JPanel {

    private JTextArea includeArea;

    public IncludeListPanel() {
        this.initializePanel();
    }

    private void initializePanel() {
        this.setLayout(new BorderLayout(5, 5));
        this.add(new JLabel("Include (packages/classes, one per line):"), BorderLayout.NORTH);
        this.includeArea = new JTextArea(5, 30);
        this.includeArea.setLineWrap(true);
        this.includeArea.setWrapStyleWord(true);
        final JScrollPane includeScroll = new JScrollPane(this.includeArea);
        this.add(includeScroll, BorderLayout.CENTER);
    }

    public void updateFromConfig(final JsonConfig config) {
        final StringBuilder includeText = new StringBuilder();
        for (final String item : config.getInclude()) {
            includeText.append(item).append("\n");
        }
        this.includeArea.setText(includeText.toString());
    }

    public void updateConfig(final JsonConfig config) {
        final String includeText = this.includeArea.getText();
        final List<String> includeList = new ArrayList<>();
        if (!includeText.trim().isEmpty()) {
            final String[] lines = includeText.split("\n");
            for (final String line : lines) {
                final String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    includeList.add(trimmed);
                }
            }
        }
        config.setInclude(includeList);
    }
}
