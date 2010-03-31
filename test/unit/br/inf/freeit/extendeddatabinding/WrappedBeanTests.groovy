package br.inf.freeit.extendeddatabinding

import org.springframework.beans.*
import org.springframework.beans.propertyeditors.*
import org.springframework.validation.*
import java.text.*
import java.beans.*


class WrappedBeanTests extends GroovyTestCase {
	WrappedBean wrappedBean
	
	void testSimpleGet() {
		wrappedBean.wrappedInstance = new Person(name:"John", birthday: new GregorianCalendar(2000, 9, 8).time, income: 10705.87, gender: Gender.MALE)
		assertEquals("John", wrappedBean.name)
		assertEquals("08/10/2000", wrappedBean.birthday)
		assertEquals("10.705,87", wrappedBean.income)
		assertEquals("MALE", wrappedBean.gender)
	}
	
	
	void testSimpleSet() {
		Person p = new Person()
		wrappedBean.wrappedInstance = p
		wrappedBean.name = "John"
		wrappedBean.birthday = "08/10/2000"
		wrappedBean.income = "10.705,87"
		wrappedBean.gender = "MALE"
		assertEquals("John", p.name)
		assertEquals(new GregorianCalendar(2000, 9, 8).time, p.birthday)
		assertEquals(new BigDecimal("10705.87"), p.income)
		assertEquals(Gender.MALE, p.gender);
	}

	void testNestedGet() {
		Person p1 = new Person(name:"John", birthday: new GregorianCalendar(2000, 9, 8).time, income: 10705.87, gender: Gender.MALE)
		Person p2 = new Person(friend:p1)
		
		wrappedBean.wrappedInstance = p2
		assertEquals("John", wrappedBean.friend.name)
		assertEquals("08/10/2000", wrappedBean.friend.birthday)
		assertEquals("10.705,87", wrappedBean.friend.income)
		assertEquals("MALE", wrappedBean.friend.gender)
	}


	void testNestedSet() {
		Person p = new Person(friend:new Person())
		wrappedBean.wrappedInstance = p
		wrappedBean.friend.name = "John"
		wrappedBean.friend.birthday = "08/10/2000"
		wrappedBean.friend.income = "10.705,87"
		wrappedBean.friend.gender = "MALE"
		assertEquals("John", p.friend.name)
		assertEquals(new GregorianCalendar(2000, 9, 8).time, p.friend.birthday)
		assertEquals(new BigDecimal("10705.87"), p.friend.income)
		assertEquals(Gender.MALE, p.friend.gender)
	}

	void testDeepNestedGet() {
		Person p1 = new Person(name:"John", birthday: new GregorianCalendar(2000, 9, 8).time, income: 10705.87, gender: Gender.MALE)
		Person p2 = new Person(friend:p1)
		Person p3 = new Person(friend:p2)
		Person p4 = new Person(friend:p3)

		wrappedBean.wrappedInstance = p4
		assertEquals("John", wrappedBean.friend.friend.friend.name)
		assertEquals("08/10/2000", wrappedBean.friend.friend.friend.birthday)
		assertEquals("10.705,87", wrappedBean.friend.friend.friend.income)
		assertEquals("MALE", wrappedBean.friend.friend.friend.gender)
	}

	void testDeepNestedSet() {
		Person p = new Person()
		p.friend = new Person()
		p.friend.friend = new Person()
		p.friend.friend.friend = new Person()
		wrappedBean.wrappedInstance = p
		wrappedBean.friend.friend.friend.name = "John"
		wrappedBean.friend.friend.friend.birthday = "08/10/2000"
		wrappedBean.friend.friend.friend.income = "10.705,87"
		wrappedBean.friend.friend.friend.gender = "MALE"
		assertEquals("John", p.friend.friend.friend.name)
		assertEquals(new GregorianCalendar(2000, 9, 8).time, p.friend.friend.friend.birthday)
		assertEquals(new BigDecimal("10705.87"), p.friend.friend.friend.income)
		assertEquals(Gender.MALE, p.friend.friend.friend.gender)
	}
	
	void testInvokeMethod() {
		Person p = new Person();
		wrappedBean.wrappedInstance = p
		assertEquals 2, wrappedBean.normalMethod(1)
	}
	
	void testWrapperProperties() {
		def p = new Person()
		wrappedBean.wrappedInstance = p
		assertSame p, wrappedBean.wrappedInstance
		def errors = new BindException(p, "person")
		wrappedBean.errors = errors
		assertSame errors, wrappedBean.errors
		assertSame wrappedBean.getBeanWrapper(), wrappedBean.beanWrapper
	}

	void setUp() {
		def locale = new Locale("pt", "BR")
	    def numberFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(locale))
	    def dateFormat = new SimpleDateFormat("dd/MM/yyyy")
		dateFormat.lenient = false
	    
		BeanWrapper bw = new BeanWrapperImpl()
	    bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true))
	    bw.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true))
	    this.wrappedBean = new WrappedBean(bw)
	}
}

static enum Gender {
    MALE, FEMALE;
}

class Person {
	String name
	Date birthday
	BigDecimal income
	Person friend
	Gender gender
	
	Errors errors
	
	Number normalMethod(Number i) {
		return i + 1
	}
}
