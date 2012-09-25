package org.purl.wf4ever.taverna.annotation.ui.view;

import java.util.Arrays;
import java.util.List;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ContextualViewFactory;
import net.sf.taverna.t2.workflowmodel.Dataflow;

public class AnnotationCollationContextViewFactory implements
		ContextualViewFactory<Dataflow> {

	public boolean canHandle(Object selection) {
		return selection instanceof Dataflow;
	}

	public List<ContextualView> getViews(Dataflow selection) {
		return Arrays.<ContextualView>asList(new AnnotationCollationView(selection));
	}
	
}
