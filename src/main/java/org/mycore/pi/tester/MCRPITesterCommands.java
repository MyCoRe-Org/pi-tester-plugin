package org.mycore.pi.tester;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIMetadataService;
import org.mycore.pi.MCRPIParser;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

import static org.mycore.pi.MCRPIService.GENERATOR_CONFIG_PREFIX;
import static org.mycore.pi.MCRPIService.METADATA_SERVICE_CONFIG_PREFIX;

@MCRCommandGroup(name = "MyCoRe PI test commands")
public class MCRPITesterCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(
        syntax = "test generator {0} with object {1}",
        help = "test the generator with the id {0} and with the object {1} and prints the generated identifier!")
    public static void testGenerator(String generatorString, String objectIDString)
        throws MCRPersistentIdentifierException {
        testGenerator(generatorString, objectIDString, null);
    }

    @MCRCommand(
        syntax = "test generator {0} with object {1} and additional {2}",
        help = "test the generator with the id {0} and with the object {1} and additional {2} and prints the generated identifier!")
    public static void testGenerator(String generatorID, String objectIDString, String additional)
        throws MCRPersistentIdentifierException {
        final MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        final MCRBase mcrObject = MCRMetadataManager.retrieve(objectID);

        final MCRPIGenerator<MCRPersistentIdentifier> generator = getGenerator(generatorID);
        final String generatedPI = generator.generate(mcrObject, additional).asString();
        LOGGER.info("The generated Identifier ist: {}", generatedPI);
    }

    @MCRCommand(
        syntax = "test metadata service {0} with object {1} and identifier {2}",
        help = "test the metadata service with the id {0} and with the object {1} and identifier {2} and prints test informations!")
    public static void testMetadataService(String metadataServiceID, String objectIDString, String identifier)
        throws MCRPersistentIdentifierException {
        final MCRObjectID objectID = MCRObjectID.getInstance(objectIDString);
        final MCRBase mcrObject = MCRMetadataManager.retrieve(objectID);

        final MCRPIMetadataService<MCRPersistentIdentifier> metadataService = getMetadataService(metadataServiceID);

        final MCRPersistentIdentifier pi = MCRPIManager.getInstance().get(identifier).findFirst()
            .orElseThrow(() -> new MCRException(identifier + " is not a valid identifier!"));

        LOGGER.info("Object: \n {}", getMCRObjectAsString(mcrObject));
        metadataService.insertIdentifier(pi, mcrObject, null);
        LOGGER.info("Object with PI: \n {}", getMCRObjectAsString(mcrObject));
        final String readIdentifier = metadataService.getIdentifier(mcrObject, null)
            .map(MCRPersistentIdentifier::asString).orElse("<EMPTY>");
        LOGGER.info("Identifier read with MetadataService: {}", readIdentifier);
        metadataService.removeIdentifier(pi, mcrObject, null);
        LOGGER.info("Object with removed PI: \n {}", getMCRObjectAsString(mcrObject));

    }

    private static String getMCRObjectAsString(MCRBase mcrObject) {
        final Document xml = mcrObject.createXML();
        return new XMLOutputter(Format.getPrettyFormat()).outputString(xml);
    }

    private static MCRPIGenerator<MCRPersistentIdentifier> getGenerator(String generator) {
        String generatorPropertyKey = GENERATOR_CONFIG_PREFIX + generator;
        String className = MCRConfiguration.instance().getString(generatorPropertyKey);

        try {
            @SuppressWarnings("unchecked")
            Class<MCRPIGenerator<MCRPersistentIdentifier>> classObject = (Class<MCRPIGenerator<MCRPersistentIdentifier>>) Class
                .forName(className);
            final Constructor<MCRPIGenerator<MCRPersistentIdentifier>> constructor = classObject
                .getConstructor(String.class);
            return constructor.newInstance(generator);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + generatorPropertyKey + ") not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + generatorPropertyKey + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    private static MCRPIMetadataService<MCRPersistentIdentifier> getMetadataService(String metadataManager) {
        final String className = MCRConfiguration.instance()
            .getString(METADATA_SERVICE_CONFIG_PREFIX + metadataManager);
        try {
            @SuppressWarnings("unchecked")
            Class<MCRPIMetadataService<MCRPersistentIdentifier>> classObject = (Class<MCRPIMetadataService<MCRPersistentIdentifier>>) Class
                .forName(className);
            Constructor<MCRPIMetadataService<MCRPersistentIdentifier>> constructor = classObject
                .getConstructor(String.class);
            return constructor.newInstance(metadataManager);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (METADATA_SERVICE_CONFIG_PREFIX + metadataManager) + ") not found: "
                    + className, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + (METADATA_SERVICE_CONFIG_PREFIX + metadataManager)
                    + ") needs a string constructor: " + className);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }
}
