package org.netbeans.mvc.basic.hyperlink;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.openide.util.Exceptions;
@MimeRegistration(mimeType = "text/x-java", service = HyperlinkProvider.class)
public class AbstractHyperlinkProviderImpl extends AbstractHyperlinkProvider {
    public AbstractHyperlinkProviderImpl() {
        super("return .*.xhtml\";");
    }
    @Override
    public void performClickAction(Document document, int i) {
        try {
            JOptionPane.showMessageDialog(null, document.getText(i, 15));
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
