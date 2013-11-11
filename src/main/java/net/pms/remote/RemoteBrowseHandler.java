package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException {
		HttpPrincipal p = t.getPrincipal();
		RootFolder root = parent.getRoot(p.getUsername(), true);
		List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), null);

		// Media browser HTML
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>").append(CRLF);
			sb.append("<head>").append(CRLF);
				sb.append("<meta charset=\"utf-8\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"http://www.universalmediaserver.com/css/reset.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"icon\" href=\"http://www.universalmediaserver.com/favicon.ico\" type=\"image/x-icon\">").append(CRLF);
				sb.append("<title>Universal Media Server</title>").append(CRLF);
			sb.append("</head>").append(CRLF);
			sb.append("<body id=\"ContentPage\">").append(CRLF);
				sb.append("<div id=\"Container\">");
					sb.append("<div id=\"Menu\">");
						sb.append("<a href=\"/\" id=\"HomeButton\"></a>");
					sb.append("</div>");
					sb.append("<ul>").append(CRLF);
						for (DLNAResource r : res) {
							String newId = r.getResourceId();
							String idForWeb = URLEncoder.encode(newId, "UTF-8");
							String thumb = "/thumb/" + idForWeb;
							String path = "/browse/";
							if (!r.isFolder()) {
								path = "/play/";
								//newId = newId + "." + r.getFormat().getMatchedId();
							}
							String name = r.resumeName();
							sb.append("<li>");
								sb.append("<a href=\"").append(path).append(idForWeb).append("\" title=\"").append(name).append("\">");
									sb.append("<img src=\"").append(thumb).append("\" alt=\"").append(name).append("\" /><br>");
									sb.append("<span>");
										sb.append(name);
									sb.append("</span>");
								sb.append("</a>").append(CRLF);
							sb.append("</li>").append(CRLF);
						}
					sb.append("</ul>");
				sb.append("</div>");
			sb.append("</body>");
		sb.append("</html>");

		return sb.toString();
	}

	private void writePage(String response, HttpExchange t) throws IOException {
		LOGGER.debug("Write page " + response);
		t.sendResponseHeaders(200, response.length());
		try (OutputStream os = t.getResponseBody()) {
			os.write(response.getBytes());
		}
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("Got a browse request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id = RemoteUtil.getId("browse/", t);
		LOGGER.debug("Found id " + id);
		String response = mkBrowsePage(id, t);
		writePage(response, t);
	}
}