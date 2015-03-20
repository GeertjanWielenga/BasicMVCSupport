package org.netbeans.mvc.basic;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.maven.api.archetype.Archetype;
import org.netbeans.modules.maven.api.archetype.ArchetypeWizards;
import org.netbeans.modules.maven.api.archetype.ProjectInfo;
import org.netbeans.modules.maven.j2ee.ui.wizard.BaseWizardIterator;
import org.netbeans.modules.maven.j2ee.ui.wizard.archetype.J2eeArchetypeFactory;
import org.netbeans.modules.maven.j2ee.utils.MavenProjectSupport;
import org.netbeans.validation.api.ui.ValidationGroup;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.templates.TemplateRegistration;
import org.openide.util.NbBundle;

public class JavaEEMVCWizardIterator extends BaseWizardIterator {

    public static final String PROP_EE_LEVEL = "eeLevel"; // NOI18N
    private J2eeModule.Type projectType;

    private JavaEEMVCWizardIterator(J2eeModule.Type projectType) {
        this.projectType = projectType;
    }

    @NbBundle.Messages("template.MVC=MVC 1.0 Application")
    @TemplateRegistration(
            folder = ArchetypeWizards.TEMPLATE_FOLDER,
            position = 0,
            displayName = "#template.MVC",
            iconBase = "org/netbeans/mvc/basic/manfred.jpg",
            description = "BasicMVCDescription.html"
    )
    public static JavaEEMVCWizardIterator createWebAppIterator() {
        return new JavaEEMVCWizardIterator(J2eeModule.Type.WAR);
    }

    @Override
    protected WizardDescriptor.Panel[] createPanels(ValidationGroup vg) {
        return new WizardDescriptor.Panel[]{
            ArchetypeWizards.basicWizardPanel(vg, false, null),
            new EELevelPanel(projectType),
            new MVCSettingsWizardPanel()
        };
    }

    @Override
    public Set<FileObject> instantiate() throws IOException {
        ProjectInfo vi = new ProjectInfo((String) 
                wiz.getProperty("groupId"), 
                (String) wiz.getProperty("artifactId"), 
                (String) wiz.getProperty("version"), 
                (String) wiz.getProperty("package")); //NOI18N
        Profile profile = (Profile) wiz.getProperty(PROP_EE_LEVEL);
        Archetype archetype = J2eeArchetypeFactory.getInstance().findArchetypeFor(projectType, profile);
        ArchetypeWizards.logUsage(archetype.getGroupId(), archetype.getArtifactId(), archetype.getVersion());
        File rootFile = FileUtil.normalizeFile((File) wiz.getProperty("projdir")); // NOI18N
        ArchetypeWizards.createFromArchetype(rootFile, vi, archetype, null, true);
        FileUtil.toFileObject(rootFile).createData("test","html");
        Set<FileObject> projects = ArchetypeWizards.openProjects(rootFile, rootFile);
        for (FileObject projectFile : projects) {
            Project project = ProjectManager.getDefault().findProject(projectFile);
            if (project == null) {
                continue;
            }
            saveSettingsToNbConfiguration(project);
            MavenProjectSupport.changeServer(project, true);
        }
        return projects;
    }

}
