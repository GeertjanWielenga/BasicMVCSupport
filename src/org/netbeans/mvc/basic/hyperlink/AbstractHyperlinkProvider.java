package org.netbeans.mvc.basic.hyperlink;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.openide.util.Exceptions;
public abstract class AbstractHyperlinkProvider implements HyperlinkProvider {
    private transient int startOffset, endOffset;
    private transient final String identifier;
    @Override
    public abstract void performClickAction(Document document, int i);
    public AbstractHyperlinkProvider(String identifier) {
        this.identifier = identifier;
    }
    @Override
    public boolean isHyperlinkPoint(Document document, int offset) {
        boolean result = false;
        Matcher matcher = null;
        try {
            matcher = Pattern.compile(identifier).matcher(document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        while (matcher.find()) {
            startOffset = matcher.start();
            endOffset = matcher.end();
            result = true;
            break;
        }
        return result;
    }
    @Override
    public int[] getHyperlinkSpan(Document document, int i) {
        int[] result = null;
        if ((StyledDocument) EditorRegistry.lastFocusedComponent().getDocument() != null) {
            result = new int[]{startOffset, endOffset};
        }
        return result;
    }
}
