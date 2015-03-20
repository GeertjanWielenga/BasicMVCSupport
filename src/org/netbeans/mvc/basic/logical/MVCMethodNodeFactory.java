package org.netbeans.mvc.basic.logical;

import com.sun.faces.action.RequestMapping;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.BeanInfo;
import java.io.IOException;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.text.StyledDocument;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.netbeans.api.java.source.Task;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.NbDocument;

@NodeFactory.Registration(position = 50, projectType = "org-netbeans-modules-maven")
public class MVCMethodNodeFactory implements NodeFactory {

    private static final Image REST_SERVICES_BADGE = ImageUtilities.loadImage("org/netbeans/modules/websvc/rest/nodes/resources/restservices.png", true); // NOI18N

    @Override
    public NodeList<?> createNodes(Project project) {
        AbstractNode nd = new AbstractNode(Children.create(new MVCMethodChildFactory(project), true)) {
            @Override
            public Image getIcon(int type) {
                return computeIcon(true, type);
            }

            @Override
            public Image getOpenedIcon(int type) {
                return computeIcon(true, type);
            }
        };
        nd.setDisplayName("MCV Methods");
        return NodeFactorySupport.fixedNodeList(nd);
    }

    private Image computeIcon(boolean opened, int type) {
        Node n = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
        ImageIcon icon = new ImageIcon(n.getOpenedIcon(BeanInfo.ICON_COLOR_16x16));
        Image image = ((ImageIcon) icon).getImage();
        image = ImageUtilities.mergeImages(image, REST_SERVICES_BADGE, 7, 7);
        return image;
    }

    private class ParseFileIdentifyMethodsBuildLogicalView extends TreePathScanner<Void, Void> {

        private final List list;
        private CompilationInfo info;

        public ParseFileIdentifyMethodsBuildLogicalView(List list, CompilationInfo info) {
            this.list = list;
            this.info = info;
        }

        @Override
        public Void visitClass(ClassTree t, Void v) {
            Element el = info.getTrees().getElement(getCurrentPath());
            if (el != null) {
                TypeElement te = (TypeElement) el;
                List<? extends Element> enclosedElements = te.getEnclosedElements();
                for (int i = 0; i < enclosedElements.size(); i++) {
                    Element enclosedElement = (Element) enclosedElements.get(i);
                    if (enclosedElement.getAnnotation(RequestMapping.class) != null) {
                        list.add(enclosedElement.getSimpleName());
                    }
                }
            }
            return null;
        }
    }

    private class ParseFileIdentifyLineOpenFile extends TreePathScanner<Void, Void> {

        private final FileObject fo;

        private String method;
        private CompilationInfo info;

        public ParseFileIdentifyLineOpenFile(FileObject fo, String method, CompilationInfo info) {
            this.fo = fo;
            this.method = method;
            this.info = info;
        }

