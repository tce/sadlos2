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
/*
 * generated by Xtext 2.9.0-SNAPSHOT
 */
package com.ge.research.sadl.tests

import com.ge.research.sadl.sADL.SadlModel
import com.google.inject.Inject
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.util.ParseHelper
import org.eclipse.xtext.junit4.validation.ValidationTestHelper
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(SADLInjectorProvider)
abstract class AbstractSADLParsingTest{

	@Inject ParseHelper<SadlModel> parseHelper
	@Inject ValidationTestHelper validationTestHelper
	
	protected def void assertNoErrors(CharSequence text) {
		val model = parseHelper.parse(text)
		val issues = validationTestHelper.validate(model)
		if (issues.isEmpty)
			return;
		var String annotatedText = text.toString
		for (issue : issues.filter[isSyntaxError].sortBy[-offset]) {
			annotatedText = annotatedText.substring(0, issue.offset) + '''[«issue.message»]''' + annotatedText.substring(issue.offset)
		}
		Assert.assertEquals(text.toString, annotatedText)
	}
	
	protected def void assertErrors(CharSequence text, String[] errPartials) {
		val model = parseHelper.parse(text)
		val issues = validationTestHelper.validate(model)
		Assert.assertFalse(issues.isEmpty)
		Assert.assertEquals(issues.size, 1)
		for (err : errPartials) {
			var found = false
			for (issue : issues) {
				if (issue.message.contains(err)) {
					found = true
				}
			}
			Assert.assertTrue(found)
		}
	}
	
	def void assertAST(CharSequence text, (SadlModel)=>void assertion) {
		val model = parseHelper.parse(text)
		validationTestHelper.assertNoErrors(model)
		assertion.apply(model)
	}
	
	def String prependUri(CharSequence sequence) {
		return '''
			«model»
			«sequence»
		'''
	}
	
	protected def String model() {
		val name = Thread.currentThread.stackTrace.findFirst[className!=AbstractSADLParsingTest.simpleName].methodName
		return '''uri "http://sadl.org/TestRequrements/«name»" alias «name».'''
	}

}
