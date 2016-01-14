package org.apache.pluto.container.om.portlet.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

import javax.portlet.annotations.PortletApplication;
import javax.portlet.annotations.PortletConfiguration;
import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.pluto.container.bean.processor.AnnotatedMethodStore;
import org.apache.pluto.container.om.portlet.PortletApplicationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class ConfigurationProcessor {
   
   
   /** Logger. */
   private static final Logger LOG = LoggerFactory
         .getLogger(ConfigurationProcessor.class);


   protected PortletApplicationDefinition pad;
   
   public ConfigurationProcessor(PortletApplicationDefinition pad) {
      this.pad = pad;
   }

   public PortletApplicationDefinition getPad() {
      return pad;
   }

   /**
    * Traverses the portlet deployment descriptor tree and returns the data in
    * the form of a portlet application definition.
    * 
    * @param rootElement
    *           Root element of portlet DD tree
    * @return The parsed portlet application definition
    * @throws IllegalArgumentException
    *            If there is a data validation error
    */
   public abstract void process(JAXBElement<?> rootElement) throws IllegalArgumentException;

   /**
    * Validates the given portlet application definition. This method should only be called after 
    * the complete configuration has been read.
    * <p>
    * The validation method is designed to be called within the portlet application servlet context.
    * It throws exceptions when specified classes cannot be loaded or other severe configuration
    * problem is discovered. It logs warnings for less severe configuration problems.
    * <p>
    * The validation code is separate from the 
    * configuration reading code so that the config reading code won't cause exceptions when it 
    * is used by the maven-portlet-plugin packaging code. 
    * 
    * @throws IllegalArgumentException
    *             If there is a validation error.
    */
   public abstract void validate() throws IllegalArgumentException;
   
   /**
    * reconciles the given annotated method store containing the bean configuration
    * with the configuration as read from the portlet deployment descriptor and 
    * the corresponding type annotations.
    * <p>
    * Portlets that are defined in the bean config are added to the portlet application
    * definition if not already present. Event reference information from the 
    * annotations is verified and added to the corresponding portlet definition.
    * <p>
    * Methods from portlet classes definied in the portlet definitions are
    * added to the annotated method store.
    * 
    * @param ams
    */
   public void reconcileBeanConfig(AnnotatedMethodStore ams) {
      // do nothing for JSR 168 & JSR 286 portlets
   }

   /**
    * Handle the locale the old-fashioned way (v1 & v2)
    */
   protected Locale deriveLocale(String lang) {
      Locale locale = Locale.ENGLISH;
      if (lang != null) {
         if (lang.contains("_") == true) {

            // tolerate underscores to support old portlets
            String[] parts = lang.split("_");
            if (parts.length == 2) {
               locale = new Locale(parts[0], parts[1]);
            } else {
               locale = new Locale(parts[0], parts[1], parts[2]);
            }

         } else {
            locale = Locale.forLanguageTag(lang);     //BCP47
         }
      }
      return locale;
   }

   /**
    * Check if input string is valid java identifier
    * 
    * @param id
    * @return
    */
   protected boolean isValidIdentifier(String id) {
      if (id == null || id.length() == 0) {
         return false;
      }
      char[] chars = id.toCharArray();
      if (!Character.isJavaIdentifierStart(chars[0])) {
         return false;
      }
      for (char c : Arrays.copyOfRange(chars, 1, chars.length)) {
         if (!Character.isJavaIdentifierPart(c) && (c != '.')) {
            return false;
         }
      }
      return true;
   }

   /**
    * checks if class name is valid by trying to load it. If the optional
    * argument <code>assignable</code> is provided, the method will check if the
    * class can be assigned.
    * 
    * @param clsName
    *           Class name string from configuration
    * @param assignable
    *           Interface to which the class should be assignable
    * @param msg
    *           Error message used when exception is thrown.
    */
   protected void checkValidClass(String clsName, Class<?> assignable, String msg) {
   
      StringBuilder txt = new StringBuilder(128);
      txt.append(msg).append(", class name: ");
      txt.append(clsName);
      if (!isValidIdentifier(clsName)) {
         txt.append(". Invalid java identifier.");
         LOG.warn(txt.toString());
         throw new IllegalArgumentException(txt.toString());
      }
   
      // Make sure the class can be loaded
      Class<?> valClass = null;
      try {
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         if (cl == null) {
            cl = this.getClass().getClassLoader();
         }
         valClass = cl.loadClass(clsName);
         if (assignable != null && !assignable.isAssignableFrom(valClass)) {
            txt.append(". Specified class is not a ");
            txt.append(assignable.getCanonicalName());
            throw new Exception();
         }
      } catch (Exception e) {
         LOG.warn(txt.toString());
         throw new IllegalArgumentException(txt.toString(), e);
      }
   }

   /**
    * checks if resource bundle name is valid by trying to load it. 
    * 
    * @param bundleName
    *           Class name string from configuration
    */
   protected void checkValidBundle(String bundleName) {
   
      StringBuilder txt = new StringBuilder(128);
      txt.append("Bad resource bundle: ");
      txt.append(bundleName);
      if (!isValidIdentifier(bundleName)) {
         txt.append(". Invalid java identifier.");
         LOG.warn(txt.toString());
         throw new IllegalArgumentException(txt.toString());
      }
   
      try {
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         if (cl == null) {
            cl = this.getClass().getClassLoader();
         }
         @SuppressWarnings("unused")
         ResourceBundle rb = ResourceBundle.getBundle(bundleName, Locale.getDefault(), cl); 
      } catch (Exception e) {
         LOG.warn(txt.toString());
         throw new IllegalArgumentException(txt.toString(), e);
      }
   }
   
   /**
    * Generates a unique name for use in cases where the item is ordered by name, but the name 
    * is optional to from the point of view of the portlet developer. For example, the filter name
    * need not be specified in the filter annotation, but if it is, the filter config can be
    * modified through a corresponding specification in the portlet deployment descriptor.
    * 
    * @return
    */
   protected String genUniqueName() {
     
      // create random name
      final String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZüÜäÄöÖß";
      StringBuilder txt = new StringBuilder(128);
      txt.append("Generated:");
      Random rand = new Random();
      for (int ii = 0; ii < 32; ii++) {
         txt.append(chars.charAt(rand.nextInt(chars.length())));
      }
      return txt.toString();

   }
   
   /**
    * Reads web app deployment descriptor to extract the locale - encoding mappings 
    * 
    * @param in            Input stream for DD
    * @throws Exception    If there is a parsing problem
    */
   public void processWebDD(InputStream in) throws Exception {

      // set up document
      DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = fact.newDocumentBuilder();
      final Document document = builder.parse(in);
      final Element root = document.getDocumentElement();

      // Generate xpath queries
      final XPathFactory xpathFactory = XPathFactory.newInstance();
      final XPath xpath = xpathFactory.newXPath();
      final XPathExpression GET_LIST = 
            xpath.compile("//locale-encoding-mapping-list/locale-encoding-mapping");
      final XPathExpression GET_LOC = xpath.compile("locale/text()");
      final XPathExpression GET_ENC = xpath.compile("encoding/text()");

      // get list of locale - encoding mappings and process them
      NodeList nodes = (NodeList) GET_LIST.evaluate(root,
            XPathConstants.NODESET);

      for (int jj = 0; jj < nodes.getLength(); jj++) {
         Node node = nodes.item(jj);
         String locstr = (String) GET_LOC.evaluate(node, XPathConstants.STRING);
         String encstr = (String) GET_ENC.evaluate(node, XPathConstants.STRING);
         Locale locale = deriveLocale(locstr);
         pad.addLocaleEncodingMapping(locale, encstr);
      }
   }
   
   /**
    * Extracts the data from the portlet application annotation and adds it to the 
    * portlet application definition structure.
    * <p>
    * The default method implementation does nothing. The V3 implementation will
    * override this method to provide function.  
    * <p>
    * This method is designed to be called before the portlet deployment descriptor
    * is read so that data from the portlet DD can override that provided through annotations.
    * 
    * @param pa      The portlet application annotation
    */
   public void processPortletAppAnnotation(PortletApplication pa) {
      // default impl = do nothing
   }
   
   /**
    * Extracts the data from the portlet annotation and adds it to a 
    * portlet definition structure. The portlet definition will be created if it does not
    * already exist.
    * <p>
    * The default method implementation does nothing. The V3 implementation will
    * override this method to provide function.  
    * <p>
    * This method is designed to be called before the portlet deployment descriptor
    * is read so that data from the portlet DD can override that provided through annotations.
    * 
    * @param pc   The portlet configuration annotation
    * @param cls  The annotated class
    */
   public void processPortletConfigAnnotation(PortletConfiguration pc, Class<?> cls) {
      // default impl = do nothing
   }

   
   /**
    * Extracts the data from the portlet annotation and adds it to a 
    * portlet filter definition structure. The portlet filter definition will be created if it does not
    * already exist.
    * <p>
    * The default method implementation does nothing. The V3 implementation will
    * override this method to provide function.  
    * <p>
    * This method is designed to be called before the portlet deployment descriptor
    * is read so that data from the portlet DD can override that provided through annotations.
    * 
    * @param cls     The annotated class. 
    */
   public void processPortletFilterAnnotation(Class<?> cls) {
      // default impl = do nothing
   }

   /**
    * Extracts the data from the portlet annotation and adds it to a 
    * portlet listener definition structure. The portlet listener definition will be created if it does not
    * already exist.
    * <p>
    * The default method implementation does nothing. The V3 implementation will
    * override this method to provide function.  
    * <p>
    * This method is designed to be called before the portlet deployment descriptor
    * is read so that data from the portlet DD can override that provided through annotations.
    * 
    * @param cls
    */
   public void processListenerAnnotation(Class<?> cls) {
   }

   /**
    * Processes PortletPreferencesValidator annotated classes.
    * 
    * @param cls
    */
   public void processValidatorAnnotation(Class<?> cls) {
   }

}