        @Override
        public Void visitClass(ClassTree t, Void v) {
            Element el = info.getTrees().getElement(getCurrentPath());
            if (el != null) {
                TypeElement te = (TypeElement) el;
                List<? extends Element> enclosedElements = te.getEnclosedElements();
                SourcePositions sp = info.getTrees().getSourcePositions();
                for (int i = 0; i < enclosedElements.size(); i++) {
                    try {
                        Element enclosedElement = (Element) enclosedElements.get(i);
                        int start = (int) sp.getStartPosition(info.getCompilationUnit(), info.getTrees().getTree(enclosedElement));
                        DataObject dobj = DataObject.find(fo);
                        EditorCookie ec = dobj.getLookup().lookup(EditorCookie.class);
                        final StyledDocument doc = ec.openDocument();
                        int startLine = NbDocument.findLineNumber(doc, start);
                        if (enclosedElement.getSimpleName().toString().equals(method)) {
                            try {
                                LineCookie lc = DataObject.find(fo).getLookup().lookup(LineCookie.class);
                                Line line = lc.getLineSet().getOriginal(startLine);
                                line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FRONT);
                            } catch (DataObjectNotFoundException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
//                    }
                }
            }

            return null;
        }
    }

    private class MVCMethodChildFactory extends ChildFactory<FileObject> {

        private final Project project;

        private MVCMethodChildFactory(Project project) {
            this.project = project;
        }

        @Override
        protected boolean createKeys(List<FileObject> list) {
            Sources sources = ProjectUtils.getSources(project);
            SourceGroup[] groups = sources.getSourceGroups(Sources.TYPE_GENERIC);
            for (SourceGroup group : groups) {
                for (FileObject fo : group.getRootFolder().getChildren()) {
                    if (fo.getName().equals("src")) {
                        FileObject srcFo = fo;
                        for (FileObject oneSrcChild : srcFo.getChildren()) {
                            javaclassFinder(oneSrcChild, list);
                        }
                    }
                }
            }
            return true;
        }

        private void javaclassFinder(FileObject fo, List<FileObject> list) {
            if (fo.getMIMEType().equals("text/x-java")) {
                list.add(fo);
            } else {
                for (FileObject fo2 : fo.getChildren()) {
                    javaclassFinder(fo2, list);
                }
            }
        }

        @Override
        protected Node createNodeForKey(FileObject key) {
            AbstractNode node = new AbstractNode(Children.create(new OneMVCMethodChildFactory(key), true));
            node.setIconBaseWithExtension("org/netbeans/mvc/basic/logical/restservice.png");
            node.setDisplayName(key.getName());
            return node;
        }

        private class OneMVCMethodChildFactory extends ChildFactory<Object> {

            private final FileObject fileObject;

            public OneMVCMethodChildFactory(FileObject key) {
                this.fileObject = key;
                fileObject.addFileChangeListener(new FileChangeAdapter() {
                    @Override
                    public void fileChanged(FileEvent fe) {
                        refresh(true);
                    }
                });
            }

            @Override
            protected boolean createKeys(final List<Object> list) {
                JavaSource javaSource = JavaSource.forFileObject(fileObject);
                if (javaSource != null) {
                    try {
                        javaSource.runUserActionTask(new Task<CompilationController>() {
                            @Override
                            public void run(CompilationController compilationController) throws Exception {
                                compilationController.toPhase(Phase.ELEMENTS_RESOLVED);
                                new ParseFileIdentifyMethodsBuildLogicalView(list, compilationController).scan(compilationController.getCompilationUnit(), null);
                            }
                        }, true);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                return true;
            }

            @Override
            protected Node createNodeForKey(final Object key) {
                AbstractNode node = new AbstractNode(Children.LEAF) {
                    @Override
                    public Action getPreferredAction() {
                        return OpenFileAtMethodAction();
                    }

                    @Override
                    public Action[] getActions(boolean context) {
                        return new Action[]{OpenFileAtMethodAction(), new AbstractAction("Test Method Uri") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            }
                        }};
                    }

                    private Action OpenFileAtMethodAction() {
                        return new AbstractAction("Open") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                JavaSource javaSource = JavaSource.forFileObject(fileObject);
                                if (javaSource != null) {
                                    try {
                                        javaSource.runUserActionTask(new Task<CompilationController>() {
                                            @Override
                                            public void run(CompilationController compilationController) throws Exception {
                                                compilationController.toPhase(Phase.ELEMENTS_RESOLVED);
                                                new ParseFileIdentifyLineOpenFile(fileObject, key.toString(), compilationController).scan(compilationController.getCompilationUnit(), null);
                                            }
                                        }, true);
                                    } catch (IOException ex) {
                                        Exceptions.printStackTrace(ex);
                                    }
                                }
                            }
                        };
                    }
                };
                node.setDisplayName(key.toString());
                node.setIconBaseWithExtension("org/netbeans/mvc/basic/logical/method.png");
                return node;
            }
        }
    }

}
