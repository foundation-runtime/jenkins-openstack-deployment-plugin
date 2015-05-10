package control_plane_ci;


//import org.jenkinsci.plugins.os_ci.openstack.ParametersUtils;

import org.jenkinsci.plugins.os_ci.repohandlers.NexusClient;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.Writer;


/**
 * Created by agrosmar on 12/10/2014.
 */
public class Test {

    private NexusClient nexusClient;

    public static void main(String[] args) throws Exception {

//        InvocationRequest request = new DefaultInvocationRequest();
//        request.setPomFile( new File( "X:\\pom.xml" ) );
//        request.setGoals( Arrays.asList( "package" ) );
//        Invoker invoker = new DefaultInvoker();
//        invoker.execute( request );




        DocumentBuilderFactory configuration = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = configuration.newDocumentBuilder();

        Document doc = builder.newDocument();

        // create the root element node

        Element element = doc.createElement("Configuration");
        /*      private String name;
        private String autoRequires;
        private DefineStatements defineStatements;
        private String artifactId;
        private String group;
        private String packager;
        private Mapping mapping;*/

        doc.appendChild(element);

        // create a comment node given the specified string

        Comment comment = doc.createComment("This is a comment");

        doc.insertBefore(comment, element);

        // add element after the first child of the root element

        Element itema = doc.createElement("name");
        Element itemb= doc.createElement("groupId");
        Element itemc = doc.createElement("autoRequires");
        Element itemd = doc.createElement("defineStatements");
        itemd.insertBefore(doc.createElement("defineStatements"),itemd.getLastChild());
        Element iteme = doc.createElement("packager");
        element.appendChild(itema);
        element.appendChild(itemb);
        element.appendChild(itemc);
        element.appendChild(itemd);
        element.appendChild(iteme);
//        DefineStatements ds = new DefineStatements();

        itemd.setAttribute("combine.children", "append");

        itema.insertBefore(doc.createTextNode("text"), itema.getLastChild());
        itemb.insertBefore(doc.createTextNode("text"), itemb.getLastChild());
        itemc.insertBefore(doc.createTextNode("text"), itemc.getLastChild());
        itemd.insertBefore(doc.createTextNode("text"), itemd.getFirstChild());
        iteme.insertBefore(doc.createTextNode("text"), iteme.getLastChild());



        prettyPrint(doc);

    }



    public static final void prettyPrint(Document xml) throws Exception {

        Transformer tf = TransformerFactory.newInstance().newTransformer();

        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        tf.setOutputProperty(OutputKeys.INDENT, "yes");

        Writer out = new StringWriter();

        tf.transform(new DOMSource(xml), new StreamResult(out));

        System.out.println(out.toString());

    }



}
