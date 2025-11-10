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
package ovh.neziw.obfuscator;

import com.formdev.flatlaf.FlatLightLaf;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import ovh.neziw.obfuscator.gui.ObfuscatorGui;

public final class ObfuscatorMain {

    private static final Logger LOGGER = Logger.getLogger(ObfuscatorMain.class.getName());

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (final Exception exception) {
                LOGGER.severe("Failed to setup FlatLightLaf: " + exception.getMessage());
                LOGGER.throwing(ObfuscatorMain.class.getName(), "main", exception);
            }
            new ObfuscatorGui().setVisible(true);
        });
    }
}
