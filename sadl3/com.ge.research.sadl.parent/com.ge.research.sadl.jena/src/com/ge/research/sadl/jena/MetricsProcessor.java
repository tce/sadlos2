package com.ge.research.sadl.jena;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;

import com.ge.research.sadl.jena.UtilsForJena;
import com.ge.research.sadl.reasoner.ConfigurationException;
import com.ge.research.sadl.reasoner.ConfigurationManagerForEditing;
import com.ge.research.sadl.reasoner.IConfigurationManager;
import com.ge.research.sadl.reasoner.IConfigurationManagerForEditing;
import com.ge.research.sadl.reasoner.ResultSet;
import com.ge.research.sadl.reasoner.utils.SadlUtils;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class MetricsProcessor {
	public static final String SADL_METRICS_NS = "http://com.ge.research.sadl/sadlmetricsmodel#";
	public static final String MARKER_CLASS_URI = SADL_METRICS_NS + "Marker";
	public static final String MARKER_PROP_URI = SADL_METRICS_NS + "marker";
	public static final String MARKER_TYPE_URI = SADL_METRICS_NS + "MarkerType";
	public static final String MARKER_TYPE_PROP_URI = SADL_METRICS_NS + "markerType";
	public static final String ERROR_MARKER_URI = SADL_METRICS_NS + "Error";
	public static final String WARNING_MARKER_URI = SADL_METRICS_NS + "Warning";
	public static final String INFO_MARKER_URI = SADL_METRICS_NS + "Information";
	public static final String TYPE_CHECK_FAILURE_URI = SADL_METRICS_NS + "TypeCheckFailure";
	public static final String UNCLASSIFIED_FAILURE_URI = SADL_METRICS_NS + "UnclassifiedFailure";
	public static final String RANGE_REDEFINITION_URI = SADL_METRICS_NS + "RangeRedefinition";
	public static final String DOMAIN_REDEFINITION_URI = SADL_METRICS_NS + "DomainRedefinition";
	public static final String CIRCULAR_IMPORT_URI = SADL_METRICS_NS + "CircularImport";
	public static final String CIRCULAR_DEFINITION_URI = SADL_METRICS_NS + "CircularDefinition";
	public static final String INVALID_EXPRESSION_URI = SADL_METRICS_NS + "InvalidExpression";
	public static final String NESTED_EQUATION_URI = SADL_METRICS_NS + "NestedEquation";
	public static final String INVALID_TABLE_FORMAT_URI = SADL_METRICS_NS + "InvalidTableFormat";
	public static final String DUPLICATE_NAME_URI = SADL_METRICS_NS + "DuplicateName";

	private String filename;
	private OntModel theJenaModel;
	private String baseUri;
	private HashMap<String,OntClass> markerClasses = null;
	private HashMap<String,Individual> markerTypeInstances = null;
	private Property markerTypeProperty = null;
	private Property markerProperty = null;
	private IConfigurationManagerForEditing configMgr = null;
	private Ontology modelOntology;
	
	public MetricsProcessor() {
		super();
	}
	
	public MetricsProcessor(String uri, org.eclipse.emf.ecore.resource.Resource resource) throws JenaProcessorException, ConfigurationException {
		if (resource == null) {
			throw new JenaProcessorException("MetricsProcessor constructor called with null resource");
		}
		String modelFolder = getModelFolderPath(resource);
		if (modelFolder != null) {
			// for JUnit tests, this will be null
			String fn = modelFolder + "/" + resource.getURI().segment(resource.getURI().segmentCount() - 1);
			baseUri = uri + ".metrics";
			filename = fn + ".metrics.owl";
			setTheJenaModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));
			configMgr = new ConfigurationManagerForEditing(modelFolder, IConfigurationManager.RDF_XML_ABBREV_FORMAT);
			modelOntology = getTheJenaModel().createOntology(baseUri);
			modelOntology.addComment("SADL model metrics", "en");
		}
		else {
			throw new ConfigurationException("Model folder not found");
		}
	}
	
	public boolean addMarker(String markerClassUri, String markerTypeUri) {
		Individual marker = getTheJenaModel().createIndividual(getMarkerClass(markerClassUri));
		Individual markerType = getMarkerTypeInstance(markerTypeUri);
		if (marker != null && markerType != null) {
			modelOntology.addProperty(getMarkerProperty(), marker);
			marker.addProperty(getMarkerTypeProperty(), markerType);
			return true;
		}
		return false;
	}
	
	public boolean saveMetrics(String format) throws IOException, ConfigurationException, URISyntaxException {
		RDFWriter w = getTheJenaModel().getWriter(format);
		w.setProperty("xmlbase", baseUri);
		FileOutputStream out = new FileOutputStream(filename);
		w.write(getTheJenaModel().getBaseModel(), out, baseUri);
		out.close();
		SadlUtils su = new SadlUtils();
		configMgr.addMapping(su.fileNameToFileUrl(filename), baseUri, null, false, "SRL_Metrics");
		configMgr.saveOntPolicyFile();
		return true;
	}
	
	public ResultSet queryProjectMetrics(String queryName, String modelFolder) {
		
		return null;
	}

	private OntModel getTheJenaModel() {
		return theJenaModel;
	}

	private void setTheJenaModel(OntModel theJenaModel) {
		this.theJenaModel = theJenaModel;
	}
	
	private String createUri(String name) {
		return baseUri + "#" + name;
	}

	private Property getMarkerTypeProperty() {
		if (markerTypeProperty == null) {
			markerTypeProperty = getTheJenaModel().createObjectProperty(MARKER_TYPE_PROP_URI);
		}
		return markerTypeProperty;
	}

	private Property getMarkerProperty() {
		if (markerProperty == null) {
			markerProperty = getTheJenaModel().createObjectProperty(MARKER_PROP_URI);
		}
		return markerProperty;
	}

	private OntClass getMarkerClass(String markerClassUri) {
		OntClass markerClass = null;
		if (markerClasses == null) {
			markerClasses = new HashMap<String, OntClass>();
		}
		if (!markerClasses.containsKey(markerClassUri)) {
			markerClass = getTheJenaModel().createClass(markerClassUri);
			markerClasses.put(markerClassUri, markerClass);
		}
		else {
			markerClass = markerClasses.get(markerClassUri);
		}
		return markerClass;
	}

	private Individual getMarkerTypeInstance(String markerTypeUri) {
		Individual markerType = null;
		if (markerTypeInstances == null) {
			markerTypeInstances = new HashMap<String,Individual>();
		}
		if (!markerTypeInstances.containsKey(markerTypeUri)) {
			markerType = getTheJenaModel().createIndividual(markerTypeUri, getTheJenaModel().createClass(MARKER_TYPE_URI));
			markerTypeInstances.put(markerTypeUri, markerType);
		}
		else {
			markerType = markerTypeInstances.get(markerTypeUri);
		}
		return markerType;
	}

	private String getModelFolderPath(org.eclipse.emf.ecore.resource.Resource resource) {
		URI v = resource.getURI().trimSegments(resource.getURI().segmentCount() - 2);
		v = v.appendSegment(UtilsForJena.OWL_MODELS_FOLDER_NAME);
		String modelFolderPathname;
		if (v.isPlatform()) {
			 IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(v.toPlatformString(true)));
			 modelFolderPathname = file.getRawLocation().toPortableString();
		}
		else {
			modelFolderPathname = v.toFileString();
		}
		return modelFolderPathname;
	}
}