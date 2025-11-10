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

import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JPanel;
import lombok.Getter;

@Getter
public class ButtonPanel extends JPanel {

    private JButton loadButton;
    private JButton saveButton;
    private JButton startObfuscationButton;

    public ButtonPanel() {
        this.initializePanel();
    }

    private void initializePanel() {
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        this.loadButton = new JButton("Load Config");
        this.add(this.loadButton);
        this.saveButton = new JButton("Save Config");
        this.add(this.saveButton);
        this.startObfuscationButton = new JButton("Start Obfuscation");
        this.startObfuscationButton.setFont(this.startObfuscationButton.getFont().deriveFont(Font.BOLD));
        this.add(this.startObfuscationButton);
    }

    public void setStartButtonEnabled(final boolean enabled) {
        this.startObfuscationButton.setEnabled(enabled);
    }

    public void setStartButtonText(final String text) {
        this.startObfuscationButton.setText(text);
    }
}
