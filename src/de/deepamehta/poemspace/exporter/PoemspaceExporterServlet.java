package de.deepamehta.poemspace.exporter;

import de.deepamehta.BaseTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;

import javax.servlet.ServletException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class PoemspaceExporterServlet extends DeepaMehtaServlet {

    private static final String PAGE_EXPORT_RESULT = "export-result";

    private static final String TOPICTYPE_PROJECT = "t-1087";
    private static final String TOPICTYPE_BEZIRK = "t-1089";
    private static final String TOPICTYPE_KIEZ = "t-3659";
    private static final String TOPICTYPE_SITECAT = "t-1134";
    private static final String TOPICTYPE_ARTCAT = "t-1140";

    private static PrintWriter out;
    private static Logger logger = Logger.getLogger("de.deepamehta.poemspace.exporter");

    protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives)
                                                                                                    throws ServletException {
        if (action == null) {
            exportToJSON();
            return PAGE_EXPORT_RESULT;
        } else {
            return super.performAction(action, params, session, directives);
        }
    }

    // -----------

    private void exportToJSON() {
        try {
            File file = new File("dm2-export.json");
            logger.info("Exporting to file " + file.getAbsolutePath());
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
            out.println("[");
            //
            exportTopics(TOPICTYPE_PROJECT, "projects", "Workspace");
            exportTopics(TOPICTYPE_BEZIRK, "Bezirke", "Bezirk");
            exportTopics(TOPICTYPE_KIEZ, "Kieze", "Kiez");
            exportTopics(TOPICTYPE_SITECAT, "site categories", "Einrichtungsart");
            exportTopics(TOPICTYPE_ARTCAT, "art categories", "Kunstgattung");
            //
            List persons = cm.getTopics(TOPICTYPE_PERSON);
            logger.info("#################### exporting " + persons.size() + " persons ####################");
            for (int i = 0; i < persons.size(); i++) {
                BaseTopic person = (BaseTopic) persons.get(i);
                exportContact(person, i + 1, true);
            }
            //
            List institutions = cm.getTopics(TOPICTYPE_INSTITUTION);
            logger.info("#################### exporting " + institutions.size() + " institutions ####################");
            for (int i = 0; i < institutions.size(); i++) {
                BaseTopic institution = (BaseTopic) institutions.get(i);
                exportContact(institution, i + 1, i < institutions.size() - 1);
            }
            //
            logger.info("#################### export complete ####################");
            out.println("]");
            out.close();
        } catch (Throwable e) {
            logger.severe("### Error while opening file: " + e);
        }
    }

    private void exportTopics(String topicTypeID, String pluralName, String targetTypeName) {
        List topics = cm.getTopics(topicTypeID);
        logger.info("#################### exporting " + topics.size() + " " + pluralName + " ####################");
        for (int i = 0; i < topics.size(); i++) {
            BaseTopic topic = (BaseTopic) topics.get(i);
            exportTopic(topic, targetTypeName, i + 1);
        }
    }

    private void exportTopic(BaseTopic topic, String targetTypeName, int nr) {
        logger.info("# " + nr + " \"" + topic.getName() + "\"");
        //
        out.println("{\n" +
            "    \"_id\": \"" + topic.getID() + "\",\n" +
            "    \"type\": \"Topic\",\n" +
            "    \"topic_type\": \"" + targetTypeName + "\",\n" +
            "    \"fields\": [");
        exportField("Name", topic.getName(), true);
        exportMultiField("Description", as.getTopicProperty(topic, "Description"), 30, false);
        out.println("    ],\n" +
            "    \"implementation\": \"PlainDocument\"\n" +
        "},");
    }

    private void exportContact(BaseTopic contact, int nr, boolean addComma) {
        StringBuffer email = new StringBuffer();
        StringBuffer website = new StringBuffer();
        int email_count = 0, email_empty_count = 0;
        int website_count = 0, website_empty_count = 0;
        HashSet projects = new HashSet();
        HashSet bezirke = new HashSet();
        HashSet kieze = new HashSet();
        HashSet sitecats = new HashSet();
        HashSet artcats = new HashSet();
        HashSet institutions = new HashSet();
        HashSet persons = new HashSet();
        //
        // follow associations
        List relTopics = cm.getRelatedTopics(contact.getID());
        Iterator i = relTopics.iterator();
        while (i.hasNext()) {
            BaseTopic relTopic = (BaseTopic) i.next();
            if (relTopic.getType().equals(TOPICTYPE_EMAIL_ADDRESS)) {
                email_count++;
                String ea = as.getTopicProperty(relTopic, PROPERTY_EMAIL_ADDRESS);
                if (ea.length() > 0) {
                    email.append(email.length() > 0 ? "\n" : "");
                    email.append(ea);
                } else {
                    email_empty_count++;
                }
            } else if (relTopic.getType().equals(TOPICTYPE_WEBPAGE)) {
                website_count++;
                String ws = as.getTopicProperty(relTopic, PROPERTY_URL);
                if (ws.length() > 0) {
                    website.append(website.length() > 0 ? "\n" : "");
                    website.append(ws);
                } else {
                    website_empty_count++;
                }
            } else if (relTopic.getType().equals(TOPICTYPE_PROJECT)) {
                projects.add(relTopic.getID());
            } else if (relTopic.getType().equals(TOPICTYPE_BEZIRK)) {
                bezirke.add(relTopic.getID());
            } else if (relTopic.getType().equals(TOPICTYPE_KIEZ)) {
                kieze.add(relTopic.getID());
            } else if (relTopic.getType().equals(TOPICTYPE_SITECAT)) {
                sitecats.add(relTopic.getID());
            } else if (relTopic.getType().equals(TOPICTYPE_ARTCAT)) {
                artcats.add(relTopic.getID());
            } else if (relTopic.getType().equals(TOPICTYPE_INSTITUTION)) {
                if (contact.getType().equals(TOPICTYPE_INSTITUTION)) {
                    if (contact.getID().compareTo(relTopic.getID()) > 0) {
                        institutions.add(relTopic.getID());     // Institution -> Institution
                    }
                } else {
                    institutions.add(relTopic.getID());         // Person -> Institution
                }
            } else if (relTopic.getType().equals(TOPICTYPE_PERSON)) {
                if (contact.getType().equals(TOPICTYPE_PERSON)) {
                    if (contact.getID().compareTo(relTopic.getID()) > 0) {
                        persons.add(relTopic.getID());          // Person -> Person
                    }
                }
                // Note: Institution -> Person is already being exported
            }
        }
        //
        logger.info("# " + nr + " \"" + as.getTopicProperty(contact, "Name") + "\", " +
            email_count + " email addresses" + (email_empty_count > 0 ? " (## " + email_empty_count + " empty)" : "") + ", " +
            website_count + " websites" + (website_empty_count > 0 ? " (## " + website_empty_count + " empty)" : "") + ", " +
            projects.size() + " projects, " + bezirke.size() + " bezirke, " + kieze.size() + " kieze, " + 
            sitecats.size() + " site cats, " + artcats.size() + " art cats, " +
            institutions.size() + " institutions, " + persons.size() + " persons"
        );
        //
        exportContact(contact, email.toString(), website.toString(), projects, bezirke, kieze, sitecats, artcats,
                institutions, persons, addComma);
    }

    private void exportContact(BaseTopic contact, String email, String website, Set projects, Set bezirke, Set kieze,
                                                  Set sitecats, Set artcats, Set institutions, Set persons, boolean addComma) {
        String contactID = contact.getID();
        String contactTypeID = contact.getType();
        String topicType = contactTypeID.equals(TOPICTYPE_PERSON) ? "Person" : "Institution";
        out.println("{\n" +
            "    \"_id\": \"" + contactID + "\",\n" +
            "    \"type\": \"Topic\",\n" +
            "    \"topic_type\": \"" + topicType + "\",\n" +
            "    \"fields\": [");
        exportField("Name", as.getTopicProperty(contact, "Name"), true);
        exportMultiField("Phone", as.getTopicProperty(contact, "Telefon"), 2, true);
        exportMultiField("Email", email, 2, true);
        exportMultiField("Website", website, 2, true);
        exportMultiField("Address", as.getTopicProperty(contact, "Adresse"), 4, true);
        exportMultiField("Notes", as.getTopicProperty(contact, "Description"), 4, true);
        exportRelationField("Bezirk", "Bezirk", true);
        exportRelationField("Kiez", "Kiez", true);
        if (contactTypeID.equals(TOPICTYPE_INSTITUTION)) {
            exportRelationField("Einrichtungsart", "Einrichtungsart", true);
        }
        exportRelationField("Kunstgattung", "Kunstgattung", true);
        exportRelationField("Workspaces", "Workspace", false);
        out.print("    ],\n" +
            "    \"implementation\": \"PlainDocument\"\n" +
        "}");
        // contact assignments
        exportAssociations(contactID, projects);
        exportAssociations(contactID, bezirke);
        exportAssociations(contactID, kieze);
        exportAssociations(contactID, sitecats);
        exportAssociations(contactID, artcats);
        exportAssociations(contactID, institutions);
        exportAssociations(contactID, persons);
        //
        out.print(addComma ? "," : "");
        out.println();
    }

    /*** Helper ***/

    private void exportField(String id, String content, boolean addComma) {
        out.print("        {" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"text\"}, " +
            "\"view\": {\"editor\": \"single line\"}, " +
            "\"content\": \"" + content + "\"" +
        "}");
        out.print(addComma ? "," : "");
        out.println();
    }

    private void exportMultiField(String id, String content, int lines, boolean addComma) {
        out.print("        {" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"text\"}, " +
            "\"view\": {\"editor\": \"multi line\", \"lines\": " + lines + "}, " +
            "\"content\": \"" + toJSON(content) + "\"" +
        "}");
        out.print(addComma ? "," : "");
        out.println();
    }

    private void exportRelationField(String id, String related_type, boolean addComma) {
        out.print("        {" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"relation\", \"related_type\": \"" + related_type + "\"}, " +
            "\"view\": {\"editor\": \"checkboxes\"}" +
        "}");
        out.print(addComma ? "," : "");
        out.println();
    }

    //

    private void exportAssociations(String topicID, Set relTopicIDs) {
        Iterator i = relTopicIDs.iterator();
        if (i.hasNext()) {
            out.println(",");
            while (i.hasNext()) {
                String relTopicID = (String) i.next();
                exportAssociation(topicID, relTopicID, i.hasNext());
            }
        }
    }

    private void exportAssociation(String topicID1, String topicID2, boolean addComma) {
        out.print("{" +
            "\"type\": \"Relation\", " +
            "\"rel_type\": \"Relation\", " +
            "\"rel_doc_ids\": [\"" + topicID1 + "\", \"" + topicID2 + "\"]" +
            "}");
        if (addComma) {
            out.println(",");
        }
    }

    //

    private String toJSON(String text) {
        // strip HTML tags
        text = text.replaceAll("<html>", "");
        text = text.replaceAll("</html>", "");
        text = text.replaceAll("<head>", "");
        text = text.replaceAll("</head>", "");
        text = text.replaceAll("<body>", "");
        text = text.replaceAll("</body>", "");
        text = text.replaceAll("<p>", "");
        text = text.replaceAll("<p style=\"margin-top: 0\">", "");
        text = text.replaceAll("</p>", "");
        // convert HTML entities
        text = toUnicode(text);
        //
        text = text.trim();
        // JSON conformity
        text = text.replaceAll("\r", "\\\\n");
        text = text.replaceAll("\n", "\\\\n");
        text = text.replaceAll("\"", "\\\\\"");
        //
        return text;
    }

    private String toUnicode(String text) {
        StringBuffer buffer = new StringBuffer();
        Pattern p = Pattern.compile("&#(\\d+);");
        Matcher m = p.matcher(text);
        while (m.find()) {
            int c = Integer.parseInt(m.group(1));
            m.appendReplacement(buffer, Character.toString((char) c));
        }
        m.appendTail(buffer);
        return buffer.toString();
    }
}
