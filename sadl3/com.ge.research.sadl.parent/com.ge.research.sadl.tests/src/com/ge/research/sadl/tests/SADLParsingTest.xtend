
/************************************************************************
 * Copyright 2007-2016- General Electric Company, All Rights Reserved
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
package com.ge.research.sadl.tests

import org.junit.Test
import org.junit.Ignore

class SADLParsingTest extends AbstractSADLParsingTest {

	@Test 
	def void testSublist_01() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching var1 is true and var2 is true and var3 is false).
		'''.assertNoErrors
	}
	
	@Test 
	def void testSublist_02() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching value >= 42).
		'''.assertNoErrors
	}
	
	@Test 
	def void testSublist_03() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching index >= 42).
		'''.assertNoErrors
	}
	
	@Test 
	def void testSublist_04() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching type is Person).
		'''.assertNoErrors
	}
	
	@Test 
	def void testSublist_05() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching OtherList does not contain value).
		'''.assertNoErrors
	}
	
	@Test 
	def void testSublist_06() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: Items is (the sublist of list_of_items matching index >= 42 and type is Person and OtherList does not contain value).
		'''.assertNoErrors
	}
	
	@Test 
	def void testListTypes_01() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			MyPets is a Pet List.
		'''.assertNoErrors
	}
	
	@Test 
	def void testListTypes_02() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			GennysPriorizedPets is the Pet List [Spot, Lassie].
		'''.assertNoErrors
	}
	
	@Test 
	def void testListTypes_03() {
		'''
			uri "http://com.ge.research.sadl/sublists". 
			
			Test: MyPets is a Pet List.
		'''.assertNoErrors
	}

	@Test 
	def void testPropertyTypeOnly() {
		'''
			uri "http://com.ge.research.sadl/proptypeonly". 
			
			dtprop is a property with values of type data.
			objprop is a property with values of type class.
		'''.assertNoErrors
	}

	@Ignore
	@Test 
	def void testSubjHasPropCompareExpr() {
		'''
			 uri "http://sadl.org/testrule.sadl" alias testrule.
			 
			 Person is a class described by age with values of type int.
			 
			 Rule R1: if x is a Person and x has age > 30 then print("hi").
			 
			 Rule R2: if x is a Person and age of x > 30 then print("hi").
 		'''.assertNoErrors
	}
}
