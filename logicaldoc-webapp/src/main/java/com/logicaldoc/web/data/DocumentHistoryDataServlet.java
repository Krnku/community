package com.logicaldoc.web.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.logicaldoc.core.PersistenceException;
import com.logicaldoc.core.document.Document;
import com.logicaldoc.core.document.dao.DocumentDAO;
import com.logicaldoc.core.document.dao.DocumentHistoryDAO;
import com.logicaldoc.core.security.Menu;
import com.logicaldoc.core.security.Session;
import com.logicaldoc.core.security.dao.MenuDAO;
import com.logicaldoc.core.util.IconSelector;
import com.logicaldoc.i18n.I18N;
import com.logicaldoc.util.Context;
import com.logicaldoc.util.io.FileUtil;

/**
 * This servlet is responsible for documents history data.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 6.0
 */
public class DocumentHistoryDataServlet extends AbstractDataServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response, Session session, Integer max,
			Locale locale) throws IOException, PersistenceException {

		MenuDAO mDao = (MenuDAO) Context.get().getBean(MenuDAO.class);
		boolean showSid = mDao.isReadEnable(Menu.SESSIONS, session.getUserId());

		PrintWriter writer = response.getWriter();
		writer.write("<list>");

		StringBuilder query = new StringBuilder(
				"select A.username, A.event, A.version, A.date, A.comment, A.filename, A.isNew, A.folderId, A.docId, A.path, A.sessionId, A.userId, A.reason, A.ip, A.device, A.geolocation, A.color, A.fileVersion from DocumentHistory A where 1=1 and A.deleted = 0 ");
		Map<String, Object> params = prepareQueryParams(request, query);
		DocumentHistoryDAO dao = (DocumentHistoryDAO) Context.get().getBean(DocumentHistoryDAO.class);
		List<Object> records = (List<Object>) dao.findByQuery(query.toString(), params, max != null ? max : 100);

		// Used only to cache the already encountered documents when the
		// history
		// is related to a single user (for dashboard visualization)
		Set<Long> docIds = new HashSet<Long>();

		/*
		 * Iterate over records composing the response XML document
		 */
		DateFormat df = getDateFormat();
		for (Object record : records) {
			Object[] cols = (Object[]) record;
			if (request.getParameter("userId") != null) {
				/*
				 * If the request contains the user specification, we report
				 * just the latest event per each document
				 */
				if (docIds.contains(cols[8]))
					continue;
				else
					docIds.add((Long) cols[8]);
			}

			printHistory(writer, cols, locale, showSid);
		}
		writer.write("</list>");
	}

	private void printHistory(PrintWriter writer, Object[] historyRecord, Locale locale, boolean showSid) {
		writer.print("<history>");
		writer.print("<user><![CDATA[" + historyRecord[0] + "]]></user>");
		writer.print("<event><![CDATA[" + I18N.message((String) historyRecord[1], locale) + "]]></event>");
		writer.print("<version>" + historyRecord[2] + "</version>");

		DateFormat df = getDateFormat();
		writer.print("<date>" + df.format((Date) historyRecord[3]) + "</date>");

		writer.print("<comment><![CDATA[" + (historyRecord[4] == null ? "" : historyRecord[4]) + "]]></comment>");
		writer.print("<filename><![CDATA[" + (historyRecord[5] == null ? "" : historyRecord[5]) + "]]></filename>");
		writer.print("<icon>"
				+ FileUtil.getBaseName(IconSelector.selectIcon(FileUtil.getExtension((String) historyRecord[5])))
				+ "</icon>");
		writer.print("<new>" + (1 == (Integer) historyRecord[6]) + "</new>");
		writer.print("<folderId>" + historyRecord[7] + "</folderId>");
		writer.print("<docId>" + historyRecord[8] + "</docId>");
		writer.print("<path><![CDATA[" + (historyRecord[9] == null ? "" : historyRecord[9]) + "]]></path>");
		if (showSid)
			writer.print("<sid><![CDATA[" + (historyRecord[10] == null ? "" : historyRecord[10]) + "]]></sid>");
		writer.print("<userId>" + historyRecord[11] + "</userId>");
		writer.print("<reason><![CDATA[" + (historyRecord[12] == null ? "" : historyRecord[12]) + "]]></reason>");
		writer.print("<ip><![CDATA[" + (historyRecord[13] == null ? "" : historyRecord[13]) + "]]></ip>");
		writer.print("<device><![CDATA[" + (historyRecord[14] == null ? "" : historyRecord[14]) + "]]></device>");
		writer.print(
				"<geolocation><![CDATA[" + (historyRecord[15] == null ? "" : historyRecord[15]) + "]]></geolocation>");
		if (historyRecord[16] != null)
			writer.write("<color><![CDATA[" + historyRecord[16] + "]]></color>");
		writer.print("<fileVersion>" + (historyRecord[17] == null ? "" : historyRecord[17]) + "</fileVersion>");
		writer.print("</history>");
	}

	private Map<String, Object> prepareQueryParams(HttpServletRequest request, StringBuilder query)
			throws PersistenceException {
		Map<String, Object> params = new HashMap<String, Object>();
		if (request.getParameter("docId") != null) {
			Long docId = Long.parseLong(request.getParameter("docId"));
			DocumentDAO ddao = (DocumentDAO) Context.get().getBean(DocumentDAO.class);
			Document doc = ddao.findDocument(docId);
			if (doc != null)
				docId = doc.getId();
			query.append(" and A.docId = :docId");
			params.put("docId", docId);
		}
		if (request.getParameter("userId") != null) {
			query.append(" and A.userId = :userId");

		}
		if (request.getParameter("event") != null) {
			query.append(" and A.event = :event");
			params.put("event", request.getParameter("event"));
		}
		query.append(" order by A.date desc ");
		return params;
	}
}