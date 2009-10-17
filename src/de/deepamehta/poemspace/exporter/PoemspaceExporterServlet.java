package de.deepamehta.poemspace.exporter;

import de.deepamehta.BaseTopic;
import de.deepamehta.service.CorporateDirectives;
import de.deepamehta.service.Session;
import de.deepamehta.service.web.DeepaMehtaServlet;
import de.deepamehta.service.web.RequestParameter;

import javax.servlet.ServletException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class PoemspaceExporterServlet extends DeepaMehtaServlet {

    private static final String PAGE_EXPORT_RESULT = "export-result";

    private static final String TOPICTYPE_PROJECT = "t-1087";
    private static final String TOPICTYPE_BEZIRK = "t-1089";
    private static final String TOPICTYPE_KIEZ = "t-3659";
    private static final String TOPICTYPE_SITECAT = "t-1134";
    private static final String TOPICTYPE_ARTCAT = "t-1140";

    private static StringBuffer json;
	private static Logger logger = Logger.getLogger("de.deepamehta.poemspace.exporter");

    protected String performAction(String action, RequestParameter params, Session session, CorporateDirectives directives)
																									throws ServletException {
		if (action == null) {
		    session.setAttribute("json", exportToJSON());
		    return PAGE_EXPORT_RESULT;
		} else {
			return super.performAction(action, params, session, directives);
		}
	}

    // -----------

    private String exportToJSON() {
        json = new StringBuffer("[\n");
        //
        exportTopics(TOPICTYPE_PROJECT, "projects", "Workspace");
        exportTopics(TOPICTYPE_BEZIRK, "Bezirke", "Bezirk");
        exportTopics(TOPICTYPE_KIEZ, "Kieze", "Kiez");
        exportTopics(TOPICTYPE_SITECAT, "site categories", "Einrichtungsart");
        exportTopics(TOPICTYPE_ARTCAT, "art categories", "Kunstgattung");
        //
	    List persons = cm.getTopics(TOPICTYPE_PERSON);
        logger.info("#################### exporting " + persons.size() + " persons ####################");
	    for (int i = 0; i < 10 /* persons.size() */; i++) {
	        BaseTopic person = (BaseTopic) persons.get(i);
	        exportContact(person, i + 1, true);
        }
        //
	    List institutions = cm.getTopics(TOPICTYPE_INSTITUTION);
        logger.info("#################### exporting " + institutions.size() + " institutions ####################");
	    for (int i = 0; i < 10 /* institutions.size() */; i++) {
	        BaseTopic institution = (BaseTopic) institutions.get(i);
	        exportContact(institution, i + 1, i < 10 /* institutions.size() */ - 1);
        }
        //
        logger.info("#################### export complete ####################");
        json.append("]\n");
        return json.toString();
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
        json.append("{\n" +
            "\"id\": \"" + topic.getID() + "\",\n" +
            "\"type\": \"Topic\",\n" +
            "\"topic_type\": \"" + targetTypeName + "\",\n" +
            "\"fields\": [\n");
        exportField("Name", topic.getName(), true);
        exportMultiField("Description", as.getTopicProperty(topic, "Description"), 30, false);
        json.append("],\n" +
            "\"implementation\": \"PlainDocument\"\n" +
        "},\n");
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
            }
        }
        //
        logger.info("# " + nr + " \"" + as.getTopicProperty(contact, "Name") + "\", " +
            email_count + " email addresses" + (email_empty_count > 0 ? " (## " + email_empty_count + " empty)" : "") + ", " +
            website_count + " websites" + (website_empty_count > 0 ? " (## " + website_empty_count + " empty)" : "") + ", " +
            projects.size() + " projects, " + bezirke.size() + " bezirke, " + kieze.size() + " kieze, " + 
            sitecats.size() + " site cats, " + artcats.size() + " art cats"
        );
        //
        exportContact(contact, email.toString(), website.toString(), projects, bezirke, kieze, sitecats, artcats, addComma);
    }

    private void exportContact(BaseTopic contact, String email, String website,
                                        Set projects, Set bezirke, Set kieze, Set sitecats, Set artcats, boolean addComma) {
        String contactID = contact.getID();
        json.append("{\n" +
            "\"id\": \"" + contactID + "\",\n" +
            "\"type\": \"Topic\",\n" +
            "\"topic_type\": \"Contact\",\n" +
            "\"fields\": [\n");
        exportField("Name", as.getTopicProperty(contact, "Name"), true);
        exportMultiField("Notes", as.getTopicProperty(contact, "Description"), 4, true);
        exportMultiField("Address", as.getTopicProperty(contact, "Adresse"), 4, true);
        exportMultiField("Phone", as.getTopicProperty(contact, "Telefon"), 2, true);
        exportMultiField("Email", email, 2, true);
        exportMultiField("Website", website, 2, true);
        exportRelationField("Workspaces", "Workspace", false);
        json.append("],\n" +
            "\"implementation\": \"PlainDocument\"\n" +
        "}");
        // contact assignments
        exportAssociations(contactID, projects);
        exportAssociations(contactID, bezirke);
        exportAssociations(contactID, kieze);
        exportAssociations(contactID, sitecats);
        exportAssociations(contactID, artcats);
        //
        json.append(addComma ? "," : "");
        json.append("\n");
    }

    /*** Helper ***/

    private void exportField(String id, String content, boolean addComma) {
        json.append("{" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"text\"}, " +
            "\"view\": {\"editor\": \"single line\"}, " +
            "\"content\": \"" + content + "\"" +
        "}");
        json.append(addComma ? "," : "");
        json.append("\n");
    }

    private void exportMultiField(String id, String content, int lines, boolean addComma) {
        json.append("{" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"text\"}, " +
            "\"view\": {\"editor\": \"multi line\", \"lines\": " + lines + "}, " +
            "\"content\": \"" + toJSON(content) + "\"" +
        "}");
        json.append(addComma ? "," : "");
        json.append("\n");
    }

    private void exportRelationField(String id, String relation_type, boolean addComma) {
        json.append("{" +
            "\"id\": \"" + id + "\", " +
            "\"model\": {\"type\": \"relation\", \"relation_type\": \"" + relation_type + "\"}, " +
            "\"view\": {\"editor\": \"checkboxes\"}" +
        "}");
        json.append(addComma ? "," : "");
        json.append("\n");
    }

    private void exportAssociations(String topicID, Set relTopicIDs) {
        Iterator i = relTopicIDs.iterator();
        json.append(i.hasNext() ? "," : "");
        json.append("\n");
        while (i.hasNext()) {
            String relTopicID = (String) i.next();
            exportAssociation(topicID, relTopicID, i.hasNext());
        }
    }

    private void exportAssociation(String topicID1, String topicID2, boolean addComma) {
        json.append("{" +
            "\"type\": \"Relation\", " +
            "\"rel_type\": \"Relation\", " +
            "\"rel_doc_ids\": [\"" + topicID1 + "\", \"" + topicID2 + "\"]" +
            "}");
        json.append(addComma ? "," : "");
        json.append("\n");
    }

    //

    private String toJSON(String text) {
        // convert HTML tags
        text = text.replaceAll("<html>", "");
        text = text.replaceAll("</html>", "");
        text = text.replaceAll("<head>", "");
        text = text.replaceAll("</head>", "");
        text = text.replaceAll("<body>", "");
        text = text.replaceAll("</body>", "");
        text = text.replaceAll("<p>", "");
        text = text.replaceAll("<p style=\"margin-top: 0\">", "");
        text = text.replaceAll("</p>", "");
        // convert HTML entities                    // IMPORTANT: must compile with encoding UTF-8
        text = text.replaceAll("&#228;", "ä");
        text = text.replaceAll("&#246;", "ö");
        text = text.replaceAll("&#252;", "ü");
        text = text.replaceAll("&#196;", "Ä");      // C4
        text = text.replaceAll("&#214;", "Ö");      // D6
        text = text.replaceAll("&#220;", "Ü");      // DC
        text = text.replaceAll("&#223;", "ß");      // DF
        text = text.replaceAll("&#8211;", "--");    // 2013     option-minus    emulated by double minus
        //
        text = text.trim();
        // JSON conformity
        text = text.replaceAll("\r", "\\\\n");
        text = text.replaceAll("\n", "\\\\n");
        text = text.replaceAll("\"", "\\\\\"");
        //
        if (text.indexOf("&#") >= 0) {
            logger.warning("### HTML entity not converted (\"" + text + "\")");
        }
        //
        return text;
    }
}
