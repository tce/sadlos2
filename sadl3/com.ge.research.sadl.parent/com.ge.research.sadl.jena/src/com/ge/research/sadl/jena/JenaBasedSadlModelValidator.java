package com.ge.research.sadl.jena;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import com.ge.research.sadl.model.CircularDefinitionException;
import com.ge.research.sadl.model.ConceptIdentifier;
import com.ge.research.sadl.model.ConceptName;
import com.ge.research.sadl.model.ConceptName.ConceptType;
import com.ge.research.sadl.model.ConceptName.RangeValueType;
import com.ge.research.sadl.model.DeclarationExtensions;
import com.ge.research.sadl.model.OntConceptType;
import com.ge.research.sadl.processing.ISadlModelValidator;
import com.ge.research.sadl.processing.ValidationAcceptor;
import com.ge.research.sadl.reasoner.ConfigurationException;
import com.ge.research.sadl.reasoner.InvalidNameException;
import com.ge.research.sadl.reasoner.TranslationException;
import com.ge.research.sadl.sADL.BinaryOperation;
import com.ge.research.sadl.sADL.BooleanLiteral;
import com.ge.research.sadl.sADL.Constant;
import com.ge.research.sadl.sADL.Declaration;
import com.ge.research.sadl.sADL.ElementInList;
import com.ge.research.sadl.sADL.Expression;
import com.ge.research.sadl.sADL.Name;
import com.ge.research.sadl.sADL.NumberLiteral;
import com.ge.research.sadl.sADL.PropOfSubject;
import com.ge.research.sadl.sADL.SadlDataType;
import com.ge.research.sadl.sADL.SadlIntersectionType;
import com.ge.research.sadl.sADL.SadlPrimitiveDataType;
import com.ge.research.sadl.sADL.SadlPropertyCondition;
import com.ge.research.sadl.sADL.SadlResource;
import com.ge.research.sadl.sADL.SadlSimpleTypeReference;
import com.ge.research.sadl.sADL.SadlTypeReference;
import com.ge.research.sadl.sADL.SadlUnionType;
import com.ge.research.sadl.sADL.StringLiteral;
import com.ge.research.sadl.sADL.SubjHasProp;
import com.ge.research.sadl.sADL.Sublist;
import com.ge.research.sadl.sADL.UnaryExpression;
import com.ge.research.sadl.sADL.Unit;
import com.ge.research.sadl.sADL.ValueTable;
import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class JenaBasedSadlModelValidator implements ISadlModelValidator {
	protected ValidationAcceptor issueAcceptor = null;
	protected OntModel theJenaModel = null;
	private DeclarationExtensions declarationExtensions = null;
	private List<String> comparisonOperators = Arrays.asList(">=",">","<=","<","==","!=","is","=","not","unique","in","contains","does",/*"not",*/"contain");
	private List<String> numericOperators = Arrays.asList("*","+","/","-","%");
	private EObject defaultContext;
	
	protected Map<EObject, TypeCheckInfo> expressionsValidated = new HashMap<EObject,TypeCheckInfo>();
	private IMetricsProcessor metricsProcessor = null; 
	
	public class TypeCheckInfo {
		private EObject context = null;
    	private ConceptIdentifier expressionType = null;
    	private ConceptIdentifier typeCheckType = null;
    	private RangeValueType rangeValueType = null;
    	private List<ConceptName> implicitProperties = null;
    	
    	public TypeCheckInfo(ConceptIdentifier eType) {
    		setExpressionType(eType);
    	}
    	
    	public TypeCheckInfo(ConceptIdentifier eType, ConceptIdentifier tcType, JenaBasedSadlModelValidator validator, EObject ctx) {
    		setExpressionType(eType);
    		setTypeCheckType(tcType);
    		context = ctx;
    		if (ctx != null) {
    			validator.expressionsValidated.put(ctx,  this);
    		}
    	}
    	
    	public TypeCheckInfo(ConceptIdentifier eType, ConceptIdentifier tcType, List<ConceptName> impliedProps, JenaBasedSadlModelValidator validator, EObject ctx) {
    		setExpressionType(eType);
    		setTypeCheckType(tcType);
    		if (impliedProps != null) {
    			if (implicitProperties == null) {
    				implicitProperties = impliedProps;
    			}
    			else {
    				implicitProperties.addAll(impliedProps);
    			}
    		}
    		context = ctx;
    		if (ctx != null) {
    			validator.expressionsValidated.put(ctx,  this);
    		}
    	}

		public ConceptIdentifier getExpressionType() {
			return expressionType;
		}

		public void setExpressionType(ConceptIdentifier expressionType) {
			this.expressionType = expressionType;
		}

		private ConceptIdentifier getTypeCheckType() {
			return typeCheckType;
		}

		private void setTypeCheckType(ConceptIdentifier typeCheckType) {
			this.typeCheckType = typeCheckType;
		}
		
		protected RangeValueType getRangeValueType() {
			return rangeValueType;
		}
		
		protected void setContext(JenaBasedSadlModelValidator validator, EObject ctx) {
			this.context = ctx;
			if (ctx != null) {
				validator.expressionsValidated.put(ctx, this);
			}
		}

		protected void setRangeValueType(RangeValueType rangeValueType) {
			this.rangeValueType = rangeValueType;
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer("TypeCheckInfo(");
			if (getRangeValueType() != null && !getRangeValueType().equals(RangeValueType.CLASS_OR_DT)) {
				sb.append(getRangeValueType().toString());
				sb.append(" of values of type, ");
			}
			sb.append(expressionType.toString());
			sb.append(", ");
			sb.append(typeCheckType.toString());
			sb.append(")");
			return sb.toString();
		}
		
		public void addImplicitProperty(ConceptName implicitProp) {
			if (implicitProp != null) {
				if (implicitProperties == null) {
					implicitProperties = new ArrayList<ConceptName>();
				}
				if (!implicitProperties.contains(implicitProp)) {
					implicitProperties.add(implicitProp);
				}
			}
		}
		
		public List<ConceptName> getImplicitProperties() {
			return implicitProperties;
		}
    }
	
	public JenaBasedSadlModelValidator(ValidationAcceptor issueAcceptor, OntModel theJenaModel, DeclarationExtensions declarationExtensions, IMetricsProcessor metricsProcessor){
		this.issueAcceptor = issueAcceptor;
		this.theJenaModel = theJenaModel;
		this.declarationExtensions = declarationExtensions;
		this.metricsProcessor  = metricsProcessor;
	}
	
	public boolean validate(Expression expr, String xsdType, String op, StringBuilder errorMessageBuilder) {
		List<String> operations = Arrays.asList(op.split("\\s+"));
		TypeCheckInfo exprTypeCheckInfo;
		try {
			exprTypeCheckInfo = getType(expr);		
			ConceptName numberLiteralConceptName = new ConceptName(XSD.xint.getURI());
			numberLiteralConceptName.setType(ConceptType.RDFDATATYPE);
			TypeCheckInfo xsdTypeCheckInfo =  new TypeCheckInfo(numberLiteralConceptName, numberLiteralConceptName, this, null);				
			if(!compareTypes(operations, expr, null, exprTypeCheckInfo, xsdTypeCheckInfo)){
				createErrorMessage(errorMessageBuilder, exprTypeCheckInfo, xsdTypeCheckInfo, op);
				return false;
			}
		} catch (InvalidNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TranslationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DontTypeCheckException e) {
			return true;
		}
		return true;
	}
	
	public boolean validate(BinaryOperation expression, StringBuilder errorMessageBuilder) {
		setDefaultContext(expression);
		Expression leftExpression = expression.getLeft();
		Expression rightExpression = expression.getRight();
		List<String> operations = Arrays.asList(expression.getOp().split("\\s+"));
		
		if(skipOperations(operations)){
			return true;
		}
		
		try {	
			TypeCheckInfo leftTypeCheckInfo = getType(leftExpression);
			TypeCheckInfo rightTypeCheckInfo = getType(rightExpression);
			if(!compareTypes(operations, leftExpression, rightExpression, leftTypeCheckInfo, rightTypeCheckInfo)){
				createErrorMessage(errorMessageBuilder, leftTypeCheckInfo, rightTypeCheckInfo, expression.getOp());
				return false;
			}
			return true;
		} catch (InvalidNameException e) {
			issueAcceptor.addError("An invalid name exception occurred while type-checking this expression.", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (TranslationException e) {
			issueAcceptor.addError("A translation exception exception occurred while type-checking this expression.", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (URISyntaxException e) {
			issueAcceptor.addError("An URI syntax exception occurred while type-checking this expression.", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (IOException e) {
			issueAcceptor.addError("An IO exception occurred while type-checking this expression.", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (ConfigurationException e) {
			issueAcceptor.addError("A configuration exception occurred while type-checking this expression.", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (NullPointerException e){
			//issueAcceptor.addError("A null pointer exception occurred while type-checking this expression.", expression);
		} catch (DontTypeCheckException e) {
			return true;
		}
		return false;
	}
	
	public boolean validate(Expression leftExpression, Expression rightExpression, String op, StringBuilder errorMessageBuilder) {
		List<String> operations = Arrays.asList(op.split("\\s+"));
		TypeCheckInfo leftTypeCheckInfo = null;
		TypeCheckInfo rightTypeCheckInfo = null;
		try {	
			leftTypeCheckInfo = getType(leftExpression);
		} catch (InvalidNameException e) {
			issueAcceptor.addError("An invalid name exception occurred while type-checking this expression.", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (TranslationException e) {
			issueAcceptor.addError("A translation exception exception occurred while type-checking this expression.", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (URISyntaxException e) {
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			issueAcceptor.addError("An URI syntax exception occurred while type-checking this expression.", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (IOException e) {
			issueAcceptor.addError("An IO exception occurred while type-checking this expression.", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (ConfigurationException e) {
			issueAcceptor.addError("A configuration exception occurred while type-checking this expression.", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (NullPointerException e){
			//issueAcceptor.addError("A null pointer exception occurred while type-checking this expression.", expression);
		} catch (DontTypeCheckException e) {
			return true;
		}
		try {	
			rightTypeCheckInfo = getType(rightExpression);
		} catch (InvalidNameException e) {
			issueAcceptor.addError("An invalid name exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (TranslationException e) {
			issueAcceptor.addError("A translation exception exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (URISyntaxException e) {
			issueAcceptor.addError("An URI syntax exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (IOException e) {
			issueAcceptor.addError("An IO exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (ConfigurationException e) {
			issueAcceptor.addError("A configuration exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			e.printStackTrace();
		} catch (NullPointerException e){
			//issueAcceptor.addError("A null pointer exception occurred while type-checking this expression.", expression);
		} catch (DontTypeCheckException e) {
			return true;
		}
		try {
			if(!compareTypes(operations, leftExpression, rightExpression, leftTypeCheckInfo, rightTypeCheckInfo)){
				createErrorMessage(errorMessageBuilder, leftTypeCheckInfo, rightTypeCheckInfo, op);
				return false;
			}
		} catch (InvalidNameException e) {
			issueAcceptor.addError("An invalid name exception occurred while type-checking this expression.", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.INVALID_EXPRESSION_URI);
			}
			e.printStackTrace();
		} catch (DontTypeCheckException e) {
			return true;
		}
		return true;
	}

	private void createErrorMessage(StringBuilder errorMessageBuilder, TypeCheckInfo leftTypeCheckInfo, TypeCheckInfo rightTypeCheckInfo, String operation) {
		String leftName = leftTypeCheckInfo != null ? leftTypeCheckInfo.expressionType != null ? leftTypeCheckInfo.expressionType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		String leftType = leftTypeCheckInfo != null ? leftTypeCheckInfo.typeCheckType != null ? leftTypeCheckInfo.typeCheckType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		String leftRange = leftTypeCheckInfo != null ? leftTypeCheckInfo.rangeValueType != null ? leftTypeCheckInfo.rangeValueType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		String rightName = rightTypeCheckInfo != null ? rightTypeCheckInfo.expressionType != null ? rightTypeCheckInfo.expressionType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		String rightType = rightTypeCheckInfo != null ? rightTypeCheckInfo.typeCheckType != null ? rightTypeCheckInfo.typeCheckType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		String rightRange = rightTypeCheckInfo != null ? rightTypeCheckInfo.rangeValueType != null ? rightTypeCheckInfo.rangeValueType.toString() : "UNIDENTIFIED" : "UNIDENTIFIED";
		
		if (!leftName.equals(leftType)) {
			errorMessageBuilder.append("Element '" + leftName + "'");
			errorMessageBuilder.append(" of type '" + leftType + "'");
		}
		else {
			errorMessageBuilder.append("Element of type '" + leftName + "");
		}
		if(!leftRange.equals("UNIDENTIFIED")){
			errorMessageBuilder.append(", with a range of '" + leftRange + "',");
		}
		if (comparisonOperators.contains(operation)) {
			errorMessageBuilder.append(" cannot be compared (" + operation + ") with ");
		}
		else {
			errorMessageBuilder.append(" cannot operate (" + operation + ") with ");
		}
		if (!rightName.equals(rightType)) {
			errorMessageBuilder.append("element '" + rightName + "'");
			errorMessageBuilder.append(" of type " + rightType + "'");
		}
		else {
			errorMessageBuilder.append("element of type '" + rightName + "'");
		}
		if(!rightRange.equals("UNIDENTIFIED")){
			errorMessageBuilder.append(", with a range of '" + rightRange + "'");
		}
		errorMessageBuilder.append(".");
	}

	protected boolean skipOperations(List<String> operations) {
		if(operations.contains("and") || operations.contains("or")){
			return true;
		}
		return false;
	}

	protected TypeCheckInfo getType(Expression expression) throws InvalidNameException, TranslationException, URISyntaxException, IOException, ConfigurationException, DontTypeCheckException{
		if (expressionsValidated.containsKey(expression)) {
			return expressionsValidated.get(expression);
		}
		if(expression instanceof Name){
			return getType((Name)expression);
		}
		else if(expression instanceof Declaration){
			SadlTypeReference decltype = ((Declaration)expression).getType();
			return getType(decltype);
			//Need to return passing case for time being
//			ConceptName declarationConceptName = new ConceptName("TODO");
//			return new TypeCheckInfo(declarationConceptName, declarationConceptName);
		}
		else if(expression instanceof StringLiteral){
			ConceptName stringLiteralConceptName = new ConceptName(XSD.xstring.getURI());
			stringLiteralConceptName.setType(ConceptType.RDFDATATYPE);
			return new TypeCheckInfo(stringLiteralConceptName, stringLiteralConceptName, this, expression);
		}
		else if(expression instanceof NumberLiteral || expression instanceof Unit){
			BigDecimal value;
			if (expression instanceof Unit) {
				value = ((Unit)expression).getValue().getValue();
			}
			else {
				value = ((NumberLiteral)expression).getValue();
			}
			ConceptName numberLiteralConceptName = null;
			if (value.stripTrailingZeros().scale() <= 0 || value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
				numberLiteralConceptName = new ConceptName(XSD.xint.getURI());
			}
			else {
				numberLiteralConceptName = new ConceptName(XSD.decimal.getURI());
			}
			numberLiteralConceptName.setType(ConceptType.RDFDATATYPE);
			return new TypeCheckInfo(numberLiteralConceptName, numberLiteralConceptName, this, expression);
		}
		else if(expression instanceof BooleanLiteral){
			ConceptName booleanLiteralConceptName = new ConceptName(XSD.xboolean.getURI());
			booleanLiteralConceptName.setType(ConceptType.RDFDATATYPE);
			return new TypeCheckInfo(booleanLiteralConceptName, booleanLiteralConceptName, this, expression);
		}
		else if(expression instanceof Constant){
			//What do we do about the rest of the constants?
			/*'--' | 'a'? 'type' ;*/
			String constant = ((Constant) expression).getConstant();	
			if(constant.equals("PI")){
				ConceptName constantConceptName = new ConceptName(XSD.decimal.getURI());
				constantConceptName.setType(ConceptType.DATATYPEPROPERTY);
				return new TypeCheckInfo(constantConceptName, constantConceptName, this, expression);
			}
			else if(constant.equals("length") || constant.equals("count") ||
					   constant.equals("index")){
						ConceptName constantConceptName = new ConceptName(XSD.xint.getURI());
						constantConceptName.setType(ConceptType.DATATYPEPROPERTY);
						return new TypeCheckInfo(constantConceptName, constantConceptName, this, expression);
					}
			else if(constant.contains("element") && (constant.contains("first") || constant.contains("last"))){
				//Handle list types???
				ConceptName declarationConceptName = new ConceptName("TODO");
				return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
			}
			else if(constant.equals("None")){
				ConceptName constantConceptName = new ConceptName(constant);
				constantConceptName.setType(ConceptType.INDIVIDUAL);
				return new TypeCheckInfo(constantConceptName, constantConceptName, this, expression);
			}
		}
		else if(expression instanceof ValueTable){
			ConceptName declarationConceptName = new ConceptName("TODO");
			return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
		}
		else if(expression instanceof PropOfSubject){
			return getType((PropOfSubject)expression);
		}
		else if(expression instanceof SubjHasProp){
			return getType(((SubjHasProp)expression).getLeft());
		}
		else if(expression instanceof UnaryExpression){
			return getType(((UnaryExpression) expression).getExpr());
		}
		else if(expression instanceof ElementInList){
			Expression el = ((ElementInList)expression).getElement();
			if (el instanceof PropOfSubject) {
				return getType(((PropOfSubject)el).getRight());
			}
			else {
				issueAcceptor.addError("Unhandled element type in element in list construct: " + el.getClass().getCanonicalName() + "; please report", expression);
				if (metricsProcessor != null) {
					metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
				}
			}
		}
		else if(expression instanceof BinaryOperation){
			List<String> operations = Arrays.asList(((BinaryOperation) expression).getOp().split("\\s+"));
			TypeCheckInfo leftTypeCheckInfo = getType(((BinaryOperation) expression).getLeft());
			TypeCheckInfo rightTypeCheckInfo = getType(((BinaryOperation) expression).getRight());
			TypeCheckInfo binopreturn = combineTypes(operations, ((BinaryOperation) expression).getLeft(), ((BinaryOperation) expression).getRight(), 
					leftTypeCheckInfo, rightTypeCheckInfo);
			if (binopreturn != null) {
				return binopreturn;
			}
			if (numericOperators.contains(((BinaryOperation) expression).getOp())) {
				ConceptName decimalLiteralConceptName = new ConceptName(XSD.decimal.getURI());
				return new TypeCheckInfo(decimalLiteralConceptName, decimalLiteralConceptName, this, expression);
			}
			else {
				// by default assume boolean binary operation
				ConceptName booleanLiteralConceptName = new ConceptName(XSD.xboolean.getURI());
				return new TypeCheckInfo(booleanLiteralConceptName, booleanLiteralConceptName, this, expression);
			}
		}
		else if (expression instanceof Sublist) {
			// the type is the type of the list
			return getType((((Sublist)expression).getList()));
		}
		
		issueAcceptor.addError("This expression cannot be decomposed into a known type", expression);
		if (metricsProcessor != null) {
			metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
		}
		return null;
	}

	private TypeCheckInfo getType(SadlTypeReference expression) throws DontTypeCheckException {
		if (expression instanceof SadlIntersectionType) {
			return getType((SadlIntersectionType)expression);
		}
		else if (expression instanceof SadlPrimitiveDataType) {
			return getType((SadlPrimitiveDataType)expression);			
		}
		else if (expression instanceof SadlPropertyCondition) {
			return getType((SadlPropertyCondition)expression);
		}
		else if (expression instanceof SadlSimpleTypeReference) {
			return getType((SadlSimpleTypeReference)expression);
		}
		else if (expression instanceof SadlUnionType) {
			return getType((SadlUnionType)expression);
		}
		issueAcceptor.addError("Unexpected type reference type: " + expression.getClass().getCanonicalName(), expression);
		if (metricsProcessor != null) {
			metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
		}
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
	}
	
	private TypeCheckInfo getType(SadlIntersectionType expression) {
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);		
	}

	private TypeCheckInfo getType(SadlPrimitiveDataType expression) {
		TypeCheckInfo tci = getType(expression.getPrimitiveType());
		tci.setContext(this, expression);
		return tci;
	}

	private TypeCheckInfo getType(SadlDataType primitiveType) {
		String nm = primitiveType.getName();
		ConceptName cn = new ConceptName(XSD.getURI() + nm);
		return new TypeCheckInfo(cn, cn, this, null);
	}

	private TypeCheckInfo getType(SadlPropertyCondition expression) {
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);		
	}

	private TypeCheckInfo getType(SadlSimpleTypeReference expression) throws DontTypeCheckException {
		return getType(expression.getType());
	}

	private TypeCheckInfo getType(SadlUnionType expression) {
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);		
	}

	private TypeCheckInfo getType(PropOfSubject expression) throws InvalidNameException, TranslationException, URISyntaxException, IOException, ConfigurationException, DontTypeCheckException{
		List<String> operations = Collections.<String>emptyList();
		TypeCheckInfo predicateTypeCheckInfo = null;
		TypeCheckInfo subjectTypeCheckInfo = null;
		Expression predicate = expression.getLeft();
		Expression subject = expression.getRight();
		
		if (predicate instanceof Constant) {
			String cnstval = ((Constant)predicate).getConstant();
			if (cnstval.equals("length") || cnstval.equals("count") || cnstval.equals("index")) {
				ConceptName nlcn = new ConceptName(XSD.xint.getURI());
				nlcn.setType(ConceptType.RDFDATATYPE);
				return new TypeCheckInfo(nlcn, nlcn, this, expression);
			}
//			else if (cnstval.equals("count")) {
//				if (subject instanceof PropOfSubject) {
//					predicate = ((PropOfSubject)subject).getLeft();
//					subject = ((PropOfSubject)subject).getRight();
//				}
//			}
//			else if (cnstval.equals("index")) {
//				if (subject instanceof PropOfSubject) {
//					predicate = ((PropOfSubject)subject).getLeft();
//					subject = ((PropOfSubject)subject).getRight();
//				}
//			}
			else if (cnstval.equals("first element")) {
			}
			else if (cnstval.equals("last element")) {
			}
			else {
				issueAcceptor.addError("Unhandled constant property", expression);
				if (metricsProcessor != null) {
					metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
				}
			}
		}
		// check for AllValuesFrom restriction before defaulting to checking property range
		TypeCheckInfo avfTypeCheckInfo = getTypeFromRestriction(subject, predicate);
		if (avfTypeCheckInfo != null) {
			return avfTypeCheckInfo;
		}
		return predicateTypeCheckInfo = getType(predicate);
//		subjectTypeCheckInfo = getType(subject);
//		return combineTypes(operations, predicate, subject, 
//				predicateTypeCheckInfo, subjectTypeCheckInfo);
	}
	
	private TypeCheckInfo getTypeFromRestriction(Expression subject, Expression predicate) {
		if (subject instanceof Name && predicate instanceof Name) {
			String subjuri = declarationExtensions.getConceptUri(((Name)subject).getName());
			Resource subj = theJenaModel.getResource(subjuri);
			if (subj != null) {
				if (subj.canAs(Individual.class)) {
					subj = subj.as(Individual.class).getRDFType(true);
				}
				if (subj != null && subj.canAs(OntClass.class)){ 
					String propuri = declarationExtensions.getConceptUri(((Name)predicate).getName());
					Property prop = theJenaModel.getProperty(propuri);
					StmtIterator sitr = theJenaModel.listStatements(null, OWL.onProperty, prop);
					while (sitr.hasNext()) {
						Statement stmt = sitr.nextStatement();
						Resource sr = stmt.getSubject();
						if (sr.canAs(OntClass.class) && subj.as(OntClass.class).hasSuperClass(sr.as(OntClass.class))) {
							if (sr.as(OntClass.class).asRestriction().isAllValuesFromRestriction()) {
								Resource avf = sr.as(OntClass.class).asRestriction().asAllValuesFromRestriction().getAllValuesFrom();
								if (avf.isLiteral()) {
									
								}
								else {
									
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private TypeCheckInfo getType(Name expression) throws InvalidNameException, TranslationException, URISyntaxException, IOException, ConfigurationException, DontTypeCheckException {
		SadlResource qnm =expression.getName();
		if (qnm.eIsProxy()) {
			// this is a proxy so we don't know its type
			issueAcceptor.addWarning("Function is not defined so return type is unknown, can't do type checking", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.WARNING_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			throw new DontTypeCheckException();
		}
		return getType(qnm);
	}
	
	protected TypeCheckInfo getType(SadlResource qnm) throws DontTypeCheckException{
		String conceptUri = declarationExtensions.getConceptUri(qnm);
		EObject expression = qnm.eContainer();
		if (conceptUri == null) {
			issueAcceptor.addError("Unidentified expression", (expression != null ? expression : qnm));
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
		}
		
		OntConceptType conceptType;
		try {
			conceptType = declarationExtensions.getOntConceptType(qnm);
		} catch (CircularDefinitionException e) {
			conceptType = e.getDefinitionType();
			issueAcceptor.addError(e.getMessage(), expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
		}
		if(conceptType.equals(OntConceptType.CLASS)){
			ConceptName conceptName = new ConceptName(conceptUri);
			conceptName.setType(ConceptType.ONTCLASS);
			return new TypeCheckInfo(conceptName, conceptName, this, expression);
		}
		else if(conceptType.equals(OntConceptType.DATATYPE_PROPERTY)){
			return getNameProperty(ConceptType.DATATYPEPROPERTY, conceptUri, expression);
		}
		else if(conceptType.equals(OntConceptType.CLASS_PROPERTY)){
			return getNameProperty(ConceptType.OBJECTPROPERTY, conceptUri, expression);
		}
		else if (conceptType.equals(OntConceptType.RDF_PROPERTY)) {
			TypeCheckInfo rdfpropcheckinfo = getNameProperty(ConceptType.RDFDATATYPE, conceptUri, expression);
			if (rdfpropcheckinfo != null) {
				return rdfpropcheckinfo;
			}
			throw new DontTypeCheckException();
		}
		else if(conceptType.equals(OntConceptType.INSTANCE)){
			//Direct type to which the instance belongs
			Individual individual = theJenaModel.getIndividual(conceptUri);
			if(individual == null){
				issueAcceptor.addError("Unidentified expression", expression);
				if (metricsProcessor != null) {
					metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
				}
				return null;
			}
			ConceptName instConceptName = new ConceptName(conceptUri);
			instConceptName.setType(ConceptType.INDIVIDUAL);
//			Resource ontResource = individual.getRDFType(true);
//			if(!ontResource.isURIResource()){
//				//Unhandled condition
//				//TODO
//				ConceptName declarationConceptName = new ConceptName("TODO");
//				return new TypeCheckInfo(declarationConceptName, declarationConceptName);
//			}
//			String uriOfTypeToBeReturned = ontResource.getURI();
//			ConceptName conceptName = new ConceptName(uriOfTypeToBeReturned);
//			conceptName.setType(ConceptType.ONTCLASS);
			return new TypeCheckInfo(instConceptName, instConceptName, this, expression);
		}
		else if(conceptType.equals(OntConceptType.VARIABLE)){
			return getVariableType(ConceptType.VARIABLE, conceptUri, expression);

		}
		else if(conceptType.equals(OntConceptType.ANNOTATION_PROPERTY)){
			//This matches any type.
			ConceptName declarationConceptName = new ConceptName("TODO");
			return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
		}
		
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
	}
	
	protected TypeCheckInfo getNameProperty(ConceptType propertyType, String conceptUri, EObject expression) throws DontTypeCheckException {
		OntProperty property = theJenaModel.getOntProperty(conceptUri);
		if(property == null){
			issueAcceptor.addError("Unidentified expression", expression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			return null;
		}
		ConceptName propConceptName = new ConceptName(conceptUri);
		propConceptName.setType(propertyType);
		return getTypeInfoFromRange(propConceptName, property, expression);
	}

	private TypeCheckInfo getTypeInfoFromRange(ConceptName propConceptName, Property property,
			EObject expression) throws DontTypeCheckException {
		ConceptType propertyType = propConceptName.getType();
		StmtIterator sitr = theJenaModel.listStatements(property, RDFS.range, (RDFNode)null);
		if (sitr.hasNext()) {
			RDFNode first = sitr.next().getObject();
			if(first.isURIResource()){
				ConceptName rangeConceptName = new ConceptName(first.asResource().getURI());
				if (propertyType.equals(ConceptType.DATATYPEPROPERTY)) {
					rangeConceptName.setType(ConceptType.RDFDATATYPE);
					OntResource range;
					try {
						range = theJenaModel.getOntResource(rangeConceptName.getUri());
						if (theJenaModel.listStatements(range, RDF.type, RDFS.Datatype).hasNext()) {
							// this is a user-defined datatype
							RDFNode rngEC = range.listPropertyValues(OWL.equivalentClass).next();
							if (rngEC != null && rngEC.canAs(OntResource.class)) {
								RDFNode baseType = rngEC.as(OntResource.class).listPropertyValues(OWL2.onDatatype).next();
								if (baseType != null && baseType.isURIResource()) {
									ConceptName baseTypeConceptName = new ConceptName(baseType.asResource().getURI());
									baseTypeConceptName.setType(ConceptType.RDFDATATYPE);
									return new TypeCheckInfo(propConceptName, baseTypeConceptName, this, expression);
								}
							}
						}
						else {
							rangeConceptName.setRangeValueType(propConceptName.getRangeValueType());
						}
					} catch (InvalidNameException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					rangeConceptName.setType(ConceptType.ONTCLASS);
				}
				sitr.close();
				List<ConceptName> impliedProperties = getImpliedProperties(first.asResource());
				return new TypeCheckInfo(propConceptName, rangeConceptName, impliedProperties, this, expression);
			}
			else {
				// this will be the case for unnamed lists
				if (first.canAs(OntClass.class)){
					if (((OntClass)first.as(OntClass.class)).hasSuperClass(theJenaModel.getOntResource(JenaBasedSadlModelProcessor.SADL_LIST_MODEL_LIST_URI))) {
						ExtendedIterator<OntClass> eitr = ((OntClass)first.as(OntClass.class)).listSuperClasses(true);
						while (eitr.hasNext()) {
							OntClass cls = eitr.next();
							if (cls.isRestriction()) {
								if (cls.canAs(AllValuesFromRestriction.class)) {
									if (((AllValuesFromRestriction)cls.as(AllValuesFromRestriction.class)).onProperty(theJenaModel.getProperty(JenaBasedSadlModelProcessor.SADL_LIST_MODEL_FIRST_URI))) {
										Resource avf = ((AllValuesFromRestriction)cls.as(AllValuesFromRestriction.class)).getAllValuesFrom();
										eitr.close();
										if (avf.isURIResource()) {
											List<ConceptName> impliedProperties = getImpliedProperties(avf.asResource());
											ConceptName rangeConceptName = new ConceptName(avf.getURI());
											if (propertyType.equals(ConceptType.DATATYPEPROPERTY)) {
												rangeConceptName.setType(ConceptType.RDFDATATYPE);
												rangeConceptName.setRangeValueType(propConceptName.getRangeValueType());
											}
											else {
												rangeConceptName.setType(ConceptType.ONTCLASS);
											}
											return new TypeCheckInfo(propConceptName, rangeConceptName, impliedProperties, this, expression);
										}
									}
								}
							}
						}
						
					}
				}
			}
		}
		else {
			StmtIterator sitr2 = theJenaModel.listStatements(property, RDFS.subPropertyOf, (RDFNode)null);
			while (sitr2.hasNext()) {
				RDFNode psuper = sitr2.next().getObject();
				if (psuper.isLiteral()) {
					TypeCheckInfo superTCInfo = getNameProperty(propertyType, psuper.asResource().getURI(), expression);
					if (superTCInfo != null) {
						return superTCInfo;
					}
				}
			}
		}
		return null;
	}

	private List<ConceptName> getImpliedProperties(Resource first) {
		StmtIterator sitr = theJenaModel.listStatements(first, theJenaModel.getProperty(JenaBasedSadlModelProcessor.SADL_IMPLICIT_MODEL_IMPLIED_PROPERTY_URI), (RDFNode)null);
		if (sitr.hasNext()) {
			List<ConceptName> retlst = new ArrayList<ConceptName>();
			while (sitr.hasNext()) {
				RDFNode obj = sitr.nextStatement().getObject();
				if (obj.isURIResource()) {
					retlst.add(new ConceptName(obj.asResource().getURI()));
				}
			}
			return retlst;
		}
		return null;
	}

	private boolean isRangeKlugyDATASubclass(OntResource rsrc) {
		if (rsrc.getURI().endsWith("#DATA")) {
			return true;
		}
		if (rsrc.canAs(OntClass.class)){
			ExtendedIterator<OntClass> itr = rsrc.as(OntClass.class).listSuperClasses();
			while (itr.hasNext()) {
				OntClass spr = itr.next();
				if (spr.isURIResource() && spr.getURI().endsWith("#DATA")) {
					return true;
				}
			}
		}
		return false;
	}

	protected TypeCheckInfo getVariableType(ConceptType variable, String conceptUri, EObject expression) throws DontTypeCheckException {
		//Needs filled in for Requirements extension
		ConceptName declarationConceptName = new ConceptName("TODO");
		return new TypeCheckInfo(declarationConceptName, declarationConceptName, this, expression);
	}
	
	private TypeCheckInfo combineTypes(List<String> operations, Expression leftExpression, Expression rightExpression,
			TypeCheckInfo leftTypeCheckInfo, TypeCheckInfo rightTypeCheckInfo) throws InvalidNameException, DontTypeCheckException {
		if(!compareTypes(operations, leftExpression, rightExpression, leftTypeCheckInfo, rightTypeCheckInfo)){
			return null;
		}
		
		if(comparisonOperators.containsAll(operations)){
			ConceptName booleanLiteralConceptName = new ConceptName(XSD.xboolean.getURI());
			booleanLiteralConceptName.setType(ConceptType.DATATYPEPROPERTY);
			return new TypeCheckInfo(booleanLiteralConceptName, booleanLiteralConceptName, this, leftExpression.eContainer());
		}
		else{
			return leftTypeCheckInfo;
		}
	}

	/**
	 * Compare two TypeCheckInfo structures
	 * @param operations
	 * @param leftExpression
	 * @param rightExpression
	 * @param leftTypeCheckInfo
	 * @param rightTypeCheckInfo
	 * @return return true if they pass type check comparison else false
	 * @throws InvalidNameException
	 * @throws DontTypeCheckException 
	 */
	private boolean compareTypes(List<String> operations, Expression leftExpression, Expression rightExpression,
			TypeCheckInfo leftTypeCheckInfo, TypeCheckInfo rightTypeCheckInfo) throws InvalidNameException, DontTypeCheckException {
		ConceptIdentifier leftConceptIdentifier = leftTypeCheckInfo != null ? leftTypeCheckInfo.getTypeCheckType(): null;
		ConceptIdentifier rightConceptIdentifier = rightTypeCheckInfo != null ? rightTypeCheckInfo.getTypeCheckType() : null; 
		if (leftConceptIdentifier == null) {
			issueAcceptor.addError("Type comparison not possible", leftExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			return false;
		}
		else if(rightConceptIdentifier == null){
			issueAcceptor.addError("Type comparison not possible", rightExpression);
			if (metricsProcessor != null) {
				metricsProcessor.addMarker(null, MetricsProcessor.ERROR_MARKER_URI, MetricsProcessor.UNCLASSIFIED_FAILURE_URI);
			}
			return false;
		}
		else if (leftConceptIdentifier.toString().equals("None") || rightConceptIdentifier.toString().equals("None") ||
				 leftConceptIdentifier.toString().equals("TODO") || rightConceptIdentifier.toString().equals("TODO")) {
			// Can't type-check on "None" as it represents that it doesn't exist.
			//TODO
			return true;
		}
		else if (!compatibleTypes(operations, leftExpression, rightExpression, leftTypeCheckInfo, rightTypeCheckInfo)) {
			if (leftTypeCheckInfo.getImplicitProperties() != null || rightTypeCheckInfo.getImplicitProperties() != null) {
				return compareTypesUsingImpliedProperties(operations, leftExpression, rightExpression, leftTypeCheckInfo, rightTypeCheckInfo);
			}
			return false;
		}
		return true;
	}

	private boolean compareTypesUsingImpliedProperties(List<String> operations, Expression leftExpression,
			Expression rightExpression, TypeCheckInfo leftTypeCheckInfo, TypeCheckInfo rightTypeCheckInfo) throws InvalidNameException, DontTypeCheckException {
		if (leftTypeCheckInfo.getImplicitProperties() != null) {
			Iterator<ConceptName> litr = leftTypeCheckInfo.getImplicitProperties().iterator();
			while (litr.hasNext()) {
				ConceptName cn = litr.next();
				Property prop = theJenaModel.getProperty(cn.getUri());
				if (prop.canAs(ObjectProperty.class)) {
					cn.setType(ConceptType.OBJECTPROPERTY);
				}
				else if (prop.canAs(DatatypeProperty.class)) {
					cn.setType(ConceptType.DATATYPEPROPERTY);
				}
				else {
					cn.setType(ConceptType.RDFPROPERTY);
				}
				TypeCheckInfo newltci = getTypeInfoFromRange(cn, prop, leftExpression);
				if (compareTypes(operations, leftExpression, rightExpression, newltci, rightTypeCheckInfo)) {
					return true;
				}
			}
		}
		else if (rightTypeCheckInfo.getImplicitProperties() != null) {
			Iterator<ConceptName> ritr = rightTypeCheckInfo.getImplicitProperties().iterator();
			while (ritr.hasNext()) {
				ConceptName cn = ritr.next();
				Property prop = theJenaModel.getProperty(cn.getUri());
				if (prop.canAs(ObjectProperty.class)) {
					cn.setType(ConceptType.OBJECTPROPERTY);
				}
				else if (prop.canAs(DatatypeProperty.class)) {
					cn.setType(ConceptType.DATATYPEPROPERTY);
				}
				else {
					cn.setType(ConceptType.RDFPROPERTY);
				}
				TypeCheckInfo newrtci = getTypeInfoFromRange(cn, prop, rightExpression);
				if (compareTypes(operations, leftExpression, rightExpression, leftTypeCheckInfo, newrtci)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean compatibleTypes(List<String> operations, Expression leftExpression, Expression rightExpression,
									TypeCheckInfo leftTypeCheckInfo, TypeCheckInfo rightTypeCheckInfo) throws InvalidNameException{
		
		if ((leftTypeCheckInfo.getRangeValueType() == null && rightTypeCheckInfo.getRangeValueType() != null && !rightTypeCheckInfo.getRangeValueType().equals(RangeValueType.CLASS_OR_DT)) || 
			(leftTypeCheckInfo.getRangeValueType() != null && !leftTypeCheckInfo.getRangeValueType().equals(RangeValueType.CLASS_OR_DT) && rightTypeCheckInfo.getRangeValueType() == null) ||
			(leftTypeCheckInfo.getRangeValueType() != null && rightTypeCheckInfo.getRangeValueType() != null && !(leftTypeCheckInfo.getRangeValueType().equals(rightTypeCheckInfo.getRangeValueType())))) {
			return false;
		}
		
		ConceptIdentifier leftConceptIdentifier = leftTypeCheckInfo.getTypeCheckType();
		ConceptIdentifier rightConceptIdentifier = rightTypeCheckInfo.getTypeCheckType();
		if (leftConceptIdentifier == null || rightConceptIdentifier == null) {
			return false;
		}
		if (leftConceptIdentifier instanceof ConceptName && rightConceptIdentifier instanceof ConceptName) {
			ConceptName leftConceptName = (ConceptName) leftConceptIdentifier;
			ConceptName rightConceptName = (ConceptName) rightConceptIdentifier;
			
			if (leftConceptName.equals(rightConceptName)) {
				return true;
			}
			else if (leftConceptName.getType() == null || rightConceptName.getType() == null) {
				if (rightConceptName.getType() == null && leftConceptName.getType() == null) {
					return true;
				}
				else {
					return false;
				}
			}
			else if (leftConceptName.getType().equals(ConceptType.RDFDATATYPE) &&
					  rightConceptName.getType().equals(ConceptType.RDFDATATYPE)) {
				if(leftConceptName.getUri().equals(rightConceptName.getUri())){
					return true;
				}
				else if (isInteger(leftConceptName) && isInteger(rightConceptName)) {
					return true;
				}
				else if(isDecimal(leftConceptName) && isInteger(rightConceptName)){
					return true;
				}
				else if(isInteger(leftConceptName) && isDecimal(rightConceptName)){
					// TODO does this need to be restricted to certain operators? This should work for numerical comparison...
					return true;
				}
				else if(isDecimal(leftConceptName) && isDecimal(rightConceptName)){
					return true;
				}
			}
			else if (leftConceptName.getType().equals(ConceptType.DATATYPEPROPERTY) &&
					  rightConceptName.getType().equals(ConceptType.DATATYPEPROPERTY)) {
				if(leftConceptName.getUri().equals(rightConceptName.getUri())){
					return true;
				}
			}
			else if(leftConceptName.getType().equals(ConceptType.OBJECTPROPERTY) &&
					 rightConceptName.getType().equals(ConceptType.OBJECTPROPERTY)){
				if(leftConceptName.getUri().equals(rightConceptName.getUri())){
					return true;
				}
			}
			else if (leftConceptName.getType().equals(ConceptType.ONTCLASS) &&
					rightConceptName.getType().equals(ConceptType.ONTCLASS)) {
				//How do we determine if either is a sub/super class of the other?
				if(leftConceptName.getUri().equals(rightConceptName.getUri())){
					return true;
				}
				// these next two ifs are a little loose, but not clear how to determine which way the comparison should be? May need tightening... AWC 5/11/2016
				try {
					if (classIsSubclassOf(theJenaModel.getOntClass(leftConceptName.getUri()), theJenaModel.getOntResource(rightConceptName.getUri()), true)) {
						return true;
					}
					if (classIsSubclassOf(theJenaModel.getOntClass(rightConceptName.getUri()), theJenaModel.getOntResource(leftConceptName.getUri()), true)) {
						return true;
					}
				} catch (JenaProcessorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if ((leftConceptName.getType().equals(ConceptType.INDIVIDUAL) && rightConceptName.getType().equals(ConceptType.ONTCLASS))) {
				return instanceBelongsToClass(theJenaModel.getIndividual(leftConceptName.getUri()), theJenaModel.getOntClass(rightConceptName.getUri()));
			}
			else if ((leftConceptName.getType().equals(ConceptType.ONTCLASS) && rightConceptName.getType().equals(ConceptType.INDIVIDUAL))){
				return instanceBelongsToClass(theJenaModel.getIndividual(rightConceptName.getUri()), theJenaModel.getOntClass(leftConceptName.getUri()));
			}
			else if ((leftConceptName.getType().equals(ConceptType.INDIVIDUAL) && rightConceptName.getType().equals(ConceptType.INDIVIDUAL))){
				// TODO Is this the right way to compare for two individuals? 
				return instancesHaveCommonType(theJenaModel.getIndividual(leftConceptName.getUri()), theJenaModel.getIndividual(rightConceptName.getUri()));
			}
		}
		return false;
	}
	
	private boolean instancesHaveCommonType(Individual individualL, Individual individualR) {
		ExtendedIterator<Resource> lcitr = individualL.listRDFTypes(true);
		ExtendedIterator<Resource> rcitr = individualR.listRDFTypes(true);
		while (lcitr.hasNext()) {
			Resource lr = lcitr.next();
			while (rcitr.hasNext()) {
				Resource rr = rcitr.next();
				if (lr.equals(rr)) {
					lcitr.close();
					rcitr.close();
					return true;
				}
			}
		}
		return false;
	}

	private boolean instanceBelongsToClass(Individual individual, OntClass ontClass) {
		ExtendedIterator<Resource> citr = individual.listRDFTypes(false);
		while (citr.hasNext()) {
			Resource cls = citr.next();
			if (cls.isURIResource() && cls.getURI().equals(ontClass.getURI())) {
				return true;
			}
			else {
				// this may be a union or intersection class; how should this be handled?
				// TODO
			}
		}
		return false;
	}

	/**
	 * return true if the first argument class is a subclass of the second
	 * argument class
	 * 
	 * @param subcls
	 * @param cls
	 * @return
	 * @throws JenaProcessorException 
	 */
	public boolean classIsSubclassOf(OntClass subcls, OntResource cls, boolean rootCall) throws JenaProcessorException {
		return JenaBasedSadlModelProcessor.classIsSubclassOf(subcls, cls, rootCall, null);
//		if (subcls == null || cls == null) {
//			return false;
//		}
//		if (cls.isURIResource() && subcls.isURIResource()
//				&& cls.getURI().equals(subcls.getURI())) {
//			return true;
//		}
//		if (cls.isAnon()) {
//			if (cls.canAs(OntClass.class)) {
//				OntClass ocls = cls.as(OntClass.class);
//				if (ocls.isUnionClass()) {
//					UnionClass ucls = cls.as(UnionClass.class);
//					try {
//						ExtendedIterator<? extends OntClass> eitr = ucls
//								.listOperands();
//						while (eitr.hasNext()) {
//							OntClass uclsmember = eitr.next();
//							if (classIsSubclassOf(subcls, uclsmember, false)) {
//								eitr.close();
//								return true;
//							}
//						}
//					}
//					catch (Exception e) {
//						issueAcceptor.addError("Unexpected error during deep validation: apparent Union Class does not return operands.", getDefaultContext());
//					}
//				}
//			}
//		}
//		try {
//			if (cls.canAs(OntClass.class)) {
//				ExtendedIterator<OntClass> eitr = cls.as(OntClass.class).listSubClasses();
//				while (eitr.hasNext()) {
//					OntClass subClsOfCls = eitr.next();
//					if (subClsOfCls.equals(subcls)) {
//						eitr.close();
//						return true;
//					}
//					else {
//						if (classIsSubclassOf(subcls, subClsOfCls, false)) {
//							eitr.close();
//							return true;
//						}
//					}
//				}
//				eitr.close();
////				if (rootCall && classIsSuperClassOf(cls.as(OntClass.class), subcls)) {
////					return true;
////				}
//			}
//			if (subcls.isAnon()) {
//				if (subcls.isIntersectionClass()) {
//					IntersectionClass icls = subcls.asIntersectionClass();
//					try {
//						ExtendedIterator<? extends OntClass> eitr = icls.listOperands();
//						while (eitr.hasNext()) {
//							OntClass iclsmember = eitr.next();
//							if (classIsSubclassOf(cls.as(OntClass.class), iclsmember, false)) {
//								eitr.close();
//								return true;
//							}
//						}
//					}
//					catch (Exception e) {
//						issueAcceptor.addError("Unexpected error during deep validation: apparent Intersection Class does not return operands.", getDefaultContext());
//					}
//				}
//			}
//// TODO We need to look for equivalent classes that provide a definition for a subclass, 
////			e.g. Component is equivalent to System is class, (System and connectedTo someValueFrom Network) => Component subclass of System.
//			if (cls.canAs(OntClass.class)) {
////				SELECT ?eqClass 
////						WHERE {?class owl:equivalentClass ?eqClass}
//				ExtendedIterator<OntClass> eqitr = cls.as(OntClass.class).listEquivalentClasses();
//				while (eqitr.hasNext()) {
//					OntClass eqcls = eqitr.next();
//					if (classIsSubclassOf(subcls, eqcls, false)) {
//						return true;
//					}
//				}
//			}
////			if (subcls.hasSuperClass(cls, false)) {  // this doesn't work, don't know why awc 6/8/2012
////				return true;
////			}
////			else {
////				if (subcls.canAs(OntClass.class)) {
////					ExtendedIterator<OntClass> eitr = subcls.as(OntClass.class).listSuperClasses(false);
////					while (eitr.hasNext()) {
////						OntClass sprcls = eitr.next();
////						if (sprcls.equals(cls)) {
////							return true;
////						}
////					}
////				}
////			}
//		} catch (Throwable t) {
//			t.printStackTrace();
//		}
//		return false;
	}


	
	private boolean isInteger(ConceptIdentifier type) throws InvalidNameException {
		if (type instanceof ConceptName) {
			String uri = ((ConceptName)type).getUri();
			if (uri.equals(XSD.integer.getURI())) {
				return true;
			}
			else if (uri.equals(XSD.xint.getURI())) {
				return true;
			}
		}
		return false;
	}

	private boolean isDecimal(ConceptIdentifier type) throws InvalidNameException {
		if (type instanceof ConceptName) {
			String uri = ((ConceptName)type).getUri();
			if (uri.equals(XSD.xfloat.getURI()) || uri.equals(XSD.xdouble.getURI()) || uri.equals(XSD.decimal.getURI())) {
				return true;
			}
		}
		return false;
	}

	private EObject getDefaultContext() {
		return defaultContext;
	}

	private void setDefaultContext(EObject defaultContext) {
		this.defaultContext = defaultContext;
	}

}
