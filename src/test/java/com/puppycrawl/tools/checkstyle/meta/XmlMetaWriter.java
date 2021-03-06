////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2020 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.meta;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class XmlMetaWriter {
    private static final Pattern FILEPATH_CONVERSION = Pattern.compile("\\.");

    /**
     * Do no allow {@code XmlMetaWriter} instances to be created.
     */
    private XmlMetaWriter() {
    }

    public static void write(ModuleDetails moduleDetails) throws TransformerException,
            ParserConfigurationException {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document doc = dBuilder.newDocument();

        final Element rootElement = doc.createElement("checkstyle-metadata");
        final Element rootChild = doc.createElement("module");
        rootElement.appendChild(rootChild);

        doc.appendChild(rootElement);

        final Element checkModule = doc.createElement(moduleDetails.getModuleType().getLabel());
        rootChild.appendChild(checkModule);

        checkModule.setAttribute("name", moduleDetails.getName());
        checkModule.setAttribute("fully-qualified-name",
                moduleDetails.getFullQualifiedName());
        checkModule.setAttribute("parent", moduleDetails.getParent());

        final Element desc = doc.createElement("description");
        final Node cdataDesc = doc.createCDATASection(moduleDetails.getDescription());
        desc.appendChild(cdataDesc);
        checkModule.appendChild(desc);
        createPropertySection(moduleDetails, checkModule, doc);
        if (!moduleDetails.getViolationMessageKeys().isEmpty()) {
            final Element messageKeys = doc.createElement("message-keys");
            for (String msg : moduleDetails.getViolationMessageKeys()) {
                final Element messageKey = doc.createElement("message-key");
                messageKey.setAttribute("key", msg);
                messageKeys.appendChild(messageKey);
            }
            checkModule.appendChild(messageKeys);
        }

        writeToFile(doc, moduleDetails);
    }

    private static void createPropertySection(ModuleDetails moduleDetails, Element checkModule,
                                              Document doc) {
        if (!moduleDetails.getProperties().isEmpty()) {
            final Element properties = doc.createElement("properties");
            checkModule.appendChild(properties);
            for (ModulePropertyDetails modulePropertyDetails : moduleDetails.getProperties()) {
                final Element property = doc.createElement("property");
                properties.appendChild(property);
                property.setAttribute("name", modulePropertyDetails.getName());
                property.setAttribute("type", modulePropertyDetails.getType());
                if (modulePropertyDetails.getDefaultValue() != null) {
                    property.setAttribute("default-value",
                            modulePropertyDetails.getDefaultValue());
                }
                if (modulePropertyDetails.getValidationType() != null) {
                    property.setAttribute("validation-type",
                            modulePropertyDetails.getValidationType());
                }
                final Element propertyDesc = doc.createElement("description");
                propertyDesc.appendChild(doc.createCDATASection(
                        modulePropertyDetails.getDescription()));
                property.appendChild(propertyDesc);
            }
        }
    }

    private static void writeToFile(Document document, ModuleDetails moduleDetails)
            throws TransformerException {
        final String rootOutputPath = System.getProperty("user.dir") + "/src/main/resources";
        String fileSeparator = "/";
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
            fileSeparator = "\\" + fileSeparator;
        }
        final String modifiedPath;
        if (moduleDetails.getFullQualifiedName().startsWith("com.puppycrawl.tools.checkstyle")) {
            final String moduleFilePath = FILEPATH_CONVERSION
                    .matcher(moduleDetails.getFullQualifiedName())
                    .replaceAll(fileSeparator);
            final int indexOfCheckstyle =
                    moduleFilePath.indexOf("checkstyle") + "checkstyle".length();

            modifiedPath = rootOutputPath + "/" + moduleFilePath.substring(0, indexOfCheckstyle)
                    + "/meta/" + moduleFilePath.substring(indexOfCheckstyle + 1) + ".xml";
        }
        else {
            String moduleName = moduleDetails.getName();
            if (moduleDetails.getModuleType() == ModuleType.CHECK) {
                moduleName += "Check";
            }
            modifiedPath = rootOutputPath + "/checkstylemeta-" + moduleName + ".xml";
        }
        if (!moduleDetails.getDescription().isEmpty()) {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            final DOMSource source = new DOMSource(document);
            final StreamResult result = new StreamResult(new File(modifiedPath));
            transformer.transform(source, result);
        }
    }
}

