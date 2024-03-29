package com.sampullara;

import java.io.File;
import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

public class DependsAgent {
    private static String m2Directory = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent installed");
        instrumentation = inst;
        inst.addTransformer(new IdentityTransformer());
    }

    private static class IdentityTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            ClassFile cf = ClassFile.of();
            ClassModel cm = cf.parse(classfileBuffer);
            return cf.build(cm.thisClass().asSymbol(), classBuilder -> {
                for (ClassElement ce : cm) {
                    classBuilder.with(ce);
                    if (ce instanceof RuntimeVisibleAnnotationsAttribute ann) {
                        for (Annotation annotation : ann.annotations()) {
                            if (annotation.className().stringValue().equals("Lcom/sampullara/Dependency;")) {
                                String groupId = null;
                                String artifactId = null;
                                String version = null;
                                for (AnnotationElement element : annotation.elements()) {
                                    if (element.name().stringValue().equals("groupid")) {
                                        groupId = ((AnnotationValue.OfString) element.value()).stringValue();
                                    } else if (element.name().stringValue().equals("artifactid")) {
                                        artifactId = ((AnnotationValue.OfString) element.value()).stringValue();
                                    } else if (element.name().stringValue().equals("version")) {
                                        version = ((AnnotationValue.OfString) element.value()).stringValue();
                                    }
                                }
                                if (groupId != null && artifactId != null && version != null) {
                                    try {
                                        URL url = URI.create("https://repo1.maven.org/maven2/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar").toURL();
                                        File m2DirectoryFile = new File(m2Directory); // Ensure the base .m2 directory exists
                                        m2DirectoryFile.mkdirs();

                                        String destinationPath = m2Directory + File.separator +
                                                groupId.replace('.', '/') + File.separator +
                                                artifactId + File.separator +
                                                version + File.separator +
                                                artifactId + "-" + version + ".jar";
                                        var destinationFile = new File(destinationPath);
                                        if (!destinationFile.exists()) {
                                            System.out.println("Installing: " + destinationFile);
                                            destinationFile.getParentFile().mkdirs(); // Create necessary parent directories
                                            Files.copy(url.openStream(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                        }
                                        System.out.println("Loaded: " + destinationFile);
                                        instrumentation.appendToSystemClassLoaderSearch(new JarFile(destinationFile));
                                    } catch (IOException e) {
                                        // This should never happen
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
