package org.purl.wf4ever.taverna.annotation.ui.view;

import java.awt.Frame;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;
import net.sf.taverna.t2.workflowmodel.Dataflow;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.purl.wf4ever.taverna.annotation.ui.view.AnnotationCollationView.WfAnnotation;

@SuppressWarnings("serial")
public class AnnotationCollationView extends ContextualView {
	private final Dataflow dataflow;
	private JLabel description = new JLabel("ads");
	private static Logger logger = Logger
			.getLogger(AnnotationCollationView.class);
	private static final String WF_BY_UUID_QUERY = "http://rdf.myexperiment.org/sparql?query=PREFIX+dcterms%3A+%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E%0D%0APREFIX+mecomp%3A+%3Chttp%3A%2F%2Frdf.myexperiment.org%2Fontologies%2Fcomponents%2F%3E+%0D%0APREFIX+mecontrib%3A+%3Chttp%3A%2F%2Frdf.myexperiment.org%2Fontologies%2Fcontributions%2F%3E+%0D%0ASELECT+DISTINCT+%3Fwf%0D%0AWHERE+%7B+%0D%0A++%3Fwf+mecomp%3Aexecutes-dataflow+%3Fdf+.+%0D%0A++%3Fdf+dcterms%3Aidentifier+%3Fuuid+.+%0D%0A++FILTER+regex%28%3Fuuid%2C%27{uuid}%27%29+%0D%0A%7D&formatting=JSON";

	private static final String TITLE_DESCRIPTION_QUERY = "http://rdf.myexperiment.org/sparql?query=BASE+%3Chttp%3A%2F%2Fwww.myexperiment.org%2F%3E%0D%0APREFIX+dc%3A+%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Fterms%2F%3E%0D%0A%0D%0ASELECT+DISTINCT+%3Fdescription%2C+%3Ftitle%0D%0AWHERE+%7B%0D%0A++{wf}+dc%3Adescription+%3Fdescription+%3B%0D%0A+++++++++++++++++++++++++++++++++++++++++++++dc%3Atitle+%3Ftitle%0D%0A%7D%0D%0A&formatting=JSON";

	public AnnotationCollationView(Dataflow selection) {
		this.dataflow = selection;
		initView();
	}

	@Override
	public JComponent getMainFrame() {
		JPanel jPanel = new JPanel();
		jPanel.add(description);
		refreshView();
		return jPanel;
	}

	@Override
	public String getViewTitle() {
		return "Collated annotations";
	}

	private List<URI> findMyExperimentWorkflows(String dataflowIdentifier)
			throws IOException, JsonParseException, JsonMappingException,
			MalformedURLException {

		String url = WF_BY_UUID_QUERY.replace("{uuid}", dataflowIdentifier);
		List<URI> wfs = new ArrayList<URI>();
		try {

			logger.info("Retrieving " + url);
			// resultsJson = IOUtils.toString(new URL(url).openStream(),
			// "utf-8");
			Map map = om.readValue(new URL(url), Map.class);
			Map resultsMap = (Map) map.get("results");
			for (Map binding : (List<Map>) resultsMap.get("bindings")) {
				Map wf = (Map) binding.get("wf");
				String wfUrl = (String) wf.get("value");
				logger.debug(wfUrl);
				wfs.add(URI.create(wfUrl));
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unexpected error", e);
		}
		return wfs;

	}

	private static ObjectMapper om = new ObjectMapper();

	public class WfAnnotation {
		private URI workflow;
		private String title;
		private String description;

		// String author;
		// Date when;
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public URI getWorkflow() {
			return workflow;
		}

		public void setWorkflow(URI workflow) {
			this.workflow = workflow;
		}
	}

	private List<WfAnnotation> findAnnotations(URI wf) throws IOException,
			JsonParseException, JsonMappingException, MalformedURLException {

		String url = TITLE_DESCRIPTION_QUERY.replace("{wf}",
				"<" + wf.toASCIIString() + ">");
		List<WfAnnotation> annotations = new ArrayList<WfAnnotation>();
		try {
			logger.info("Retrieving " + url);
			Map map = om.readValue(new URL(url), Map.class);
			Map resultsMap = (Map) map.get("results");
			for (Map binding : (List<Map>) resultsMap.get("bindings")) {
				Map title = (Map) binding.get("title");
				String titleValue = (String) title.get("value");

				WfAnnotation ann = new WfAnnotation();
				ann.setWorkflow(wf);
				ann.setTitle(titleValue);

				Map desc = (Map) binding.get("title");
				String descValue = (String) title.get("value");
				ann.setDescription(descValue);
				annotations.add(ann);
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Unexpected error", e);
		}
		return annotations;

	}

	/**
	 * Typically called when the activity configuration has changed.
	 */
	@Override
	public void refreshView() {
		description.setText(getText());
		// TODO: Might also show extra service information looked
		// up dynamically from endpoint/registry
	}

	public String getText() {

		List<URI> wfs;
		try {
			wfs = findMyExperimentWorkflows(dataflow.getIdentifier());
		} catch (IOException e) {
			logger.warn(
					"Unable to retrieve workflow " + dataflow.getIdentifier(),
					e);
			return "Could not connect to myExperiment: "
					+ e.getLocalizedMessage();
		}
		Set<WfAnnotation> annotations = new LinkedHashSet<WfAnnotation>();
		for (URI wf : wfs) {
			try {
				List<WfAnnotation> anns = findAnnotations(wf);
				annotations.addAll(anns);
			} catch (Exception e) {
				logger.warn("Could not read annotations for " + wf, e);
			}
		}

		Map<String, List<WfAnnotation>> titles = new LinkedHashMap<String, List<WfAnnotation>>();
		for (WfAnnotation ann : annotations) {
			String title = ann.getTitle();
			if (!titles.containsKey(title)) {
				titles.put(title, new ArrayList<WfAnnotation>());
			}
			titles.get(title).add(ann);
		}
		Map<String, List<WfAnnotation>> descriptions = new LinkedHashMap<String, List<WfAnnotation>>();
		for (WfAnnotation ann : annotations) {
			String desc = ann.getDescription();
			if (!descriptions.containsKey(desc)) {
				descriptions.put(desc, new ArrayList<WfAnnotation>());
			}
			descriptions.get(desc).add(ann);
		}

		StringBuffer sb = new StringBuffer();
		sb.append("<html><body>");

		// titles
		sb.append("<h3>Title(s)</h3>");
		for (String title : titles.keySet()) {
			sb.append("<p>" + title + "<p>");
			sb.append("<p>From: </p><ul>");
			for (WfAnnotation ann : titles.get(title)) {
				sb.append("<li>" + ann.getWorkflow() + "</li>");
			}
			sb.append("</ul>");
		}
		sb.append("</body></html>");

		return sb.toString();
	}

	/**
	 * View position hint
	 */
	@Override
	public int getPreferredPosition() {
		// We want to be on top
		return 100;
	}

	@Override
	public Action getConfigureAction(final Frame owner) {
		return null;
	}

}
