/************************************************************************
 * Copyright © 2007-2016 - General Electric Company, All Rights Reserved
 *
 * Project: SADL
 *
 * Description: The Semantic Application Design Language (SADL) is a
 * language for building semantic models and expressing rules that
 * capture additional domain knowledge. The SADL-IDE (integrated
 * development environment) is a set of Eclipse plug-ins that
 * support the editing and testing of semantic models using the
 * SADL language.
 *
 * This software is distributed "AS-IS" without ANY WARRANTIES
 * and licensed under the Eclipse Public License - v 1.0
 * which is available at http://www.eclipse.org/org/documents/epl-v10.php
 *
 ***********************************************************************/
package com.ge.research.sadl.processing

import org.eclipse.emf.common.util.Diagnostic
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
//import org.eclipse.xtend.lib.annotations.Data
import org.eclipse.xtext.util.IAcceptor
import org.eclipse.xtext.validation.CheckType
import org.eclipse.xtext.validation.DiagnosticConverterImpl
import org.eclipse.xtext.validation.FeatureBasedDiagnostic
import org.eclipse.xtext.validation.Issue

//@Data class ValidationAcceptor {
class ValidationAcceptor {
	
	IAcceptor<Issue> acceptor
	
	int errorCount = 0
	int warningCount = 0
	int infoCount = 0
	
	DiagnosticConverterImpl converter = new DiagnosticConverterImpl
	
	new(IAcceptor<Issue> accept) {
		acceptor = accept
	}
	
	def void addError(String message, EObject context) {
		addError(message, context, null)
	}
	
	def void addError(String message, EObject context, EStructuralFeature feature) {
		val diag = new FeatureBasedDiagnostic(Diagnostic.ERROR, message, context, feature, -1, CheckType.NORMAL, 'processor.issue')
		converter.convertValidatorDiagnostic(diag, acceptor)
		errorCount++
	}
	
	def void addWarning(String message, EObject context) {
		addWarning(message, context, null)
	}
	
	def void addWarning(String message, EObject context, EStructuralFeature feature) {
		val diag = new FeatureBasedDiagnostic(Diagnostic.WARNING, message, context, feature, -1, CheckType.NORMAL, 'processor.issue')
		converter.convertValidatorDiagnostic(diag, acceptor)
		warningCount++
	}
	
	def void addInfo(String message, EObject context) {
		addInfo(message, context, null)
	}
	
	def void addInfo(String message, EObject context, EStructuralFeature feature) {
		val diag = new FeatureBasedDiagnostic(Diagnostic.INFO, message, context, feature, -1, CheckType.NORMAL, 'processor.issue')
		converter.convertValidatorDiagnostic(diag, acceptor)
		infoCount++
	}
	
	def int getErrorCount() {
		return errorCount;
	}

	def void clearErrorCount() {
		errorCount = 0;
	}

	def int getWarningCount() {
		return warningCount;
	}

	def void clearWarningCount() {
		warningCount = 0;
	}

	def int getInfoCount() {
		return infoCount;
	}

	def void clearInfoCount() {
		infoCount = 0;
	}
	
	def void clearMarkerCount() {
		clearErrorCount();
		clearWarningCount();
		clearInfoCount();
	}
	
	def int[] getMarkerCount() {
		return #[errorCount, warningCount, infoCount]
	}
}